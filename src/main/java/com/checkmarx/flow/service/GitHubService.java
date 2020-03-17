package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.RepoIssue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.dto.github.Content;
import com.checkmarx.flow.exception.GitHubClientException;
import com.checkmarx.flow.exception.GitHubClientRunTimeException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.ScanResults;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.*;

@Service
public class GitHubService extends RepoService {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private static final String HTTP_BODY_IS_NULL = "HTTP Body is null for content api ";
    private static final String CONTENT_NOT_FOUND_IN_RESPONSE = "Content not found in JSON response";
    private static final String STATUSES_URL_KEY = "statuses_url";
    private static final String STATUSES_URL_NOT_PROVIDED = "statuses_url was not provided within the request object, which is required for blocking / unblocking pull requests";

    public static final String MERGE_SUCCESS_DESCRIPTION = "Checkmarx Scan Completed";
    public static final String MERGE_FAILURE_DESCRIPTION = "Checkmarx Scan completed. Vulnerability scan failed";
    public static final String MERGE_SUCCESS = "success";
    public static final String MERGE_FAILURE = "failure";

    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private final FlowProperties flowProperties;
    private final MergeResultEvaluator mergeResultEvaluator;

    private static final String FILE_CONTENT = "/{namespace}/{repo}/contents/{config}?ref={branch}";
    private static final String LANGUAGE_TYPES = "/{namespace}/{repo}/languages";
    private static final String REPO_CONTENT = "/{namespace}/{repo}/contents?ref={branch}";

