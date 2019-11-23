package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.RepoIssue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.dto.github.Content;
import com.checkmarx.flow.exception.GitHubClientException;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class GitHubService extends RepoService {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private final FlowProperties flowProperties;
    private static final String FILE_CONTENT = "/{namespace}/{repo}/contents/{config}?ref={branch}";
    private static final String LANGUAGE_TYPES = "/{namespace}/{repo}/languages";
    private static final String REPO_CONTENT = "/{namespace}/{repo}/contents?ref={branch}";

    public GitHubService(@Qualifier("flowRestTemplate") RestTemplate restTemplate, GitHubProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    @PostConstruct
    private void postConstruct() {
        if(properties == null) return;
        String apiUrl = properties.getApiUrl();
        if(apiUrl != null){
            if(apiUrl.endsWith("/")){
                properties.setApiUrl(StringUtils.chop(apiUrl));
            }
        }
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
        HttpEntity httpEntity = new HttpEntity<>(RepoIssue.getJSONComment("body",comment).toString(), createAuthHeaders());
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    void startBlockMerge(ScanRequest request, String url){
        if(properties.isBlockMerge()) {
            HttpEntity httpEntity = new HttpEntity<>(
                    getJSONStatus("pending", url, "Checkmarx Scan Initiated").toString(),
                    createAuthHeaders()
            );
            if(ScanUtils.empty(request.getAdditionalMetadata("statuses_url"))){
                log.warn("statuses_url was not provided within the request object, which is required for blocking / unblocking pull requests");
                return;
            }
            restTemplate.exchange(request.getAdditionalMetadata("statuses_url"),
                    HttpMethod.POST, httpEntity, String.class);
        }
    }

    void endBlockMerge(ScanRequest request, String url, boolean findingsPresent){
        String state = "success";
        if(properties.isErrorMerge() && findingsPresent){
            state = "failure";
        }
        if(properties.isBlockMerge()) {
            HttpEntity httpEntity = new HttpEntity<>(
                    getJSONStatus(state, url, "Checkmarx Scan Completed").toString(),
                    createAuthHeaders()
            );
            if(ScanUtils.empty(request.getAdditionalMetadata("statuses_url"))){
                log.error("statuses_url was not provided within the request object, which is required for blocking / unblocking pull requests");
                return;
            }
            restTemplate.exchange(request.getAdditionalMetadata("statuses_url"),
                    HttpMethod.POST, httpEntity, String.class);
        }
    }

    void failBlockMerge(ScanRequest request, String url){
        if(properties.isBlockMerge()) {
            HttpEntity httpEntity = new HttpEntity<>(
                    getJSONStatus("failure", url, "Checkmarx Issue Threshold Met").toString(),
                    createAuthHeaders()
            );
            if(ScanUtils.empty(request.getAdditionalMetadata("statuses_url"))){
                log.error("statuses_url was not provided within the request object, which is required for blocking / unblocking pull requests");
                return;
            }
            restTemplate.exchange(request.getAdditionalMetadata("statuses_url"),
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
        String endpoint = properties.getApiUrl().concat(REPO_CONTENT);
        endpoint = endpoint.replace("{namespace}", request.getNamespace());
        endpoint = endpoint.replace("{repo}", request.getRepoName());
        endpoint = endpoint.replace("{branch}", request.getBranch());
        scanGitContent(0, endpoint, sources);
        return sources;
    }

    private Sources getRepoLanguagePercentages(ScanRequest request) {
        //"/{namespace}/{repo}/languages"
        Sources sources = new Sources();
        Map<String, Long> langs = new HashMap<>();
        Map<String, Integer> langsPercent = new HashMap<>();
        HttpHeaders headers = createAuthHeaders();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getApiUrl().concat(LANGUAGE_TYPES),
                    HttpMethod.GET,
                    new HttpEntity(headers),
                    String.class,
                    request.getNamespace(),
                    request.getRepoName(),
                    request.getBranch()
            );
            if(response.getBody() == null){
                log.warn("HTTP Body is null for content api ");
            }
            else {
                JSONObject json = new JSONObject(response.getBody());
                Iterator<String> keys = json.keys();
                Long total = 0L;
                while(keys.hasNext()) {
                    String key = keys.next();
                    Long bytes = json.getLong(key);
                    langs.put(key, bytes);
                    total += bytes;
                }
                for (Map.Entry<String,Long> entry : langs.entrySet()){
                    Long bytes = entry.getValue();
                    Double percentage = (Double.valueOf(bytes) / Double.valueOf(total) * 100);
                    langsPercent.put(entry.getKey(), percentage.intValue());
                }
                sources.setLanguageStats(langsPercent);
            }
        }catch (NullPointerException e){
            log.warn("Content not found in JSON response");
        }catch (HttpClientErrorException.NotFound e){
            log.error(ExceptionUtils.getStackTrace(e));
        }catch (HttpClientErrorException e){
            log.error(ExceptionUtils.getRootCauseMessage(e));
        }
        return sources;
    }

    private List<Content> getRepoContent(String endpoint) {
        //"/{namespace}/{repo}/languages"
        HttpHeaders headers = createAuthHeaders();
        try {
            ResponseEntity<Content[]> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    new HttpEntity(headers),
                    Content[].class
            );
            if(response.getBody() == null){
                log.warn("HTTP Body is null for content api ");
            }
            return Arrays.asList(response.getBody());
        }catch (NullPointerException e){
            log.warn("Content not found in JSON response");
        }catch (HttpClientErrorException.NotFound e){
            log.error(ExceptionUtils.getStackTrace(e));
        }catch (HttpClientErrorException e){
            log.error(ExceptionUtils.getRootCauseMessage(e));
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
        //"/{namespace}/{repo}/contents/{config}?ref={branch}"
        HttpHeaders headers = createAuthHeaders();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getApiUrl().concat(FILE_CONTENT),
                    HttpMethod.GET,
                    new HttpEntity(headers),
                    String.class,
                    request.getNamespace(),
                    request.getRepoName(),
                    properties.getConfigAsCode(),
                    request.getBranch()
            );
            if(response.getBody() == null){
                log.warn("HTTP Body is null for content api ");
            }
            else {
                JSONObject json = new JSONObject(response.getBody());
                String content = json.getString("content");
                if(ScanUtils.empty(content)){
                    log.warn("Content not found in JSON response");
                    return null;
                }
                String decodedContent = new String(Base64.decodeBase64(content.trim()));
                return com.checkmarx.sdk.utils.ScanUtils.getConfigAsCode(decodedContent);
            }
        }catch (NullPointerException e){
            log.warn("Content not found in JSON response");
        }catch (HttpClientErrorException.NotFound e){
            log.info("No Config As code was found [{}]", properties.getConfigAsCode());
        }catch (HttpClientErrorException e){
            log.error(ExceptionUtils.getRootCauseMessage(e));
        }catch (Exception e){
            log.error(ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }

}