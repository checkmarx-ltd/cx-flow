package com.checkmarx.flow.service;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.BitBucketClientException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.beans.ConstructorProperties;
import java.util.Base64;


@Service
public class BitBucketService {

    private static final Logger log = LoggerFactory.getLogger(BitBucketService.class);
    private final RestTemplate restTemplate;
    private final BitBucketProperties properties;
    private final FlowProperties flowProperties;

    @ConstructorProperties({"restTemplate", "properties", "flowProperties"})
    public BitBucketService(@Qualifier("flowRestTemplate") RestTemplate restTemplate, BitBucketProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    private HttpHeaders createAuthHeaders(){
        String encoding = Base64.getEncoder().encodeToString(properties.getToken().getBytes());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "Basic ".concat(encoding));
        httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return httpHeaders;
    }

    void processMerge(ScanRequest request,ScanResults results) throws BitBucketClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);
            sendMergeComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Merge Request comment");
            throw new BitBucketClientException();
        }
    }

    void processServerMerge(ScanRequest request,ScanResults results) throws BitBucketClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);
            sendServerMergeComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Merge Request comment");
            throw new BitBucketClientException();
        }
    }

    void sendMergeComment(ScanRequest request, String comment){
        HttpEntity httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), createAuthHeaders());
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    void sendServerMergeComment(ScanRequest request, String comment){
        HttpEntity httpEntity = new HttpEntity<>(getServerJSONComment(comment).toString(), createAuthHeaders());
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    void processCommit(ScanRequest request, ScanResults results) throws BitBucketClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);
            sendCommitComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Commit comment");
            throw new BitBucketClientException();
        }
    }

    private void sendCommitComment(ScanRequest request, String comment){
        JSONObject note = new JSONObject();
        note.put("note", comment);
        HttpEntity<String> httpEntity = new HttpEntity<>(note.toString(), createAuthHeaders());
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    private static JSONObject getJSONComment(String comment) throws JSONException {
        JSONObject requestBody = new JSONObject();
        JSONObject content = new JSONObject();
        content.put("raw", comment);
        requestBody.put("content", content);
        return requestBody;
    }

    private static JSONObject getServerJSONComment(String comment) throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("text", comment);
        return requestBody;
    }

}