    public GitHubService(@Qualifier("flowRestTemplate") RestTemplate restTemplate,
                         GitHubProperties properties,
                         FlowProperties flowProperties,
                         MergeResultEvaluator mergeResultEvaluator) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.mergeResultEvaluator = mergeResultEvaluator;
    }

    private HttpHeaders createAuthHeaders(){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "token ".concat(properties.getToken()));
        return httpHeaders;
    }

    void processPull(ScanRequest request, ScanResults results) throws GitHubClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);
            sendMergeComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Merge Request comment: {}", ExceptionUtils.getRootCauseMessage(e));
            throw new GitHubClientException();
        }
    }

    void sendMergeComment(ScanRequest request, String comment){
        HttpEntity<?> httpEntity = new HttpEntity<>(RepoIssue.getJSONComment("body",comment).toString(), createAuthHeaders());
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    void startBlockMerge(ScanRequest request, String url){
        if(properties.isBlockMerge()) {
            HttpEntity<?> httpEntity = new HttpEntity<>(
                    getJSONStatus("pending", url, "Checkmarx Scan Initiated").toString(),
                    createAuthHeaders()
            );
            if(ScanUtils.empty(request.getAdditionalMetadata(STATUSES_URL_KEY))){
                log.warn(STATUSES_URL_NOT_PROVIDED);
                return;
            }
            restTemplate.exchange(request.getAdditionalMetadata(STATUSES_URL_KEY),
                    HttpMethod.POST, httpEntity, String.class);
        }
    }

    void endBlockMerge(ScanRequest request, ScanResults results){
        if(properties.isBlockMerge()) {
            HttpEntity<String> httpEntity = getStatusRequestEntity(results);
            if(ScanUtils.empty(request.getAdditionalMetadata(STATUSES_URL_KEY))){
                log.error(STATUSES_URL_NOT_PROVIDED);
                return;
            }
            restTemplate.exchange(request.getAdditionalMetadata(STATUSES_URL_KEY),
                    HttpMethod.POST, httpEntity, String.class);
        }
    }

    private HttpEntity<String> getStatusRequestEntity(ScanResults results) {
        String state;
        String description;
        if (mergeResultEvaluator.isMergeAllowed(results, properties)) {
            state = MERGE_SUCCESS;
            description = MERGE_SUCCESS_DESCRIPTION;
        } else {
            state = MERGE_FAILURE;
            description = MERGE_FAILURE_DESCRIPTION;
        }

        JSONObject requestBody = getJSONStatus(state, results.getLink(), description);
        return new HttpEntity<>(requestBody.toString(), createAuthHeaders());
    }

    void failBlockMerge(ScanRequest request, String url){
        if(properties.isBlockMerge()) {
            HttpEntity<?> httpEntity = new HttpEntity<>(
                    getJSONStatus(MERGE_FAILURE, url, "Checkmarx Issue Threshold Met").toString(),
                    createAuthHeaders()
            );
            if(ScanUtils.empty(request.getAdditionalMetadata(STATUSES_URL_KEY))){
                log.error(STATUSES_URL_NOT_PROVIDED);
                return;
            }
            restTemplate.exchange(request.getAdditionalMetadata(STATUSES_URL_KEY),
                    HttpMethod.POST, httpEntity, String.class);
        }
    }

    private JSONObject getJSONStatus(String state, String url, String description){
        JSONObject requestBody = new JSONObject();
        requestBody.put("state", state);
        requestBody.put("target_url", url);
        requestBody.put("description", description);
        requestBody.put("context", "checkmarx");
        return requestBody;
    }

    @Override
    public Sources getRepoContent(ScanRequest request) {
        log.debug("Auto profiling is enabled");
        if(ScanUtils.anyEmpty(request.getNamespace(), request.getRepoName(), request.getBranch())){
            return null;
        }
        Sources sources = getRepoLanguagePercentages(request);
        String endpoint = getGitHubEndPoint(request);
        scanGitContent(0, endpoint, sources);
        return sources;
    }

    private String getGitHubEndPoint(ScanRequest request) {
        String endpoint = properties.getApiUrl().concat(REPO_CONTENT);
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
        HttpHeaders headers = createAuthHeaders();

        String urlTemplate = properties.getApiUrl().concat(LANGUAGE_TYPES);
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
                    double percentage = (Double.valueOf(bytes) / Double.valueOf(total) * 100);
                    langsPercent.put(entry.getKey(), (int) percentage);
                }
                sources.setLanguageStats(langsPercent);
            }
        } catch (NullPointerException e) {
            log.warn(CONTENT_NOT_FOUND_IN_RESPONSE);
        }catch (HttpClientErrorException.NotFound e){
            String error = "Got 404 'Not Found' error. GitHub endpoint: " + getGitHubEndPoint(request) + " is invalid.";
            throw new GitHubClientRunTimeException(error, e);
        }catch (HttpClientErrorException e){
            log.error(ExceptionUtils.getRootCauseMessage(e));
        }
        return sources;
    }

    private List<Content> getRepoContent(String endpoint) {
        log.info("Getting repo content from {}", endpoint);
        //"/{namespace}/{repo}/languages"
        HttpHeaders headers = createAuthHeaders();
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
            log.error("Error getting repo content.", e);
        }
        return Collections.emptyList();
    }


    private void scanGitContent(int depth, String endpoint, Sources sources){
        if(depth >= flowProperties.getProfilingDepth()){
            return;
        }
        List<Content> contents = getRepoContent(endpoint);
        for(Content content: contents){
            if(content.getType().equals("dir")){
                scanGitContent(depth + 1, content.getUrl(), sources);
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
                result = loadCxConfigFromGitHub(properties.getConfigAsCode(), request);
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

    private CxConfig loadCxConfigFromGitHub(String filename, ScanRequest request) {
        HttpHeaders headers = createAuthHeaders();
        String urlTemplate = properties.getApiUrl().concat(FILE_CONTENT);
        ResponseEntity<String> response = restTemplate.exchange(
                urlTemplate,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                request.getNamespace(),
                request.getRepoName(),
                filename,
                request.getBranch()
        );
        if (response.getBody() == null) {
            log.warn(HTTP_BODY_IS_NULL);
            return null;
        } else {
            JSONObject json = new JSONObject(response.getBody());
            String content = json.getString("content");
            if (ScanUtils.empty(content)) {
                log.warn(CONTENT_NOT_FOUND_IN_RESPONSE);
                return null;
            }
            String decodedContent = new String(Base64.decodeBase64(content.trim()));
            return com.checkmarx.sdk.utils.ScanUtils.getConfigAsCode(decodedContent);
        }
    }
}