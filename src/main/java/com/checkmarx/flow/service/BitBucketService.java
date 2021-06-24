package com.checkmarx.flow.service;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.dto.bitbucketserver.Content;
import com.checkmarx.flow.dto.bitbucketserver.Value;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.flow.exception.BitBucketClientException;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sast.CxConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class BitBucketService extends RepoService {

    public static final String REPO_SELF_URL = "repo-self-url";
    public static final String CX_USER_SCAN_QUEUE = "/CxWebClient/UserQueue.aspx";
    public static final String PATH_SEPARATOR = "/";
    private static final String LOG_COMMENT = "comment: {}";
    private static final String BUILD_IN_PROGRESS = "INPROGRESS";
    private static final String BUILD_SUCCESSFUL = "SUCCESSFUL";
    private static final String BUILD_FAILED = "FAILED";
    private static final String BITBUCKET_DIRECTORY = "DIRECTORY";
    private static final String BITBUCKET_FILE = "FILE";
    private static final String BITBUCKET_CLOUD_FILE = "commit_file";
    private static final String FILE_CONTENT_FOR_BB_CLOUD = "/src/{hash}/{config}";
    private static final String FILE_CONTENT_FOR_BB_SERVER = "/raw/{config}?at={hash}";
    private static final String BROWSE_CONTENT_FOR_BB_SERVER = "/browse/{path}?at={branch}";
    private static final String BROWSE_CONTENT_FOR_BB_CLOUD_WITH_DEPTH_PARAM = "/src/{hash}/?pagelen=100&max_depth={depth}";
    private static final String BUILD_STATUS_KEY_FOR_CXFLOW = "cxflow";
    private static final String HTTP_BODY_IS_NULL = "Unable to download Config as code file. Response body is null.";
    private static final String CONTENT_NOT_FOUND_IN_RESPONSE = "Content not found in JSON response for Config as code";
    private final RestTemplate restTemplate;
    private final BitBucketProperties properties;
    private final FlowProperties flowProperties;
    private final ThresholdValidator thresholdValidator;
    private final ScmConfigOverrider scmConfigOverrider;
    private String browseRepoEndpoint = "";

    public BitBucketService(@Qualifier("flowRestTemplate") RestTemplate restTemplate,
                            BitBucketProperties properties,
                            FlowProperties flowProperties,
                            ThresholdValidator thresholdValidator,
                            ScmConfigOverrider scmConfigOverrider) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.thresholdValidator = thresholdValidator;
        this.scmConfigOverrider = scmConfigOverrider;
    }

    private static JSONObject getJSONComment(String comment) {
        JSONObject requestBody = new JSONObject();
        JSONObject content = new JSONObject();
        content.put("raw", comment);
        requestBody.put("content", content);
        return requestBody;
    }

    private static JSONObject getServerJSONComment(String comment) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("text", comment);
        return requestBody;
    }

    private HttpHeaders createAuthHeaders(String scmInstance) {
        String token = scmConfigOverrider.determineConfigToken(properties, scmInstance);
        String encoding = Base64.getEncoder().encodeToString(token.getBytes());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "Basic ".concat(encoding));
        httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return httpHeaders;
    }

    void processMerge(ScanRequest request, ScanResults results) throws BitBucketClientException {
        try {
            String comment = HTMLHelper.getMergeCommentMD(request, results, properties);
            log.debug(LOG_COMMENT, comment);
            sendMergeComment(request, comment);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating Merge Request comment", e);
            throw new BitBucketClientException();
        }
    }

    void processServerMerge(ScanRequest request, ScanResults results, ScanDetails scanDetails) throws BitBucketClientException {
        try {
            String comment = HTMLHelper.getMergeCommentMD(request, results, properties);
            log.debug(LOG_COMMENT, comment);

            if (properties.isBlockMerge()) {
                if (thresholdValidator.isMergeAllowed(results, properties, new PullRequestReport(scanDetails, request))) {
                    sendServerMergeComment(request, comment);
                } else {
                    sendServerMergeTask(request, comment);
                }
            } else {
                sendServerMergeComment(request, comment);
            }

        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating Merge Request comment", e);
            throw new BitBucketClientException();
        }
    }

    public void setBuildStartStatus(ScanRequest request) {

        if (properties.isBlockMerge()) {

            String cxBaseUrl = request.getAdditionalMetadata("cxBaseUrl");
            JSONObject buildStatusBody = createBuildStatusRequestBody(BUILD_IN_PROGRESS, BUILD_STATUS_KEY_FOR_CXFLOW, "Checkmarx Scan Initiated", cxBaseUrl.concat(CX_USER_SCAN_QUEUE), "Waiting for scan to complete..");
            sendBuildStatus(request, buildStatusBody.toString());
        }
    }

    public void setBuildEndStatus(ScanRequest request, ScanResults results, ScanDetails scanDetails) {

        if (properties.isBlockMerge()) {

            String status = BUILD_SUCCESSFUL;
            if (!thresholdValidator.isMergeAllowed(results, properties, new PullRequestReport(scanDetails, request))) {
                status = BUILD_FAILED;
            }

            JSONObject buildStatusBody = createBuildStatusRequestBody(status, BUILD_STATUS_KEY_FOR_CXFLOW, "Checkmarx Scan Results", results.getLink(), results.getScanSummary().toString());

            sendBuildStatus(request, buildStatusBody.toString());
        }
    }

    public void setBuildFailedStatus(ScanRequest request, String buildName, String buildUrl, String description) {
        if (properties.isBlockMerge()) {
            JSONObject buildStatusBody = createBuildStatusRequestBody(BUILD_FAILED, BUILD_STATUS_KEY_FOR_CXFLOW, buildName, buildUrl, description);
            sendBuildStatus(request, buildStatusBody.toString());
        }
    }

    private JSONObject createBuildStatusRequestBody(String buildState, String buildKey, String buildName, String buildUrl, String buildDescription) {
        JSONObject buildJsonBody = new JSONObject();
        buildJsonBody.put("state", buildState);
        buildJsonBody.put("key", buildKey);
        buildJsonBody.put("url", buildUrl);

        //Following fields are optional
        if (!ScanUtils.empty(buildName)) {
            buildJsonBody.put("name", buildName);
        }
        if (!ScanUtils.empty(buildDescription)) {
            buildJsonBody.put("description", buildDescription);
        }
        return buildJsonBody;
    }

    private void sendBuildStatus(ScanRequest request, String buildStatusRequestBody) {
        String buildStatusApiUrl = request.getAdditionalMetadata("buildStatusUrl");
        HttpEntity<String> httpEntity = new HttpEntity<>(buildStatusRequestBody, createAuthHeaders(request.getScmInstance()));
        restTemplate.exchange(buildStatusApiUrl, HttpMethod.POST, httpEntity, String.class);
    }

    public void sendMergeComment(ScanRequest request, String comment) {
        HttpEntity<String> httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), createAuthHeaders(request.getScmInstance()));
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    public void sendServerMergeComment(ScanRequest request, String comment) {
        HttpEntity<String> httpEntity = new HttpEntity<>(getServerJSONComment(comment).toString(), createAuthHeaders(request.getScmInstance()));
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    private void sendServerMergeTask(ScanRequest request, String comment) {

        ResponseEntity<String> retrievedResult = retrieveExistingOpenTasks(request);
        Object cxFlowTask = getCxFlowTask(retrievedResult);

        if (cxFlowTask != null) {
            Integer taskId = ((JSONObject) cxFlowTask).getInt("id");
            Integer taskVersion = ((JSONObject) cxFlowTask).getInt("version");

            JSONObject taskBody = new JSONObject();
            taskBody.put("id", taskId);
            taskBody.put("version", taskVersion);
            taskBody.put("severity", "BLOCKER");
            taskBody.put("text", comment);

            HttpEntity<String> httpEntity = new HttpEntity<>(taskBody.toString(), createAuthHeaders(request.getScmInstance()));
            restTemplate.exchange(request.getMergeNoteUri().concat("/" + taskId), HttpMethod.PUT, httpEntity, String.class);

        } else {
            JSONObject taskBody = new JSONObject();
            taskBody.put("severity", "BLOCKER");
            taskBody.put("text", comment);
            HttpEntity<String> httpEntity = new HttpEntity<>(taskBody.toString(), createAuthHeaders(request.getScmInstance()));
            restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
        }
    }

    private Object getCxFlowTask(ResponseEntity<String> retrievedResult) {

        if (retrievedResult.getBody() != null) {
            JSONObject json = new JSONObject(retrievedResult.getBody());
            JSONArray taskList = json.getJSONArray("values");

            if (!taskList.isEmpty()) {
                for (Object task : taskList) {
                    Object author = ((JSONObject) task).get("author");
                    String taskAuthor = (String) ((JSONObject) author).get("slug");

                    if (taskAuthor.equals(getCxFlowServiceAccountSlug())) {
                        return task;
                    }
                }
            } else {
                return null;
            }
        }
        return null;
    }

    private String getCxFlowServiceAccountSlug() {
        String[] basicAuthCredentials = properties.getToken().split(":");
        return basicAuthCredentials[0];

    }

    private ResponseEntity<String> retrieveExistingOpenTasks(ScanRequest request) {

        String blockerCommentUrl = request.getAdditionalMetadata("blocker-comment-url");

        Map<String, String> params = new HashMap<>();
        params.put("state", "OPEN");

        HttpEntity<Object> httpEntity = new HttpEntity<>(createAuthHeaders(request.getScmInstance()));
        return restTemplate.exchange(blockerCommentUrl.concat("?state={state}"), HttpMethod.GET, httpEntity, String.class, params);

    }

    void processCommit(ScanRequest request, ScanResults results) throws BitBucketClientException {
        try {
            String comment = HTMLHelper.getMergeCommentMD(request, results, properties);
            log.debug(LOG_COMMENT, comment);
            sendCommitComment(request, comment);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating Commit comment", e);
            throw new BitBucketClientException();
        }
    }

    private void sendCommitComment(ScanRequest request, String comment) {
        JSONObject note = new JSONObject();
        note.put("note", comment);
        HttpEntity<String> httpEntity = new HttpEntity<>(note.toString(), createAuthHeaders(request.getScmInstance()));
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    @Override
    public Sources getRepoContent(ScanRequest request) {

        log.debug("Auto profiling is enabled");
        if (ScanUtils.anyEmpty(request.getNamespace(), request.getRepoName(), request.getBranch())) {
            return null;
        }
        Sources sources = new Sources();
        browseRepoEndpoint = getBitbucketEndPoint(request);
        if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
            scanGitContentFromBitbucketServer(0, browseRepoEndpoint, request.getScmInstance(), sources);
        } else {
            scanGitContentFromBBCloud(browseRepoEndpoint, request.getScmInstance(), sources);
        }
        return sources;
    }

    private String getBitbucketEndPoint(ScanRequest request) {
        String repoSelfUrl = request.getAdditionalMetadata(REPO_SELF_URL);
        String endpoint;

        if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
            endpoint = repoSelfUrl.concat(BROWSE_CONTENT_FOR_BB_SERVER);
            endpoint = endpoint.replace("{branch}", request.getBranch());
        } else {
            endpoint = repoSelfUrl.concat(BROWSE_CONTENT_FOR_BB_CLOUD_WITH_DEPTH_PARAM);
            endpoint = endpoint.replace("{hash}", request.getHash());
            endpoint = endpoint.replace("{depth}", flowProperties.getProfilingDepth().toString());
        }
        return endpoint;
    }

    private void scanGitContentFromBBCloud(String endpoint, String scmInstance, Sources sources) {

        com.checkmarx.flow.dto.bitbucket.Content content = getRepoContentFromBBCloud(endpoint, scmInstance);
        List<com.checkmarx.flow.dto.bitbucket.Value> values = content.getValues();

        for (com.checkmarx.flow.dto.bitbucket.Value value : values) {
            String type = value.getType();
            if (type.equals(BITBUCKET_CLOUD_FILE)) {
                String fileName = value.getEscapedPath();
                String filePath = value.getPath();
                sources.addSource(filePath, fileName);
            }
        }
    }

    private com.checkmarx.flow.dto.bitbucket.Content getRepoContentFromBBCloud(String endpoint, String scmInstance) {
        log.info("Getting repo content from {}", endpoint);
        com.checkmarx.flow.dto.bitbucket.Content content = new com.checkmarx.flow.dto.bitbucket.Content();
        HttpHeaders headers = createAuthHeaders(scmInstance);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (response.getBody() == null) {
                log.warn(HTTP_BODY_IS_NULL);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            content = objectMapper.readValue(response.getBody(), com.checkmarx.flow.dto.bitbucket.Content.class);
            return content;
        } catch (NullPointerException e) {
            log.warn(CONTENT_NOT_FOUND_IN_RESPONSE);
        } catch (HttpClientErrorException e) {
            log.warn("Repo content is unavailable. The reason can be that branch has been deleted.");
        } catch (JsonProcessingException e) {
            log.error(String.format("Error in processing the JSON response from the repo. Error details : %s", ExceptionUtils.getRootCauseMessage(e)));
        }
        return content;
    }

    private Content getRepoContentFromBitbucketServer(String endpoint, String scmInstance) {
        log.info("Getting repo content from {}", endpoint);
        Content content = new Content();
        HttpHeaders headers = createAuthHeaders(scmInstance);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (response.getBody() == null) {
                log.warn(HTTP_BODY_IS_NULL);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            content = objectMapper.readValue(response.getBody(), Content.class);
            return content;
        } catch (NullPointerException e) {
            log.warn(CONTENT_NOT_FOUND_IN_RESPONSE);
        } catch (HttpClientErrorException e) {
            log.warn("Repo content is unavailable. The reason can be that branch has been deleted.");
        } catch (JsonProcessingException e) {
            log.error(String.format("Error in processing the JSON response from the repo. Error details : %s", ExceptionUtils.getRootCauseMessage(e)));
        }
        return content;
    }

    private void scanGitContentFromBitbucketServer(int depth, String endpoint, String scmInstance, Sources sources) {

        if (depth >= flowProperties.getProfilingDepth()) {
            return;
        }
        if (depth == 0) {
            endpoint = endpoint.replace("{path}", Strings.EMPTY);
        }

        Content content = getRepoContentFromBitbucketServer(endpoint, scmInstance);
        List<Value> values = content.getChildren().getValues();

        for (Value value : values) {
            String type = value.getType();
            if (type.equals(BITBUCKET_DIRECTORY)) {
                String directoryName = value.getPath().getToString();
                String fullDirectoryPath = content.getPath().getToString();
                fullDirectoryPath = fullDirectoryPath + PATH_SEPARATOR + directoryName;
                String directoryURL = browseRepoEndpoint.replace("{path}", fullDirectoryPath);
                scanGitContentFromBitbucketServer(depth + 1, directoryURL, scmInstance, sources);
            } else if (type.equals(BITBUCKET_FILE)) {
                String directoryName = content.getPath().getToString();
                String fileName = value.getPath().getToString();
                String fullPath = directoryName.concat("/").concat(fileName);
                sources.addSource(fullPath, fileName);
            }
        }
    }

    @Override
    public CxConfig getCxConfigOverride(ScanRequest request) {
        CxConfig result = null;
        if (StringUtils.isNotBlank(properties.getConfigAsCode())) {
            try {
                result = loadCxConfigFromBitbucket(request);
            } catch (NullPointerException e) {
                log.warn(CONTENT_NOT_FOUND_IN_RESPONSE);
            } catch (HttpClientErrorException.NotFound e) {
                log.info(String.format("No Config as code was found with the name: %s", properties.getConfigAsCode()));
            } catch (Exception e) {
                log.error(String.format("Error in getting config as code from the repo. Error details : %s", ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        return result;
    }

    @Override
    public void deleteComment(String url, ScanRequest scanRequest) {
        // not implemented
    }

    @Override
    public void updateComment(String commentUrl, String comment, ScanRequest scanRequest) {
        // not implemented
    }

    @Override
    public void addComment(ScanRequest scanRequest, String comment) {
        // not implemented
    }

    @Override
    public List<RepoComment> getComments(ScanRequest scanRequest) {
        return Collections.emptyList();
    }

    private CxConfig loadCxConfigFromBitbucket(ScanRequest request) {
        CxConfig cxConfig;
        HttpHeaders headers = createAuthHeaders(request.getScmInstance());
        String repoSelfUrl = request.getAdditionalMetadata(REPO_SELF_URL);

        String urlTemplate;
        if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
            urlTemplate = repoSelfUrl.concat(FILE_CONTENT_FOR_BB_SERVER);
        } else {
            urlTemplate = repoSelfUrl.concat(FILE_CONTENT_FOR_BB_CLOUD);
        }

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("hash", request.getHash());
        uriVariables.put("config", properties.getConfigAsCode());

        ResponseEntity<String> response = restTemplate.exchange(
                urlTemplate,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class, uriVariables
        );
        if (response.getBody() == null) {
            log.warn(HTTP_BODY_IS_NULL);
            cxConfig = null;
        } else {
            JSONObject json = new JSONObject(response.getBody());
            if (ScanUtils.empty(json.toString())) {
                log.warn(CONTENT_NOT_FOUND_IN_RESPONSE);
                cxConfig = null;
            } else {
                cxConfig = com.checkmarx.sdk.utils.ScanUtils.getConfigAsCode(json.toString());
            }
        }
        return cxConfig;
    }


    public String createIssue(ScanRequest request,
                              String title,
                              String description,
                              String assignee,
                              String priority) {
        throw new NotImplementedException("Not Implemented Exception");
    }

}
