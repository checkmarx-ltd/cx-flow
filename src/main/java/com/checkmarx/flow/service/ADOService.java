package com.checkmarx.flow.service;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.RepoIssue;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.azure.CreateWorkItemAttr;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.flow.exception.ADOClientException;
import com.checkmarx.flow.exception.GitHubClientRunTimeException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.ScanResults;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ADOService {
    private static final Logger log = LoggerFactory.getLogger(ADOService.class);
    private static final String API_VERSION = "?api-version=";
    private static final String ADO_COMMENT_CONTENT_FIELD_NAME = "content";
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

    public void sendMergeComment(ScanRequest request, String comment){
        String mergeUrl = request.getMergeNoteUri();
        if(ScanUtils.empty(mergeUrl)){
            log.error("mergeUrl was not provided within the request object, which is required for commenting on pull request");
            return;
        }
        log.debug(mergeUrl);
        try {
            RepoComment commentToUpdate = PullRequestCommentsHelper.getCommentToUpdate(getComments(mergeUrl), comment);
            if (commentToUpdate != null && PullRequestCommentsHelper.shouldUpdateComment(comment, commentToUpdate.getComment())) {
                updateComment(commentToUpdate, comment);
            } else if (commentToUpdate == null){
                String threadId = request.getAdditionalMetadata("ado_thread_id");
                if (ScanUtils.empty(threadId)) {
                    HttpEntity<String> httpEntity = new HttpEntity<>(getJSONThread(comment).toString(), createAuthHeaders());
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
                    HttpEntity<String> httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), createAuthHeaders());
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

    private void updateComment(RepoComment repoComment, String newComment) {
        log.debug("Updating exisiting comment. url: {}", repoComment.getCommentUrl());
        log.debug("Updated comment: {}" , repoComment);
        HttpEntity<?> httpEntity = new HttpEntity<>(RepoIssue.getJSONComment(ADO_COMMENT_CONTENT_FIELD_NAME,newComment).toString(), createAuthHeaders());
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
            restTemplate.exchange(getFullAdoApiUrl(url).concat("-preview"),
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

    private List<RepoComment> getComments(String url) throws IOException {
        HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity , String.class);
        List<RepoComment> result = new ArrayList<>();
        ObjectMapper objMapper = new ObjectMapper();
        JsonNode root = objMapper.readTree(response.getBody());
        JsonNode value = root.path("value");
        Iterator<JsonNode> threadsIter = value.getElements();
        while (threadsIter.hasNext()) {
            JsonNode thread = threadsIter.next();
            JsonNode comments = thread.get("comments");
            Iterator<JsonNode> commentsIter = comments.getElements();
            while (commentsIter.hasNext()) {
                JsonNode commentNode = commentsIter.next();
                if (commentNode.has(ADO_COMMENT_CONTENT_FIELD_NAME)) {
                    String commentStr = commentNode.get(ADO_COMMENT_CONTENT_FIELD_NAME).asText();
                }
                result.add(createRepoComment(commentNode));
            }

        }
        return result;

    }

    private RepoComment createRepoComment(JsonNode commentNode)  {
        String commentBody = commentNode.path(ADO_COMMENT_CONTENT_FIELD_NAME).getTextValue();
        long id = commentNode.path("id").asLong();
        String commentUrl = commentNode.path(("_links")).path("self").path("href").asText();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String updatedStr = commentNode.path("lastContentUpdatedDate").asText();
        String createdStr = commentNode.path("publishedDate").asText();
        try {
            return new RepoComment(id, commentBody, commentUrl, sdf.parse(createdStr), sdf.parse(updatedStr));
        }
        catch (ParseException pe) {
            throw new GitHubClientRunTimeException("Error parsing github pull request created or updted date", pe);
        }
    }

}