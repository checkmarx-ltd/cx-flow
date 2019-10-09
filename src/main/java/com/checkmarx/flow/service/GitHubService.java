package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.RepoIssue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.exception.GitHubClientException;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GitHubService extends RepoService {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private final FlowProperties flowProperties;
    private static final String FILE_CONENT = "/{namespace}/{repo}/contents/{config}?ref={branch}";

    public GitHubService(@Qualifier("flowRestTemplate") RestTemplate restTemplate, GitHubProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    private HttpHeaders createAuthHeaders(){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "token ".concat(properties.getToken()));
        return httpHeaders;
    }

    Map<String, List<String>> process(ScanResults results, ScanRequest request) throws GitHubClientException {
        return null;
    }

    void processPull(ScanRequest request, ScanResults results) throws GitHubClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);
            sendMergeComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Merge Request comment");
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
                log.error("statuses_url was not provided within the request object, which is required for blocking / unblocking pull requests");
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
    public Sources getRepoContent() {
        return null;
    }

    @Override
    public CxConfig getCxConfigOverride(ScanRequest request) throws CheckmarxException {
        //"/{namespace}/{repo}/contents/{config}?ref={branch}"
        HttpHeaders headers = createAuthHeaders();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getApiUrl().concat(FILE_CONENT),
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
                String decodedContent = new String(Base64.getDecoder().decode(content.trim()));
                return com.checkmarx.sdk.utils.ScanUtils.getConfigAsCode(decodedContent);
            }
        }catch (NullPointerException e){
            log.warn("Content not found in JSON response");
        }catch (HttpClientErrorException.NotFound e){
            log.info("No Config As code was found [{}]", properties.getConfigAsCode());
        }catch (HttpClientErrorException e){
            log.error(ExceptionUtils.getRootCauseMessage(e));
        }
        catch (Exception e){
            log.error(ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }
}