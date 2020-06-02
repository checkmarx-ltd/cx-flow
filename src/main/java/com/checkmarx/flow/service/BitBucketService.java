package com.checkmarx.flow.service;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.flow.exception.BitBucketClientException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.beans.ConstructorProperties;
import java.util.*;


@Service
public class BitBucketService {

    private static final Logger log = LoggerFactory.getLogger(BitBucketService.class);
    private final RestTemplate restTemplate;
    private final BitBucketProperties properties;
    private final FlowProperties flowProperties;
    private final MergeResultEvaluator mergeResultEvaluator;

    private static final String BUILD_IN_PROGRESS = "INPROGRESS";
    private static final String BUILD_SUCCESSFUL = "SUCCESSFUL";
    private static final String BUILD_FAILED = "FAILED";

    @ConstructorProperties({"restTemplate", "properties", "flowProperties", "mergeResultEvaluator"})
    public BitBucketService(@Qualifier("flowRestTemplate") RestTemplate restTemplate, BitBucketProperties properties, FlowProperties flowProperties, MergeResultEvaluator mergeResultEvaluator) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.mergeResultEvaluator = mergeResultEvaluator;
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
            log.error("Error occurred while creating Merge Request comment", e);
            throw new BitBucketClientException();
        }
    }

    void processServerMerge(ScanRequest request,ScanResults results, ScanDetails scanDetails) throws BitBucketClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);

            if(properties.isBlockMerge()) {
                if (mergeResultEvaluator.isMergeAllowed(results, properties, new PullRequestReport(scanDetails, request))) {
                    sendServerMergeComment(request, comment);
                }
                else
                {
                    sendServerMergeTask(request, comment);
                }
            }
            else
            {
                sendServerMergeComment(request, comment);
            }

        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Merge Request comment", e);
            throw new BitBucketClientException();
        }
    }

    void setBuildStartStatus(ScanRequest request){
        if(properties.isBlockMerge()) {
            updateInProgressBuildStatus(request, BUILD_IN_PROGRESS);
        }
    }

    void setBuildEndStatus(ScanRequest request,ScanResults results, ScanDetails scanDetails){

        if(properties.isBlockMerge()) {
            String status = BUILD_SUCCESSFUL;
            if (!mergeResultEvaluator.isMergeAllowed(results, properties, new PullRequestReport(scanDetails, request))) {
                status = BUILD_FAILED;
            }

            String buildStatusUrl = request.getAdditionalMetadata("buildStatusUrl");

            JSONObject buildStatusBody = new JSONObject();
            buildStatusBody.put("state", status);
            buildStatusBody.put("key", "cxflow");
            buildStatusBody.put("name", "Checkmarx Scan Results");
            buildStatusBody.put("url", results.getLink());
            buildStatusBody.put("description", results.getScanSummary().toString());

            HttpEntity httpEntity = new HttpEntity<>(buildStatusBody.toString(), createAuthHeaders());
            restTemplate.exchange(buildStatusUrl, HttpMethod.POST, httpEntity, String.class);
        }
    }

    void updateInProgressBuildStatus(ScanRequest request, String status)
    {
            String buildStatusUrl = request.getAdditionalMetadata("buildStatusUrl");
            String cxBaseUrl = request.getAdditionalMetadata("cxBaseUrl");

            JSONObject buildStatusBody = new JSONObject();
            buildStatusBody.put("state", status);
            buildStatusBody.put("key", "cxflow");
            buildStatusBody.put("url", cxBaseUrl.concat("/CxWebClient/UserQueue.aspx"));

            //Following is optional
            buildStatusBody.put("name", "Checkmarx Scan Initiated");
            buildStatusBody.put("description", "Waiting for scan to complete..");

            HttpEntity httpEntity = new HttpEntity<>(buildStatusBody.toString(), createAuthHeaders());
            restTemplate.exchange(buildStatusUrl, HttpMethod.POST, httpEntity, String.class);
    }

    public void sendMergeComment(ScanRequest request, String comment){
        HttpEntity httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), createAuthHeaders());
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    public void sendServerMergeComment(ScanRequest request, String comment){
        HttpEntity httpEntity = new HttpEntity<>(getServerJSONComment(comment).toString(), createAuthHeaders());
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    void sendServerMergeTask(ScanRequest request, String comment){

        ResponseEntity<String> retrievedResult = retrieveExistingOpenTasks(request);
        Object cxFlowTask =  getCxFlowTask(retrievedResult);

        if(cxFlowTask !=null)
        {
            Integer taskId = ((JSONObject) cxFlowTask).getInt("id");
            Integer taskVersion = ((JSONObject) cxFlowTask).getInt("version");

            JSONObject taskBody = new JSONObject();
            taskBody.put("id", taskId);
            taskBody.put("version", taskVersion);
            taskBody.put("severity","BLOCKER");
            taskBody.put("text", comment);

            HttpEntity httpEntity = new HttpEntity<>(taskBody.toString(), createAuthHeaders());
            restTemplate.exchange(request.getMergeNoteUri().concat("/"+ taskId), HttpMethod.PUT, httpEntity, String.class);

        }
        else {
            JSONObject taskBody = new JSONObject();
            taskBody.put("severity", "BLOCKER");
            taskBody.put("text", comment);
            HttpEntity httpEntity = new HttpEntity<>(taskBody.toString(), createAuthHeaders());
            restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
        }
    }

    private Object getCxFlowTask(ResponseEntity<String> retrievedResult) {

        if(retrievedResult.getBody() != null) {
            JSONObject json = new JSONObject(retrievedResult.getBody());
            JSONArray taskList = json.getJSONArray("values");

            if(!taskList.isEmpty())
            {
              for ( Object task : taskList)
              {
                  Object author = ((JSONObject)task).get("author");
                  String taskAuthor = (String) ((JSONObject)author).get("slug");

                  if(taskAuthor.equals(getCxFlowServiceAccountSlug()))
                  {
                      return task;
                  }
              }
            }
            else
            {
                return null;
            }
        }
        return null;
    }

    private String getCxFlowServiceAccountSlug()
    {
        String[] basicAuthCredentials = properties.getToken().split(":");
        String cxFlowSlug = basicAuthCredentials[0];
        return cxFlowSlug;
    }

    private ResponseEntity<String> retrieveExistingOpenTasks(ScanRequest request) {

        String blockerCommentUrl = request.getAdditionalMetadata("blocker-comment-url");

        Map<String, String> params = new HashMap<String,String>();
        params.put("state", "OPEN");

        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<String> response = restTemplate.exchange(blockerCommentUrl.concat("?state={state}"), HttpMethod.GET,httpEntity,String.class,params);

        return response;
    }

    void processCommit(ScanRequest request, ScanResults results) throws BitBucketClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);
            sendCommitComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Commit comment", e);
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
