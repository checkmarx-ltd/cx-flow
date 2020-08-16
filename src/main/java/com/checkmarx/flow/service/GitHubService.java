package com.checkmarx.flow.service;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.github.Content;
import com.checkmarx.flow.dto.report.AnalyticsReport;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.flow.exception.GitHubClientRunTimeException;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class GitHubService extends RepoService {
    private static final String HTTP_BODY_IS_NULL = "HTTP Body is null for content api ";
    private static final String CONTENT_NOT_FOUND_IN_RESPONSE = "Content not found in JSON response";
    private static final String STATUSES_URL_KEY = "statuses_url";
    private static final String STATUSES_URL_NOT_PROVIDED = "statuses_url was not provided within the request object, which is required for blocking / unblocking pull requests";


    public static final String MERGE_SUCCESS = "success";
    public static final String MERGE_FAILURE = "failure";
    private static final String MERGE_ERROR = "error";

    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private final FlowProperties flowProperties;
    private final ThresholdValidator thresholdValidator;
    private final ScmConfigOverrider scmConfigOverrider;

    private static final String FILE_CONTENT = "/{namespace}/{repo}/contents/{config}?ref={branch}";
    private static final String LANGUAGE_TYPES = "/{namespace}/{repo}/languages";
    private static final String REPO_CONTENT = "/{namespace}/{repo}/contents?ref={branch}";

    private static final String API_REQUEST = "API request: {}";
    private static final String API_RESPONSE = "API response: {}";
    private static final String API_RESPONSE_CODE = "API response code: {}";
    private static final String REQUEST_DETAILS = "; request details: %s";

    public static final String CX_USER_SCAN_QUEUE = "/CxWebClient/UserQueue.aspx";

    public GitHubService(@Qualifier("flowRestTemplate") RestTemplate restTemplate,
                         GitHubProperties properties,
                         FlowProperties flowProperties,
                         ThresholdValidator thresholdValidator,
                         ScmConfigOverrider scmConfigOverrider) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.thresholdValidator = thresholdValidator;
        this.scmConfigOverrider = scmConfigOverrider;
    }

    private HttpHeaders createAuthHeaders(ScanRequest scanRequest){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "token ".concat(scmConfigOverrider.determineConfigToken(properties, scanRequest)));
        return httpHeaders;
    }

    void processPull(ScanRequest request, ScanResults results) {
            String comment = HTMLHelper.getMergeCommentMD(request, results, properties);
            log.debug("comment: {}", comment);
            sendMergeComment(request, comment);
    }

    private void updateComment(String baseUrl, String comment, ScanRequest scanRequest) {
        log.debug("Updating exisiting comment. url: {}", baseUrl);
        log.debug("Updated comment: {}" , comment);
        HttpEntity<?> httpEntity = new HttpEntity<>(RepoIssue.getJSONComment("body",comment).toString(), createAuthHeaders(scanRequest));
        restTemplate.exchange(baseUrl, HttpMethod.PATCH, httpEntity, String.class);
    }

    public List<RepoComment> getComments(ScanRequest scanRequest) throws IOException  {
        HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders(scanRequest));
        ResponseEntity<String> response = restTemplate.exchange(scanRequest.getMergeNoteUri(), HttpMethod.GET, httpEntity , String.class);
        List<RepoComment> result = new ArrayList<>();
        ObjectMapper objMapper = new ObjectMapper();
        JsonNode root = objMapper.readTree(response.getBody());
        Iterator<JsonNode> it = root.getElements();
        while (it.hasNext()) {
            JsonNode commentNode = it.next();
            RepoComment comment = createRepoComment(commentNode);
            if (PullRequestCommentsHelper.isCheckMarxComment(comment)) {
                result.add(comment);
            }
        }
        return result;
    }

    public void deleteComment(String url, ScanRequest scanRequest) {
        HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders(scanRequest));
        restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, String.class);
    }


    private RepoComment createRepoComment(JsonNode commentNode)  {
        String commentBody = commentNode.path("body").getTextValue();
        long id = commentNode.path("id").asLong();
        String commentUrl = commentNode.path(("url")).asText();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String updatedStr = commentNode.path("updated_at").asText();
        String createdStr = commentNode.path("created_at").asText();
        try {
            return new RepoComment(id, commentBody, commentUrl, sdf.parse(createdStr), sdf.parse(updatedStr));
        }
        catch (ParseException pe) {
            throw new GitHubClientRunTimeException("Error parsing github pull request created or updted date", pe);
        }
    }

    public void sendMergeComment(ScanRequest request, String comment) {
        try {
            RepoComment commentToUpdate = PullRequestCommentsHelper.getCommentToUpdate(getComments(request), comment);
            if (commentToUpdate !=  null) {
                log.debug("Got candidate comment to update. comment: {}", commentToUpdate.getComment());
                if (!PullRequestCommentsHelper.shouldUpdateComment(comment, commentToUpdate.getComment())) {
                    log.debug("sendMergeComment: Comment should not be updated");
                    return;
                }
                log.debug("sendMergeComment: Going to update GitHub pull request comment");
                updateComment(commentToUpdate.getCommentUrl(), comment, request);
            } else {
                log.debug("sendMergeComment: Going to create a new GitHub pull request comment");
                addComment(request, comment);
            }
        }
        catch (Exception e) {
            // We "swallow" the exception so that the flow will not be terminated because of errors in GIT comments
            log.error("Error while adding or updating repo pull request comment", e);
        }
    }

    private void addComment(ScanRequest request, String comment) {
        log.debug("Adding a new comment");
        HttpEntity<?> httpEntity = new HttpEntity<>(RepoIssue.getJSONComment("body",comment).toString(), createAuthHeaders(request));
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    public void startBlockMerge(ScanRequest request, String url){
        if(properties.isBlockMerge()) {
            final String PULL_REQUEST_STATUS = "pending";
            HttpEntity<?> httpEntity = new HttpEntity<>(
                    getJSONStatus(PULL_REQUEST_STATUS, url, "Checkmarx Scan Initiated").toString(),
                    createAuthHeaders(request)
            );
            String statusApiUrl = request.getAdditionalMetadata(STATUSES_URL_KEY);
            if (ScanUtils.empty(statusApiUrl)) {
                log.error(STATUSES_URL_NOT_PROVIDED);
                return;
            }
            log.debug("Setting pull request status to '{}': {}", PULL_REQUEST_STATUS, statusApiUrl);

            String logErrorMessage = String.format("failed to set pull request status to %s", PULL_REQUEST_STATUS);
            statusExchange(request, httpEntity, statusApiUrl, logErrorMessage);
        }
    }

    private void statusExchange(ScanRequest request, HttpEntity<?> httpEntity, String statusApiUrl, String message) {
        log.trace(API_REQUEST, httpEntity.getBody());
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(statusApiUrl, HttpMethod.POST, httpEntity, String.class);
            log.debug(API_RESPONSE_CODE, responseEntity.getStatusCode());
            log.trace(API_RESPONSE, responseEntity);
        } catch (RestClientException e) {
            String msg = message;
            if (log.isDebugEnabled()) {
                msg += String.format(REQUEST_DETAILS, request.toString());
            }
            log.warn(msg, e);
        }
    }

    void endBlockMerge(ScanRequest request, ScanResults results, ScanDetails scanDetails) {
        logPullRequestWithScaResults(request, results);

        if (properties.isBlockMerge()) {
            String statusApiUrl = request.getAdditionalMetadata(STATUSES_URL_KEY);
            if (ScanUtils.empty(statusApiUrl)) {
                log.error(STATUSES_URL_NOT_PROVIDED);
                return;
            }

            PullRequestReport report = new PullRequestReport(scanDetails, request);

            HttpEntity<String> httpEntity = getStatusRequestEntity(results, report, request);

            logPullRequestWithSastOsa(results, report);

            log.debug("Updating pull request status: {}", statusApiUrl);
            statusExchange(request, httpEntity, statusApiUrl, "failed to update merge status for completed scan");
        } else {
            log.debug("Pull request blocking is disabled in configuration, no need to unblock.");

            logPullRequestWithSastOsa(request, results, scanDetails);
        }
    }

    private void logPullRequestWithSastOsa(ScanRequest request, ScanResults results, ScanDetails scanDetails) {

        //Report pull request only if there was SAST/OSA scan
        //Otherwise it would be only SCA
        if(hasSastOsaScan(results)) {
            PullRequestReport report = new PullRequestReport(scanDetails, request);
            Map<FindingSeverity, Integer> findings = ThresholdValidatorImpl.getSastFindingCountPerSeverity(results);
            report.setFindingsPerSeverity(findings);
            report.setPullRequestResult(OperationResult.successful());
            report.log();
        }
    }

    private boolean hasSastOsaScan(ScanResults results) {
        return results.getSastScanId() != null || Boolean.TRUE.equals(results.getOsa()) || results.getScanSummary() != null;
    }

    private void logPullRequestWithSastOsa(ScanResults results, PullRequestReport report) {
        //Report pull request only if there was SAST/OSA scan
        //Otherwise it would be only SCA
        if(hasSastOsaScan(results)) {
            report.log();
        }
    }

    private void logPullRequestWithScaResults(ScanRequest request, ScanResults results) {
        if(results.getScaResults() != null ) {
            PullRequestReport report = new PullRequestReport(results.getScaResults().getScanId(), request, AnalyticsReport.SCA);
            report.setFindingsPerSeveritySca(results);
            report.setPullRequestResult(OperationResult.successful());
            report.log();
        }
    }

    private HttpEntity<String> getStatusRequestEntity(ScanResults results, PullRequestReport pullRequestReport, ScanRequest scanRequest) {
        String statusForApi = MERGE_SUCCESS;

        if (!thresholdValidator.isMergeAllowed(results, properties, pullRequestReport)) {
            statusForApi = MERGE_FAILURE;
        }

        log.debug("Setting pull request status to '{}'.", statusForApi);
        JSONObject requestBody = getJSONStatus(statusForApi, results.getLink(), pullRequestReport.getPullRequestResult().getMessage());
        return new HttpEntity<>(requestBody.toString(), createAuthHeaders(scanRequest));
    }

    void failBlockMerge(ScanRequest request, String url){
        if(properties.isBlockMerge()) {
            HttpEntity<?> httpEntity = new HttpEntity<>(
                    getJSONStatus(MERGE_FAILURE, url, "Checkmarx Issue Threshold Met").toString(),
                    createAuthHeaders(request)
            );
            String statusApiUrl = request.getAdditionalMetadata(STATUSES_URL_KEY);
            if (ScanUtils.empty(statusApiUrl)) {
                log.error(STATUSES_URL_NOT_PROVIDED);
                return;
            }
            log.debug("Setting pull request status: {}", statusApiUrl);
            statusExchange(request, httpEntity, statusApiUrl, "failed to set merge status to failure");
        }
    }

    private static JSONObject getJSONStatus(String state, String url, String description){
        JSONObject requestBody = new JSONObject();
        requestBody.put("state", state);
        requestBody.put("context", "checkmarx");

        if(!ScanUtils.empty(url)) {
            requestBody.put("target_url", url);
        }
        if(!ScanUtils.empty(description)) {
            requestBody.put("description", description);
        }
        return requestBody;
    }

    void errorBlockMerge(ScanRequest request, String targetURL, String description){
        if(properties.isBlockMerge()) {
            JSONObject jsonRequestBody = getJSONStatus(MERGE_ERROR, targetURL, description);
            HttpEntity<?> httpEntity = new HttpEntity<>(jsonRequestBody.toString(), createAuthHeaders(request));
            String statusApiUrl = request.getAdditionalMetadata(STATUSES_URL_KEY);
            if (ScanUtils.empty(statusApiUrl)) {
                log.error(STATUSES_URL_NOT_PROVIDED);
                return;
            }
            log.debug("Setting pull request status: {}", statusApiUrl);
            statusExchange(request, httpEntity, statusApiUrl, "failed to set merge status to failure");
        }
    }

    @Override
    public Sources getRepoContent(ScanRequest request) {
        log.debug("Auto profiling is enabled");
        if(ScanUtils.anyEmpty(request.getNamespace(), request.getRepoName(), request.getBranch())){
            return null;
        }
        Sources sources = getRepoLanguagePercentages(request);
        String endpoint = getGitHubEndPoint(request);
        scanGitContent(0, endpoint, sources, request);
        return sources;
    }

    private String getGitHubEndPoint(ScanRequest request) {
        String endpoint = scmConfigOverrider.determineConfigApiUrl(properties, request).concat(REPO_CONTENT);
        endpoint = endpoint.replace("{namespace}", request.getNamespace());
        endpoint = endpoint.replace("{repo}", request.getRepoName());
        endpoint = endpoint.replace("{branch}", request.getBranch());
        return endpoint;
    }

    private Sources getRepoLanguagePercentages(ScanRequest request) {
        //"/{namespace}/{repo}/languages"
        Sources sources = new Sources();
        Map<String, Long> langs = new HashMap<>();
        Map<String, Integer> langsPercent = new HashMap<>();
        HttpHeaders headers = createAuthHeaders(request);

        String urlTemplate = scmConfigOverrider.determineConfigApiUrl(properties, request).concat(LANGUAGE_TYPES);
        String url = new DefaultUriBuilderFactory()
                .expand(urlTemplate, request.getNamespace(), request.getRepoName())
                .toString();

        log.info("Getting repo languages from {}", url);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if(response.getBody() == null){
                log.warn(HTTP_BODY_IS_NULL);
            }
            else {
                JSONObject json = new JSONObject(response.getBody());
                Iterator<String> keys = json.keys();
                long total = 0L;
                while(keys.hasNext()) {
                    String key = keys.next();
                    long bytes = json.getLong(key);
                    langs.put(key, bytes);
                    total += bytes;
                }
                for (Map.Entry<String,Long> entry : langs.entrySet()){
                    Long bytes = entry.getValue();
                    double percentage = 0;
                    if (total != 0L) {
                        percentage = (Double.valueOf(bytes) / (double) total * 100);
                    }
                    langsPercent.put(entry.getKey(), (int) percentage);
                }
                sources.setLanguageStats(langsPercent);
            }
        } catch (NullPointerException e) {
            log.warn(CONTENT_NOT_FOUND_IN_RESPONSE);
        }catch (HttpClientErrorException.NotFound e){
            String error = "Got 404 'Not Found' error. GitHub endpoint: " + getGitHubEndPoint(request) + " is invalid.";
            log.warn(error);
        }catch (HttpClientErrorException e){
            log.error(ExceptionUtils.getRootCauseMessage(e));
        }
        return sources;
    }

    private List<Content> getRepoContent(String endpoint, ScanRequest scanRequest) {
        log.info("Getting repo content from {}", endpoint);
        //"/{namespace}/{repo}/languages"
        HttpHeaders headers = createAuthHeaders(scanRequest);
        try {
            ResponseEntity<Content[]> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Content[].class
            );
            if(response.getBody() == null){
                log.warn(HTTP_BODY_IS_NULL);
            }
            return Arrays.asList(response.getBody());
        } catch (NullPointerException e) {
            log.warn(CONTENT_NOT_FOUND_IN_RESPONSE);
        } catch (HttpClientErrorException e) {
            log.warn("Repo content is unavailable. The reason can be that branch has been deleted.");
        }
        return Collections.emptyList();
    }


    private void scanGitContent(int depth, String endpoint, Sources sources, ScanRequest scanRequest){
        if(depth >= flowProperties.getProfilingDepth()){
            return;
        }
        List<Content> contents = getRepoContent(endpoint, scanRequest);
        for(Content content: contents){
            if(content.getType().equals("dir")){
                scanGitContent(depth + 1, content.getUrl(), sources, scanRequest);
            }
            else if (content.getType().equals("file")){
                sources.addSource(content.getPath(), content.getName());
            }
        }
    }

    @Override
    public CxConfig getCxConfigOverride(ScanRequest request) {
        CxConfig result = null;
        if (StringUtils.isNotBlank(properties.getConfigAsCode())) {
            try {
                result = loadConfigAsCode(properties.getConfigAsCode(), request);
            } catch (NullPointerException e) {
                log.warn(CONTENT_NOT_FOUND_IN_RESPONSE);
            } catch (HttpClientErrorException.NotFound e) {
                log.info("No Config As code was found [{}]", properties.getConfigAsCode());
            } catch (Exception e) {
                log.error(ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return result;
    }

    private CxConfig loadConfigAsCode(String filename, ScanRequest request) {
        CxConfig result = null;

        String effectiveBranch = determineConfigAsCodeBranch(request);
        String fileContent = downloadFileContent(filename, request, effectiveBranch);
        if (fileContent == null) {
            log.warn(HTTP_BODY_IS_NULL);
        } else {
            JSONObject json = new JSONObject(fileContent);
            String content = json.getString("content");
            if (ScanUtils.empty(content)) {
                log.warn(CONTENT_NOT_FOUND_IN_RESPONSE);
            } else {
                String decodedContent = new String(Base64.decodeBase64(content.trim()));
                result = com.checkmarx.sdk.utils.ScanUtils.getConfigAsCode(decodedContent);
            }
        }

        return result;
    }

    private String downloadFileContent(String filename, ScanRequest request, String branch) {
        ResponseEntity<String> response = null;

        if (StringUtils.isNotEmpty(branch)) {
            HttpHeaders headers = createAuthHeaders(request);
            String urlTemplate = scmConfigOverrider.determineConfigApiUrl(properties, request).concat(FILE_CONTENT);

            response = restTemplate.exchange(
                    urlTemplate,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class,
                    request.getNamespace(),
                    request.getRepoName(),
                    filename,
                    branch);
        } else {
            log.warn("Unable to load config-as-code.");
        }

        return response != null ? response.getBody() : null;
    }

    private String determineConfigAsCodeBranch(ScanRequest request) {
        String result;
        log.debug("Determining a branch to get config-as-code from.");
        if (properties.isUseConfigAsCodeFromDefaultBranch()) {
            result = tryGetDefaultBranch(request);
        } else {
            result = tryGetCurrentBranch(request);
        }
        return result;
    }

    private static String tryGetCurrentBranch(ScanRequest request) {
        String result = null;
        if (StringUtils.isNotEmpty(request.getBranch())) {
            result = request.getBranch();
            log.debug("Using the current branch ({}) to get config-as-code.", result);
        }
        else {
            log.warn("Tried to use current branch to get config-as-code. " +
                    "However, current branch couldn't be determined.");
        }
        return result;
    }

    private static String tryGetDefaultBranch(ScanRequest request) {
        String result = null;
        if (StringUtils.isNotEmpty(request.getDefaultBranch())) {
            result = request.getDefaultBranch();
            log.debug("Using the default branch ({}) to get config-as-code.", result);
        }
        else {
            log.warn("Configuration indicates that default branch must be used to get config-as-code. " +
                    "However, default branch couldn't be determined.");
        }
        return result;
    }
}
