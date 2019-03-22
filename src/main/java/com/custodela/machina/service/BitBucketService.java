package com.custodela.machina.service;

import com.custodela.machina.config.BitBucketProperties;
import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.dto.ScanRequest;
import com.custodela.machina.dto.ScanResults;
import com.custodela.machina.exception.BitBucketClienException;
import com.custodela.machina.utils.ScanUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.beans.ConstructorProperties;
import java.util.Base64;


@Service
public class BitBucketService {

    private static final String ISSUES_PER_PAGE = "100";
    private static final int UNKNOWN_INT = -1;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitBucketService.class);
    private final RestTemplate restTemplate;
    private final BitBucketProperties properties;
    private final MachinaProperties machinaProperties;

    @ConstructorProperties({"restTemplate", "properties", "machinaProperties"})
    public BitBucketService(RestTemplate restTemplate, BitBucketProperties properties, MachinaProperties machinaProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.machinaProperties = machinaProperties;
    }

    private HttpHeaders createAuthHeaders(){
        String encoding = Base64.getEncoder().encodeToString(properties.getToken().getBytes());

        return new HttpHeaders() {{
            set("Content-Type", "application/json");
            set("Authorization", "Basic ".concat(encoding));
            set("Accept", "application/json");
        }};
    }

    void processMerge(ScanRequest request,ScanResults results) throws BitBucketClienException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, machinaProperties);
            log.debug("comment: {}", comment);
            sendMergeComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Merge Request comment");
            throw new BitBucketClienException();
        }
    }

    void processServerMerge(ScanRequest request,ScanResults results) throws BitBucketClienException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, machinaProperties);
            log.debug("comment: {}", comment);
            sendServerMergeComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Merge Request comment");
            throw new BitBucketClienException();
        }
    }

    void sendMergeComment(ScanRequest request, String comment){
        HttpEntity httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), createAuthHeaders());
        ResponseEntity<String> response = restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    void sendServerMergeComment(ScanRequest request, String comment){
        HttpEntity httpEntity = new HttpEntity<>(getServerJSONComment(comment).toString(), createAuthHeaders());
        ResponseEntity<String> response = restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    void processCommit(ScanRequest request,ScanResults results) throws BitBucketClienException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, machinaProperties);
            log.debug("comment: {}", comment);
            sendCommitComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Commit comment");
            throw new BitBucketClienException();
        }
    }

    private void sendCommitComment(ScanRequest request, String comment){
        JSONObject note = new JSONObject();
        note.put("note", comment);
        HttpEntity<String> httpEntity = new HttpEntity<>(note.toString(), createAuthHeaders());
        ResponseEntity<String> response = restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
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
