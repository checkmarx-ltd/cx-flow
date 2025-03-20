package com.checkmarx.flow.service;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.controller.ADOController;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.azure.*;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.flow.exception.ADOClientException;
import com.checkmarx.flow.utils.ADOUtils;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.CommonUtils;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxPropertiesBase;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.dto.sast.CxConfig;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.service.CxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ADOService {
    private static final String API_VERSION = "?api-version=";
    public static final String REPO_SELF_URL = "repo-self-url";
    public static final String REPO_ID = "repo-id";
    public static final String PROJECT_SELF_URL = "project-self-url";
    private static final String GET_FILE_CONTENT = "/items?path={filePath}&version={branch}&$format=text&api-version={apiVersion}";
    private static final String GET_DIRECTORY_CONTENT = "/items?scopePath={filePath}&version={branch}&api-version={apiVersion}&recursionLevel={recursionLevel}";
    private static final String LANGUAGE_METRICS = "/_apis/projectanalysis/languagemetrics";
    private static final String ADO_COMMENT_CONTENT_FIELD_NAME = "content";
    private static final String IS_DELETED_FIELD_NAME = "isDeleted";
    private static final String NO_CONTENT_FOUND_IN_RESPONSE = "No content found in JSON response.";
    private static final String HTTP_RESPONSE_BODY_IS_NULL = "Response body is empty.";
    private static final String BRANCH_DELETED_REF = StringUtils.repeat('0', 40);
    private static final int NAMESPACE_INDEX = 3;

    private static final Integer RESOLVED = 2;
    private static final Integer CLOSED = 4;
    private static final String PREVIEW = "-preview";
    private final RestTemplate restTemplate;
    private final ADOProperties properties;
    private final FlowProperties flowProperties;
    private final CxPropertiesBase cxProperties;
    private final ScmConfigOverrider scmConfigOverrider;
    private final ThresholdValidator thresholdValidator;
    private final ConfigurationOverrider configOverrider;
    private final FlowService flowService;

    @Autowired
    HelperService helperService;

    @Autowired
    FilterFactory filterFactory;

    private String browseRepoEndpoint = "";

    @Autowired
    GitAuthUrlGenerator gitAuthUrlGenerator;

    @Autowired
    JiraProperties jiraProperties;

    @Autowired
    @Qualifier("cxService")
    private CxService cxService;

    public ADOService(@Qualifier("flowRestTemplate") RestTemplate restTemplate, ADOProperties properties, FlowProperties flowProperties, CxScannerService cxScannerService, ScmConfigOverrider scmConfigOverrider, ThresholdValidator thresholdValidator, @Lazy ConfigurationOverrider configOverrider, @Lazy FlowService flowService) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.cxProperties = cxScannerService.getProperties();
        this.scmConfigOverrider = scmConfigOverrider;
        this.thresholdValidator = thresholdValidator;
        this.configOverrider = configOverrider;
        this.flowService = flowService;

    }

    void processPull(ScanRequest request, ScanResults results) throws ADOClientException {
        try {
            String comment = HTMLHelper.getMergeCommentMD(request, results, properties);
            log.debug("comment: {}", comment);
            sendMergeComment(request, comment);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating Merge Request comment", e);
            throw new ADOClientException();
        }
    }

    public void sendMergeComment(ScanRequest request, String comment){
        String mergeUrl = request.getMergeNoteUri();
        if(ScanUtils.empty(mergeUrl)){
            log.error("mergeUrl was not provided within the request object, which is required for commenting on pull request");
            return;
        }
        log.debug(mergeUrl);
        try {
            RepoComment commentToUpdate = PullRequestCommentsHelper.getCommentToUpdate(getComments(mergeUrl, request), comment);
            if (commentToUpdate != null && PullRequestCommentsHelper.shouldUpdateComment(comment, commentToUpdate.getComment())) {
                updateComment(commentToUpdate, comment, request);
            } else if (commentToUpdate == null){
                String threadId = request.getAdditionalMetadata("ado_thread_id");
                if (ScanUtils.empty(threadId)) {
                    HttpEntity<String> httpEntity = new HttpEntity<>(getJSONThread(comment).toString(), ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, request.getScmInstance())));
                    log.debug("Creating new thread for comments");
                    ResponseEntity<String> response = restTemplate.exchange(getFullAdoApiUrl(mergeUrl),
                            HttpMethod.POST, httpEntity, String.class);
                    if (response.getBody() != null) {
                        JSONObject json = new JSONObject(response.getBody());
                        int id = json.getInt("id");
                        request.putAdditionalMetadata("ado_thread_id", Integer.toString(id));
                        log.debug("Created new thread with Id {}", id);
                    }
                } else {
                    HttpEntity<String> httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, request.getScmInstance())));
                    mergeUrl = mergeUrl.concat("/").concat(threadId).concat("/comments");
                    log.debug("Adding comment to thread Id {}", threadId);
                    restTemplate.exchange(getFullAdoApiUrl(mergeUrl),
                            HttpMethod.POST, httpEntity, String.class);
                }
            }
        }
        catch (Exception e) {
            // We "swallow" the exception so that the flow will not be terminated because of errors in GIT comments
            log.error("Error while adding or updating repo pull request comment", e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    private String getFullAdoApiUrl(String url) {
        return url.concat(API_VERSION).concat(properties.getApiVersion());
    }

    private void updateComment(RepoComment repoComment, String newComment, ScanRequest scanRequest) {
        log.debug("Updating exisiting comment. url: {}", repoComment.getCommentUrl());
        log.debug("Updated comment: {}", repoComment);
        HttpEntity<?> httpEntity = new HttpEntity<>(RepoIssue.getJSONComment(ADO_COMMENT_CONTENT_FIELD_NAME, newComment).toString(), ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance())));
        try {
            restTemplate.exchange(getFullAdoApiUrl(repoComment.getCommentUrl()), HttpMethod.PATCH, httpEntity, String.class);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while updating comment. http error {} ", e.getStatusCode());
            log.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    public void startBlockMerge(ScanRequest request){
        if(properties.isBlockMerge()) {
            String url = request.getAdditionalMetadata("statuses_url");
            if(ScanUtils.empty(url)){
                log.warn("No status url found, skipping status update");
                return;
            }
            int statusId = createStatus("pending","Checkmarx Scan Initiated", url,
                    cxProperties.getBaseUrl().concat("/CxWebClient/UserQueue.aspx"), request);
            if(statusId != -1) {
                request.getAdditionalMetadata().put("status_id", Integer.toString(statusId));
            }
        }
    }


    void endBlockMerge(ScanRequest request, ScanResults results, ScanDetails scanDetails){
        if(properties.isBlockMerge()) {
            Integer projectId = Integer.parseInt(results.getProjectId());
            String url = request.getAdditionalMetadata("statuses_url");
            String statusId = request.getAdditionalMetadata("status_id");
            String threadUrl = null;
            if(request.getAdditionalMetadata("ado_thread_id") != null){
                threadUrl = request.getMergeNoteUri().concat("/").concat(request.getAdditionalMetadata("ado_thread_id"));
            }
            if(statusId == null){
                log.warn("No status Id found, skipping status update");
                return;
            }
            CreateWorkItemAttr item = new CreateWorkItemAttr();
            item.setOp("remove");
            item.setPath("/".concat(statusId));
            List<CreateWorkItemAttr> list = new ArrayList<>();
            list.add(item);

            HttpEntity<List<CreateWorkItemAttr>> httpEntity = new HttpEntity<>(
                    list,
                    ADOUtils.createPatchAuthHeaders(scmConfigOverrider.determineConfigToken(properties, request.getScmInstance())
                    ));
            if(ScanUtils.empty(url)){
                log.error("statuses_url was not provided within the request object, which is required for blocking / unblocking pull requests");
                return;
            }
            //TODO remove preview once applicable
            log.info("Removing pending status from pull {}", url);
            try {
                restTemplate.exchange(getFullAdoApiUrl(url).concat("-preview"),
                        HttpMethod.PATCH, httpEntity, Void.class);
            } catch (HttpClientErrorException e) {
                log.error("Error occurred in endBlockMerge. http error {} ", e.getStatusCode());
                log.debug(ExceptionUtils.getStackTrace(e));
            }
            /*
                if the SAST server fails to scan a project it generates a result with ProjectId = -1
                This if statement adds a status of failed to the ADO PR, and sets the status of thread to
                CLOSED.
             */
            if(projectId == -1){
                log.debug("SAST scan could not be processed due to some error. Creating status of failed to {}", url);
                createStatus("failed", "Checkmarx Scan could not be processed.", url, results.getLink(), request);
                if(threadUrl != null) {
                    createThreadStatus(CLOSED, threadUrl, request);
                }
                return;
            }

            boolean isMergeAllowed = thresholdValidator.isMergeAllowed(results, properties, new PullRequestReport(scanDetails, request));

            if(!isMergeAllowed){
                log.debug("Creating status of failed to {}", url);
                createStatus("failed", "Checkmarx Scan Completed", url, results.getLink(), request);
                if(threadUrl != null) {
                    createThreadStatus(CLOSED, threadUrl, request);
                }
            }
            else{
                log.debug("Creating status of succeeded to {}", url);
                createStatus("succeeded", "Checkmarx Scan Completed", url, results.getLink(), request);
                if(threadUrl != null) {
                    createThreadStatus(RESOLVED, threadUrl, request);
                }
            }
        }
    }


    void endBlockMergeFailed(ScanRequest request){
        if(properties.isBlockMerge()) {
            String url = request.getAdditionalMetadata("statuses_url");
            String statusId = request.getAdditionalMetadata("status_id");
            String threadUrl = null;
            if(request.getAdditionalMetadata("ado_thread_id") != null){
                threadUrl = request.getMergeNoteUri().concat("/").concat(request.getAdditionalMetadata("ado_thread_id"));
            }
            if(statusId == null){
                log.warn("No status Id found, skipping status update");
                return;
            }
            CreateWorkItemAttr item = new CreateWorkItemAttr();
            item.setOp("remove");
            item.setPath("/".concat(statusId));
            List<CreateWorkItemAttr> list = new ArrayList<>();
            list.add(item);

            HttpEntity<List<CreateWorkItemAttr>> httpEntity = new HttpEntity<>(
                    list,
                    ADOUtils.createPatchAuthHeaders(scmConfigOverrider.determineConfigToken(properties, request.getScmInstance())
                    ));
            if(ScanUtils.empty(url)){
                log.error("statuses_url was not provided within the request object, which is required for blocking / unblocking pull requests");
                return;
            }
            //TODO remove preview once applicable
            log.info("Removing pending status from pull {}", url);
            try {
                restTemplate.exchange(getFullAdoApiUrl(url).concat("-preview"),
                        HttpMethod.PATCH, httpEntity, Void.class);
            } catch (HttpClientErrorException e) {
                log.error("Error occurred in endBlockMerge. http error {} ", e.getStatusCode());
                log.debug(ExceptionUtils.getStackTrace(e));
            }
            /*
                if the SAST server fails to scan a project it generates a result with ProjectId = -1
                This if statement adds a status of failed to the ADO PR, and sets the status of thread to
                CLOSED.
             */




            log.debug("Creating status of failed to {}", url);
            createStatus("failed", "Scan failed due to some error", url, "NA", request);
            if(threadUrl != null) {
                createThreadStatus(CLOSED, threadUrl, request);
            }

        }
    }

    int createStatus(String state, String description, String url, String sastUrl, ScanRequest scanRequest){
        HttpEntity<String> httpEntity = new HttpEntity<>(
                getJSONStatus(state, sastUrl, description).toString(),
                ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance())
                ));
        //TODO remove preview once applicable
        log.info("Adding {} status to pull {}",state, url);
        try{
            ResponseEntity<String> response = restTemplate.exchange(getFullAdoApiUrl(url).concat("-preview"),
                    HttpMethod.POST, httpEntity, String.class);
            log.debug(String.valueOf(response.getStatusCode()));
            if(response.getBody() != null) {
                JSONObject json = new JSONObject(response.getBody());
                return json.getInt("id");
            }
        }catch (NullPointerException e){
            log.error("Error retrieving status id", e);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating status. http error {} ", e.getStatusCode());
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        return -1;
    }

    /*
        This function is used to update the status of the Thread
        of the PR on Azure from Active to either a RESOLVED or CLOSED based on the status
        of the scan is Succeeded or Failed respectively.
        The status is sent as an Integer value
        status of 2 = RESOLVED
        status of 4 = CLOSED
     */
    void createThreadStatus(Integer status, String url, ScanRequest scanRequest){
        HttpEntity<String> httpEntity = new HttpEntity<>(
                getJSONThreadUpdate(status).toString(),
                ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance())
                ));
        try {
            ResponseEntity<String> response = restTemplate.exchange(getFullAdoApiUrl(url).concat(PREVIEW),
                    HttpMethod.PATCH, httpEntity, String.class);
            if(response.getBody() != null) {
                log.info("Successfully Updated thread status to {}",status);
            }
        } catch (NullPointerException e) {
            log.error("Error updating the thread status", e);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while Creating Thread Status. http error {} ", e.getStatusCode());
            log.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    private JSONObject getJSONStatus(String state, String url, String description){
        JSONObject requestBody = new JSONObject();
        JSONObject context = new JSONObject();
        requestBody.put("state", state);
        requestBody.put("description", description);
        context.put("name", "checkmarx");
        context.put("genre", "sast");
        requestBody.put("context", context);
        requestBody.put("target_url", url);
        return requestBody;
    }

    private JSONObject getJSONThread(String description){
        JSONObject requestBody = new JSONObject();
        JSONArray comments = new JSONArray();
        JSONObject comment = new JSONObject();
        comment.put("parentCommentId", 0);
        comment.put(ADO_COMMENT_CONTENT_FIELD_NAME, description);
        comment.put("commentType", 1);
        comments.put(comment);
        requestBody.put("comments", comments);
        if(properties.getCommentStatusWhook()!= -1){
            requestBody.put("status", properties.getCommentStatusWhook());
        }else{
            requestBody.put("status", properties.getCommentStatus());
        }

        return requestBody;
    }

    private JSONObject getJSONComment(String description){
        JSONObject requestBody = new JSONObject();
        requestBody.put(ADO_COMMENT_CONTENT_FIELD_NAME, description);
        requestBody.put("parentCommentId", 1);
        requestBody.put("commentType", 1);

        return requestBody;
    }

    /*
        getJSONThreadUpdate is used to create a JSON Payload with the thread status
        value to update it in the PR
     */
    private JSONObject getJSONThreadUpdate(Integer status)
    {
        JSONObject requestBody = new JSONObject();
        requestBody.put("status",status);
        return requestBody;
    }

    public List<RepoComment> getComments(String url, ScanRequest scanRequest) throws IOException {
        int maxNumberOfCommentThreads = 10000;
        HttpEntity<?> httpEntity = new HttpEntity<>(ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance())));
        List<RepoComment> result = new ArrayList<>();
        try {
            ResponseEntity<String>  response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            ObjectMapper objMapper = new ObjectMapper();
            JsonNode root = objMapper.readTree(response.getBody());
            JsonNode value = root.path("value");
            Iterator<JsonNode> threadsIter = value.elements();
            int iteration = 0;
            while (threadsIter.hasNext() && iteration < maxNumberOfCommentThreads) {
                JsonNode thread = threadsIter.next();
                JsonNode comments = thread.get("comments");
                Iterator<JsonNode> commentsIter = comments.elements();
                int commentsCount = 0;
                while (commentsIter.hasNext() && commentsCount < maxNumberOfCommentThreads) {
                    JsonNode commentNode = commentsIter.next();
                    // Remove empty or deleted comments
                    if (commentNode.has(ADO_COMMENT_CONTENT_FIELD_NAME) && !isCommentDeleted(commentNode)) {
                        RepoComment rc = createRepoComment(commentNode);
                        if (PullRequestCommentsHelper.isCheckMarxComment(rc)) {
                            result.add(rc);
                        }
                    }
                    commentsCount++;
                }
                iteration++;
            }
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while getting Comments. http error {} ", e.getStatusCode());
            log.debug(ExceptionUtils.getStackTrace(e));
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON response");
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        return result;

    }

    private boolean isCommentDeleted(JsonNode commentNode) {
        if (commentNode.has(IS_DELETED_FIELD_NAME) && commentNode.get(IS_DELETED_FIELD_NAME) != null) {
            return commentNode.get(IS_DELETED_FIELD_NAME).asBoolean();
        }
        return false;
    }

    private RepoComment createRepoComment(JsonNode commentNode)  {
        String commentBody = commentNode.path(ADO_COMMENT_CONTENT_FIELD_NAME).textValue();
        long id = commentNode.path("id").asLong();
        String commentUrl = commentNode.path(("_links")).path("self").path("href").asText();
        String updatedStr = commentNode.path("lastContentUpdatedDate").asText();
        String createdStr = commentNode.path("publishedDate").asText();
        return new RepoComment(id, commentBody, commentUrl, parseDate(createdStr), parseDate(updatedStr));
    }

    private Date parseDate(String dateStr) {
        LocalDateTime date = ZonedDateTime.parse(dateStr).toLocalDateTime();
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    public void deleteComment(String url, ScanRequest scanRequest) {
        url = getFullAdoApiUrl(url);
        HttpEntity<?> httpEntity = new HttpEntity<>(ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance())));
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, String.class);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while deleting comment. http error {} ", e.getStatusCode());
            log.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    public CxConfig getCxConfigOverride(ScanRequest request, String branch) {
        CxConfig result = null;
        if (StringUtils.isNotBlank(properties.getConfigAsCode())) {
            try {
                result = loadCxConfigFromADO(request, branch);
            } catch (NullPointerException e) {
                log.warn(NO_CONTENT_FOUND_IN_RESPONSE);
            } catch (HttpClientErrorException.NotFound e) {
                log.info("No Config as code was found with the name: {}, in branch {}", properties.getConfigAsCode(), branch);
            } catch (Exception e) {
                log.error("Error in getting config as code from the repo. Error details : {}", ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return result;
    }

    private CxConfig loadCxConfigFromADO(ScanRequest request, String branch) {
        CxConfig cxConfig = null;
        HttpHeaders headers = ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, request.getScmInstance()));
        String repoSelfUrl = request.getAdditionalMetadata(REPO_SELF_URL);
        String url = repoSelfUrl.concat(GET_FILE_CONTENT);

        log.info("Trying to load config-as-code from '{}' branch", branch);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class,
                    properties.getConfigAsCode(),
                    branch,
                    properties.getApiVersion()
            );
            if (response.getBody() == null) {
                log.warn(HTTP_RESPONSE_BODY_IS_NULL);
                cxConfig = null;
            } else {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                if (ScanUtils.empty(jsonResponse.toString())) {
                    log.warn(NO_CONTENT_FOUND_IN_RESPONSE);
                    cxConfig = null;
                } else {
                    cxConfig = com.checkmarx.sdk.utils.ScanUtils.getConfigAsCode(jsonResponse.toString());
                }
            }
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while loading cxconfig from ADO. Http Error {} ", e.getStatusCode());
            log.debug(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e) {
            log.error("Error processing JSON response, Please check cx.config file format");
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        return cxConfig;
    }

    public Sources getRepoContent(ScanRequest request) {
        log.debug("Auto profiling is enabled");
        if (ScanUtils.anyEmpty(request.getNamespace(), request.getRepoName(), request.getBranch())) {
            return null;
        }
        Sources sources = getRepoLanguagePercentages(request);
        browseRepoEndpoint = getADOEndPoint(request);
        scanGitContent(0, browseRepoEndpoint, sources, request);
        return sources;
    }

    private String getADOEndPoint(ScanRequest request) {

        String projectUrl = request.getAdditionalMetadata(REPO_SELF_URL);
        String endpoint = projectUrl.concat(GET_DIRECTORY_CONTENT);

        endpoint = endpoint.replace("{apiVersion}", properties.getApiVersion());
        endpoint = endpoint.replace("{recursionLevel}", "OneLevel");
        endpoint = endpoint.replace("{branch}", request.getBranch());
        return endpoint;
    }

    private Sources getRepoLanguagePercentages(ScanRequest request) {
        Sources sources = new Sources();
        Map<String, Integer> languagePercent = new HashMap<>();
        HttpHeaders headers = ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, request.getScmInstance()));

        String projectUrl = request.getAdditionalMetadata(PROJECT_SELF_URL);
        String urlTemplate = projectUrl.concat(LANGUAGE_METRICS);

        log.info("Getting repo languages from {}", urlTemplate);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    urlTemplate, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if(response.getBody() == null){
                log.warn(HTTP_RESPONSE_BODY_IS_NULL);
            }
            else {
                JSONObject jsonBody = new JSONObject(response.getBody());
                JSONArray repoLanguageStats = jsonBody.getJSONArray("repositoryLanguageAnalytics");

                for(Object repo : repoLanguageStats){
                    String repoId = ((JSONObject)repo).getString("id");
                    if(repoId.equals(request.getAdditionalMetadata(REPO_ID)))
                    {
                        JSONArray languageBreakdown = ((JSONObject)repo).getJSONArray("languageBreakdown");
                        for(Object language : languageBreakdown){
                            String key = ((JSONObject)language).getString("name");
                            if(((JSONObject)language).has("languagePercentage")) {
                                Integer percentage = ((JSONObject) language).getInt("languagePercentage");
                                languagePercent.put(key, percentage);
                            }
                        }
                    }
                }
                sources.setLanguageStats(languagePercent);
            }
        } catch (NullPointerException e) {
            log.warn(NO_CONTENT_FOUND_IN_RESPONSE);
        }catch (HttpClientErrorException.NotFound e){
            String error = "Got 404 'Not Found' error. Azure endpoint: " + urlTemplate + " is invalid.";
            log.warn(error);
            log.debug(ExceptionUtils.getStackTrace(e));
        }catch (HttpClientErrorException e){
            log.error("Error occurred in getRepoLanguagePercentages method", ExceptionUtils.getRootCauseMessage(e));
            log.debug(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e) {
            log.error("Error processing JSON response", e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        return sources;
    }

    private void scanGitContent(int depth, String endpoint, Sources sources, ScanRequest scanRequest){
        if(depth >= flowProperties.getProfilingDepth()){
            return;
        }

        if(depth == 0) {
            endpoint = endpoint.replace("{filePath}", Strings.EMPTY);
        }

        Content contents = getRepoContent(endpoint, scanRequest);
        List<Value> values = contents.getValue();

        for(int i=1; i< values.size(); i++){
            Value value =  values.get(i);
            if(value.getIsFolder()){
                String fullDirectoryUrl = browseRepoEndpoint.replace("{filePath}", value.getPath());
                scanGitContent(depth + 1, fullDirectoryUrl, sources, scanRequest);
            }
            else {
                sources.addSource(value.getPath(), value.getPath());
            }
        }
    }

    private Content getRepoContent(String endpoint, ScanRequest scanRequest) {
        log.info("Getting repo content from {}", endpoint);
        Content contents = new Content();
        HttpHeaders headers = ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance()));
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if(response.getBody() == null){
                log.warn(HTTP_RESPONSE_BODY_IS_NULL);
            }
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            contents = objectMapper.readValue(response.getBody(), Content.class);
            return contents;
        } catch (NullPointerException e) {
            log.warn(NO_CONTENT_FOUND_IN_RESPONSE);
        } catch (HttpClientErrorException e) {
            log.warn("Repo content is unavailable. The reason can be that branch has been deleted.");
            log.debug(ExceptionUtils.getStackTrace(e));
        } catch (JsonProcessingException e) {
            log.error(String.format("Error in processing the JSON response from the repo. Error details : %s", ExceptionUtils.getRootCauseMessage(e)));
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        return contents;
    }

    public boolean isScanSubmittedComment() {
        return this.properties.isScanSubmittedComment();
    }

    private boolean isScanCommand(String command) {
        return command.equalsIgnoreCase("cancel") || command.equalsIgnoreCase("status");
    }

    private Optional<Integer> extractScanId(String command, String comment) {
        log.info("Extracting ScanId from comment: {}", command);
        Pattern pattern;
        pattern = command.equalsIgnoreCase("cancel") ? Pattern.compile("@cxflow cancel (\\d+)") : Pattern.compile("@cxflow status (\\d+)");
        Matcher matcher = pattern.matcher(comment);
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    private URI buildPullrequestCommentsURL(String baseUrl, String projectName, String repositoryId, Integer pullRequestId, Integer threadId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).pathSegment(projectName, "_apis", "git", "repositories", repositoryId, "pullRequests", String.valueOf(pullRequestId), "threads", String.valueOf(threadId), "comments");
        return builder.queryParam("api-version", "7.1").build().toUri();
    }


    private void processPRCommentCommand(PRCommentEvent event, ADOProperties properties, String command, String baseUrl,
                                         String projectName, String repositoryId, Integer pullRequestId, Integer threadId, Optional<Integer> scanID, Map<FindingSeverity, Integer> thresholdMap, List<String> branches, ControllerRequest controllerRequest, String product,
                                         ResourceContainers resourceContainers, String body, ADOController.Action action, String uid, AdoDetailsRequest adoDetailsRequest,String userName) {
        switch (command) {
            case "hi":
                postComment(properties, " Hi " + userName + "," + "\n How can CX-Flow help you? \n" + "- Get the status of the current scan by posting the command: <b>@CXFlow</b> status scanID\n" + "- Perform a new scan by posting the command: <b>@CXFlow</b> rescan\n" + "- Cancel a running scan by posting the command: <b>@CXFlow</b> cancel scanID", baseUrl, projectName, repositoryId, pullRequestId, threadId);
                log.info("Finished processing for PR comment :@Cxflow hi");
                break;

            case "status":
                if (scanID.isPresent()) {
                    postComment(properties, "- Scan with scanID " + scanID.get() + " is in: " + "<b>" + cxService.getScanStatusName(scanID.get()) + "</b>" + " state", baseUrl, projectName, repositoryId, pullRequestId, threadId);
                    log.info("Finished processing for PR comment :@Cxflow Scan status");
                }
                break;

            case "cancel":
                if (scanID.isPresent()) {
                    if (!cxService.getScanStatusName(scanID.get()).equalsIgnoreCase("Finished")) {
                        cxService.cancelScan(scanID.get());
                        postComment(properties, "- Scan cancelled with ScanID:" + scanID.get(), baseUrl, projectName, repositoryId, pullRequestId, threadId);
                    } else {
                        postComment(properties, "- Cannot cancel already finished Scan with ScanID: " + scanID.get(), baseUrl, projectName, repositoryId, pullRequestId, threadId);
                    }

                    log.info("Finished processing for PR comment :@CxFlow cancel");
                }
                break;

            case "rescan":
                postComment(properties, "- Rescan initiated.", baseUrl, projectName, repositoryId, pullRequestId, threadId);
                String rescanStatus=triggerRescan(event, controllerRequest, adoDetailsRequest, product, resourceContainers, body, action, uid, thresholdMap, branches,threadId);
                log.info("Finished processing for PR comment :@CxFlow {}",command);
                log.info("Status for rescan: {} ", rescanStatus);
                break;

             default:
                unsupportedCommand(properties,  baseUrl, projectName, repositoryId, pullRequestId, threadId,userName);
                 log.info("Received Unsupported command for CxFlow");
        }
    }

    private void unsupportedCommand(ADOProperties properties, String baseUrl, String projectName, String repositoryId, Integer pullRequestId, Integer threadId,String username){
        postComment(properties, "I'm afraid I can't do that " + username, baseUrl, projectName, repositoryId, pullRequestId, threadId);
    }

    private void postComment(ADOProperties properties, String content, String baseUrl, String projectName, String repositoryId, Integer pullRequestId, Integer threadId) {
        try {
            log.info("Posting the Comment");
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.valueOf(javax.ws.rs.core.MediaType.APPLICATION_JSON));
            headers.setBearerAuth(properties.getToken());

            Map<String, String> body = new HashMap<>();
            body.put("content", content);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            URI uriToPost = buildPullrequestCommentsURL(baseUrl, projectName, repositoryId, pullRequestId, threadId);
            restTemplate.postForEntity(uriToPost, request, String.class);

        } catch (HttpClientErrorException e) {
            log.error("Error occurred while posting comment on PR {}", e.getStatusCode());
            log.debug(ExceptionUtils.getStackTrace(e));
        }

    }

    private void processNoScanIdCommand(ADOProperties properties, String command, String baseUrl, String projectName, String repositoryId, Integer pullRequestId, Integer threadId) {
        log.info("Processing No ScanId provided Command");
        postComment(properties, "Please provide Scan ID", baseUrl, projectName, repositoryId, pullRequestId, threadId);
    }

    public void adoPRCommentHandler(PRCommentEvent event, ADOProperties properties, String comment, String baseUrl, String projectName, String repositoryId, Integer pullRequestId, Integer threadId, Map<FindingSeverity, Integer> thresholdMap, List<String> branches, ControllerRequest controllerRequest, String product, ResourceContainers resourceContainers, String body, ADOController.Action action, String uid, AdoDetailsRequest adoDetailsRequest) {
        log.info("Parsing the PR comment");
        String userName=event.getResource().getComment().getAuthor().getDisplayName();
        String command = CommonUtils.parseCommand(comment);
        Optional<Integer> scanID = isScanCommand(command) ? extractScanId(command, comment) : Optional.empty();

        if (scanID.isEmpty() && isScanCommand(command)) {
            processNoScanIdCommand(properties, command, baseUrl, projectName, repositoryId, pullRequestId, threadId);
        } else {
            processPRCommentCommand(event, properties, command, baseUrl, projectName, repositoryId, pullRequestId, threadId, scanID, thresholdMap, branches, controllerRequest, product,
                    resourceContainers, body, action, uid, adoDetailsRequest,userName);
        }
    }

    private String triggerRescan(PRCommentEvent event, ControllerRequest controllerRequest, AdoDetailsRequest adoDetailsRequest, String product,
                                 ResourceContainers resourceContainers, String body, ADOController.Action action,
                                 String uid, Map<FindingSeverity, Integer> thresholdMap, List<String> branches,Integer threadId) {
        try {
            ResourceComment resource = event.getResource();
            Repository repository = resource.getPullRequest().getRepository();
            String pullUrl = resource.getPullRequest().getUrl();
            String app = repository.getName();

            if (repository.getName().startsWith(properties.getTestRepository())) {
                log.info("Handling ADO Test Event");
                return "Test Event";
            }

            if (StringUtils.isNotEmpty(controllerRequest.getApplication())) {
                app = controllerRequest.getApplication();
            }

            BugTracker.Type bugType = BugTracker.Type.ADOPULL;
            if (StringUtils.isNotEmpty(controllerRequest.getBug())) {
                bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());
            }

            if (controllerRequest.getAppOnly() != null) {
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }

            if (controllerRequest.getCommentmsgid() != null) {
                properties.setCommentStatusWhook(controllerRequest.getCommentmsgid());
            } else {
                properties.setCommentStatusWhook(-1);
            }

            initAdoSpecificParams(adoDetailsRequest);

            if (StringUtils.isEmpty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

            String ref = resource.getPullRequest().getSourceRefName();
            String currentBranch = ScanUtils.getBranchFromRef(ref);
            String targetBranch = ScanUtils.getBranchFromRef(resource.getPullRequest().getTargetRefName());


            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);


            //build request object
            String gitUrl = repository.getWebUrl();
            String token = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.ADO, gitUrl, token);

            ScanRequest request = ScanRequest.builder().application(app).product(p).project(controllerRequest.getProject()).team(controllerRequest.getTeam()).namespace(determineNamespace(resourceContainers)).repoName(repository.getName()).repoUrl(gitUrl).repoUrlWithAuth(gitAuthUrl).repoType(ScanRequest.Repository.ADO).branch(currentBranch).refs(ref).mergeNoteUri(pullUrl.concat("/threads")).mergeTargetBranch(targetBranch).email(null).scanPreset(controllerRequest.getPreset()).incremental(controllerRequest.getIncremental()).excludeFolders(controllerRequest.getExcludeFolders()).excludeFiles(controllerRequest.getExcludeFiles()).bugTracker(bt).filter(filter).thresholds(thresholdMap).organizationId(determineNamespace(resourceContainers)).gitUrl(gitUrl).build();

            //setScmInstance
            Optional.ofNullable(controllerRequest.getScmInstance()).ifPresent(request::setScmInstance);

            request.putAdditionalMetadata(ADOService.PROJECT_SELF_URL, getProjectURL(event.getResourceContainers()));
            fillRequestWithAdditionalData(request, repository, body.toString());
            checkForConfigAsCode(request, getConfigBranch(request, resource, action));
            request.putAdditionalMetadata("statuses_url", pullUrl.concat("/statuses"));
            request.putAdditionalMetadata(Constants.ADO_ISSUE_KEY, adoDetailsRequest.getAdoIssue());
            request.putAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY, adoDetailsRequest.getAdoBody());
            request.putAdditionalMetadata(Constants.ADO_OPENED_STATE_KEY, adoDetailsRequest.getAdoOpened());
            request.putAdditionalMetadata(Constants.ADO_CLOSED_STATE_KEY, adoDetailsRequest.getAdoClosed());
            request.setId(uid);
            //only initiate scan/automation if target branch is applicable
            if (helperService.isBranch2Scan(request, branches)) {
                log.debug(request.getProject() + " :: Calling  isBranch2Scan function End : " + System.currentTimeMillis());
                log.debug(request.getProject() + " :: Free Memory : " + Runtime.getRuntime().freeMemory());
                log.debug(request.getProject() + " :: Total Numbers of processors : " + Runtime.getRuntime().availableProcessors());
                long startTime = System.currentTimeMillis();
                log.debug(request.getProject() + " :: Start Time : " + startTime);
                flowService.initiateAutomation(request);
                long endTime = System.currentTimeMillis();
                log.debug(request.getProject() + " :: End Time  : " + endTime);
                log.debug(request.getProject() + " :: Total Time Taken  : " + (endTime - startTime));
            }
            else{
                return "FAILED";
            }

        } catch (IllegalArgumentException e) {
            log.error("\"Error submitting Scan Request. Product: " + product + "or Bugtracker option incorrect:" + controllerRequest.getBug(), e);
            return "FAILED";
        }

        return "SUCCESS";
    }

    public void initAdoSpecificParams(AdoDetailsRequest request) {
        if (StringUtils.isEmpty(request.getAdoIssue())) {
            request.setAdoIssue(properties.getIssueType());
        }
        if (StringUtils.isEmpty(request.getAdoBody())) {
            request.setAdoBody(properties.getIssueBody());
        }
        if (StringUtils.isEmpty(request.getAdoOpened())) {
            request.setAdoOpened(properties.getOpenStatus());
        }
        if (StringUtils.isEmpty(request.getAdoClosed())) {
            request.setAdoClosed(properties.getClosedStatus());
        }
    }


    public void checkForConfigAsCode(ScanRequest request, String branch) {
        CxConfig cxConfig = getCxConfigOverride(request, branch);
        configOverrider.overrideScanRequestProperties(cxConfig, request);
    }

    public void fillRequestWithAdditionalData(ScanRequest request, Repository repository, String hookPayload) {
        request.putAdditionalMetadata(ADOService.REPO_ID, repository.getId());
        request.putAdditionalMetadata(ADOService.REPO_SELF_URL, repository.getUrl());
        request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, hookPayload);
    }

    public <T> String getConfigBranch(ScanRequest request, T resource, ADOController.Action action) {
        String branch = request.getBranch();
        log.info("the branch requested is " + branch);
        try {

            if (isDeleteBranchEvent(resource) && action.equals(ADOController.Action.PUSH)) {
                branch = request.getDefaultBranch();
                log.debug("branch to read config-as-code: {}", branch);
            }
        } catch (Exception ex) {
            log.info("failed to get branch for config as code. using default");
        }
        return branch;
    }


    public <T> boolean isDeleteBranchEvent(T resource) {
        if (resource instanceof Resource) {
            return checkdeleteBranchResource((Resource) resource);
        }
        return false;
    }

    private boolean checkdeleteBranchResource(Resource resource) {
        if (resource.getRefUpdates().size() == 1) {
            String newBranchRef = resource.getRefUpdates().get(0).getNewObjectId();

            if (newBranchRef.equals(BRANCH_DELETED_REF)) {
                log.info("new-branch ref is empty - detect ADO DELETE event");
                return true;
            }
            return false;
        }
        int refCount = resource.getRefUpdates().size();
        log.warn("unexpected number of refUpdates in push event: {}", refCount);
        return false;
    }


    public String determineNamespace(ResourceContainers resourceContainers) {
        return getNameSpace(resourceContainers);
    }

    private String getNameSpace(ResourceContainers resourceContainers) {
        String namespace = "";
        try {
            log.debug("Trying to extract namespace from request body");
            String projectUrl = resourceContainers.getProject().getBaseUrl();
            namespace = projectUrl.split("/")[NAMESPACE_INDEX];
        } catch (Exception e) {
            log.warn("Can't find namespace in body resource containers: {}", e.getMessage());
        }
        log.info("using namespace: {}", namespace);
        return namespace;
    }

    private String getProjectURL(ResourceContainers resourceContainers) {
        String projectId = resourceContainers.getProject().getId();
        String baseUrl = resourceContainers.getProject().getBaseUrl();
        return baseUrl.concat(projectId);
    }


}