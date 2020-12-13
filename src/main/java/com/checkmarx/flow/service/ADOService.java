package com.checkmarx.flow.service;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.RepoIssue;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.dto.azure.Content;
import com.checkmarx.flow.dto.azure.CreateWorkItemAttr;
import com.checkmarx.flow.dto.azure.Value;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.flow.exception.ADOClientException;
import com.checkmarx.flow.utils.ADOUtils;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.CxPropertiesBase;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private final RestTemplate restTemplate;
    private final ADOProperties properties;
    private final FlowProperties flowProperties;
    private final CxPropertiesBase cxProperties;
    private final ScmConfigOverrider scmConfigOverrider;
    private final ThresholdValidator thresholdValidator;
    private String browseRepoEndpoint = "";

    public ADOService(@Qualifier("flowRestTemplate") RestTemplate restTemplate, ADOProperties properties,
                      FlowProperties flowProperties, CxScannerService cxScannerService,
                      ScmConfigOverrider scmConfigOverrider, ThresholdValidator thresholdValidator) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.cxProperties = cxScannerService.getProperties();
        this.scmConfigOverrider = scmConfigOverrider;
        this.thresholdValidator = thresholdValidator;
    }

    void processPull(ScanRequest request, ScanResults results) throws ADOClientException {
        try {
            String comment = HTMLHelper.getMergeCommentMD(request, results, properties);
            log.debug("comment: {}", comment);
            sendMergeComment(request, comment);
        } catch (HttpClientErrorException e){
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
        }
    }

    private String getFullAdoApiUrl(String url) {
        return url.concat(API_VERSION).concat(properties.getApiVersion());
    }

    private void updateComment(RepoComment repoComment, String newComment, ScanRequest scanRequest) {
        log.debug("Updating exisiting comment. url: {}", repoComment.getCommentUrl());
        log.debug("Updated comment: {}" , repoComment);
        HttpEntity<?> httpEntity = new HttpEntity<>(RepoIssue.getJSONComment(ADO_COMMENT_CONTENT_FIELD_NAME,newComment).toString(), ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance())));
        restTemplate.exchange(getFullAdoApiUrl(repoComment.getCommentUrl()), HttpMethod.PATCH, httpEntity, String.class);
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
            String url = request.getAdditionalMetadata("statuses_url");
            String statusId = request.getAdditionalMetadata("status_id");
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
            restTemplate.exchange(getFullAdoApiUrl(url).concat("-preview"),
                    HttpMethod.PATCH, httpEntity, Void.class);

            boolean isMergeAllowed = thresholdValidator.isMergeAllowed(results, properties, new PullRequestReport(scanDetails, request));

            if(!isMergeAllowed){
                log.debug("Creating status of failed to {}", url);
                createStatus("failed", "Checkmarx Scan Completed", url, results.getLink(), request);
            }
            else{
                log.debug("Creating status of succeeded to {}", url);
                createStatus("succeeded", "Checkmarx Scan Completed", url, results.getLink(), request);
            }
        }
    }

    int createStatus(String state, String description, String url, String sastUrl, ScanRequest scanRequest){
        HttpEntity<String> httpEntity = new HttpEntity<>(
                getJSONStatus(state, sastUrl, description).toString(),
                ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance())
        ));
        //TODO remove preview once applicable
        log.info("Adding pending status to pull {}", url);
        ResponseEntity<String> response = restTemplate.exchange(getFullAdoApiUrl(url).concat("-preview"),
                HttpMethod.POST, httpEntity, String.class);
        log.debug(String.valueOf(response.getStatusCode()));
        try{
            if(response.getBody() != null) {
                JSONObject json = new JSONObject(response.getBody());
                return json.getInt("id");
            }
        }catch (NullPointerException e){
            log.error("Error retrieving status id");
        }
        return -1;
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
        requestBody.put("status", 1);

        return requestBody;
    }

    private JSONObject getJSONComment(String description){
        JSONObject requestBody = new JSONObject();
        requestBody.put(ADO_COMMENT_CONTENT_FIELD_NAME, description);
        requestBody.put("parentCommentId", 1);
        requestBody.put("commentType", 1);

        return requestBody;
    }

    public List<RepoComment> getComments(String url, ScanRequest scanRequest) throws IOException {
        int maxNumberOfCommentThreads = 10000;
        HttpEntity<?> httpEntity = new HttpEntity<>(ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance())));
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity , String.class);
        List<RepoComment> result = new ArrayList<>();
        ObjectMapper objMapper = new ObjectMapper();
        JsonNode root = objMapper.readTree(response.getBody());
        JsonNode value = root.path("value");
        Iterator<JsonNode> threadsIter = value.getElements();
        int iteration = 0;
        while (threadsIter.hasNext() && iteration < maxNumberOfCommentThreads) {
            JsonNode thread = threadsIter.next();
            JsonNode comments = thread.get("comments");
            Iterator<JsonNode> commentsIter = comments.getElements();
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
        return result;

    }

    private boolean isCommentDeleted(JsonNode commentNode) {
        if (commentNode.has(IS_DELETED_FIELD_NAME) && commentNode.get(IS_DELETED_FIELD_NAME) != null) {
            return commentNode.get(IS_DELETED_FIELD_NAME).asBoolean();
        }
        return false;
    }

    private RepoComment createRepoComment(JsonNode commentNode)  {
        String commentBody = commentNode.path(ADO_COMMENT_CONTENT_FIELD_NAME).getTextValue();
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
        restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, String.class);
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
        CxConfig cxConfig;
        HttpHeaders headers = ADOUtils.createAuthHeaders(scmConfigOverrider.determineConfigToken(properties, request.getScmInstance()));
        String repoSelfUrl = request.getAdditionalMetadata(REPO_SELF_URL);
        String url = repoSelfUrl.concat(GET_FILE_CONTENT);

        log.info("Trying to load config-as-code from '{}' branch", branch);
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
        return cxConfig;
    }

    public Sources getRepoContent(ScanRequest request) {
        log.debug("Auto profiling is enabled");
        if(ScanUtils.anyEmpty(request.getNamespace(), request.getRepoName(), request.getBranch())){
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
        }catch (HttpClientErrorException e){
            log.error(ExceptionUtils.getRootCauseMessage(e));
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
        } catch (JsonProcessingException e) {
            log.error(String.format("Error in processing the JSON response from the repo. Error details : %s", ExceptionUtils.getRootCauseMessage(e)));
        }
        return contents;
    }
}