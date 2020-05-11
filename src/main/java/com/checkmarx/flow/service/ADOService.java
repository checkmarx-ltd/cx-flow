package com.checkmarx.flow.service;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.azure.CreateWorkItemAttr;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.flow.exception.ADOClientException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.ScanResults;
import org.json.JSONArray;
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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ADOService {
    private static final Logger log = LoggerFactory.getLogger(ADOService.class);
    private static final String API_VERSION = "?api-version=";
    private final RestTemplate restTemplate;
    private final ADOProperties properties;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;

    public ADOService(@Qualifier("flowRestTemplate") RestTemplate restTemplate, ADOProperties properties, FlowProperties flowProperties, CxProperties cxProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
    }

    private HttpHeaders createAuthHeaders(){
        String encoding = Base64.getEncoder().encodeToString(":".concat(properties.getToken()).getBytes());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", "application/json");
        httpHeaders.set("Authorization", "Basic ".concat(encoding));
        httpHeaders.set("Accept", "application/json");
        return httpHeaders;
    }

    private HttpHeaders createPatchAuthHeaders(){
        HttpHeaders httpHeaders = createAuthHeaders();
        httpHeaders.set("Content-Type", "application/json-patch+json");
        return httpHeaders;
    }

    void processPull(ScanRequest request, ScanResults results) throws ADOClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);
            sendMergeComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Merge Request comment", e);
            throw new ADOClientException();
        }
    }

    void sendMergeComment(ScanRequest request, String comment){
        String mergeUrl = request.getMergeNoteUri();
        if(ScanUtils.empty(mergeUrl)){
            log.error("mergeUrl was not provided within the request object, which is required for commenting on pull request");
            return;
        }
        log.debug(mergeUrl);
        String threadId = request.getAdditionalMetadata("ado_thread_id");
        if(ScanUtils.empty(threadId)){
            HttpEntity<String> httpEntity = new HttpEntity<>(getJSONThread(comment).toString(), createAuthHeaders());
            log.debug("Creating new thread for comments");
            ResponseEntity<String> response = restTemplate.exchange(mergeUrl.concat(API_VERSION).concat(properties.getApiVersion()),
                    HttpMethod.POST, httpEntity, String.class);
            if(response.getBody() != null) {
                JSONObject json = new JSONObject(response.getBody());
                int id = json.getInt("id");
                request.putAdditionalMetadata("ado_thread_id", Integer.toString(id));
                log.debug("Created new thread with Id {}", id);
            }
        }
        else{
            HttpEntity<String> httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), createAuthHeaders());
            mergeUrl = mergeUrl.concat("/").concat(threadId).concat("/comments");
            log.debug("Adding comment to thread Id {}", threadId);
            restTemplate.exchange(mergeUrl.concat(API_VERSION).concat(properties.getApiVersion()),
                    HttpMethod.POST, httpEntity, String.class);
        }
    }

    void startBlockMerge(ScanRequest request){
        if(properties.isBlockMerge()) {
            String url = request.getAdditionalMetadata("statuses_url");
            if(ScanUtils.empty(url)){
                log.warn("No status url found, skipping status update");
                return;
            }
            int statusId = createStatus("pending","Checkmarx Scan Initiated", url,
                    cxProperties.getBaseUrl().concat("/CxWebClient/UserQueue.aspx"));
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
                    createPatchAuthHeaders()
            );
            if(ScanUtils.empty(url)){
                log.error("statuses_url was not provided within the request object, which is required for blocking / unblocking pull requests");
                return;
            }
            //TODO remove preview once applicable
            log.info("Removing pending status from pull {}", url);
            restTemplate.exchange(url.concat(API_VERSION).concat(properties.getApiVersion().concat("-preview")),
                    HttpMethod.PATCH, httpEntity, Void.class);

            MergeResultEvaluatorImpl evaluator = new MergeResultEvaluatorImpl(flowProperties);
            boolean isMergeAllowed = evaluator.isMergeAllowed(results, properties, new PullRequestReport(scanDetails, request));
            
            if(!isMergeAllowed){
                log.debug("Creating status of failed to {}", url);
                createStatus("failed", "Checkmarx Scan Completed", url, results.getLink());
            }
            else{
                log.debug("Creating status of succeeded to {}", url);
                createStatus("succeeded", "Checkmarx Scan Completed", url, results.getLink());
            }
        }
    }

    int createStatus(String state, String description, String url, String sastUrl){
        HttpEntity<String> httpEntity = new HttpEntity<>(
                getJSONStatus(state, sastUrl, description).toString(),
                createAuthHeaders()
        );
        //TODO remove preview once applicable
        log.info("Adding pending status to pull {}", url);
        ResponseEntity<String> response = restTemplate.exchange(url.concat(API_VERSION).concat(properties.getApiVersion().concat("-preview")),
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
        comment.put("content", description);
        comment.put("commentType", 1);
        comments.put(comment);
        requestBody.put("comments", comments);
        requestBody.put("status", 1);

        return requestBody;
    }

    private JSONObject getJSONComment(String description){
        JSONObject requestBody = new JSONObject();
        requestBody.put("content", description);
        requestBody.put("parentCommentId", 1);
        requestBody.put("commentType", 1);

        return requestBody;
    }

}