package com.checkmarx.flow.service;

import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.ScanResults;
import com.checkmarx.flow.dto.gitlab.Note;
import com.checkmarx.flow.exception.GitLabClientException;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.beans.ConstructorProperties;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


@Service
public class GitLabService {

    private static final String PROJECT = "/projects/{namespace}{x}{repo}";
    public static final String MERGE_NOTE_PATH = "/projects/{id}/merge_requests/{iid}/notes";
    public static final String MERGE_PATH = "/projects/{id}/merge_requests/{iid}";
    public static final String COMMIT_PATH = "/projects/{id}/repository/commits/{sha}/comments";
    private static final int UNKNOWN_INT = -1;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitLabService.class);
    private final RestTemplate restTemplate;
    private final GitLabProperties properties;
    private final FlowProperties flowProperties;

    @ConstructorProperties({"restTemplate", "properties", "flowProperties"})
    public GitLabService(RestTemplate restTemplate, GitLabProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }


    Integer getProjectDetails(String namespace, String repoName){

        try {
            String url = properties.getApiUrl().concat(PROJECT);

            url = url.replace("{namespace}", namespace);
            url = url.replace("{x}", "%2F");
            url = url.replace("{repo}", repoName);
            URI uri = new URI(url);

            HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
            JSONObject obj = new JSONObject(response.getBody());
            return obj.getInt("id");
        }catch (HttpClientErrorException e){
            log.error("Error calling gitlab project api {}", e.getResponseBodyAsString());
            log.error(ExceptionUtils.getStackTrace(e));
        }catch (JSONException e){
            log.error("Error parsing gitlab project response.", e);
            log.error(ExceptionUtils.getStackTrace(e));
        }
        catch (URISyntaxException e){
            log.error("Incorrect URI");
            log.error(ExceptionUtils.getStackTrace(e));
        }

        return UNKNOWN_INT;
    }


    Map<String, List<String>> process(ScanResults results, ScanRequest request) throws GitLabClientException {
        return null;
    }

    /**
     * Creates authentication header for GitLab API Access
     * TODO swap out for Portal based customer storage and possibly OAuth
     * https://docs.gitlab.com/ee/api/README.html#oauth2-tokens
     * https://docs.gitlab.com/ee/api/README.html#personal-access-tokens
     * https://gitlab.msu.edu/help/integration/oauth_provider.md
     * @return HttpHeaders for authentication
     */
    private HttpHeaders createAuthHeaders(){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set("PRIVATE-TOKEN", properties.getToken());
        httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return httpHeaders;
    }

    void processMerge(ScanRequest request,ScanResults results) throws GitLabClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);
            sendMergeComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Merge Request comment");
            throw new GitLabClientException();
        }
    }

    void sendMergeComment(ScanRequest request, String comment){
        Note note = Note.builder()
                .body(comment)
                .build();
        HttpEntity<Note> httpEntity = new HttpEntity<>(note, createAuthHeaders());
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    void processCommit(ScanRequest request,ScanResults results) throws GitLabClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);
            sendCommitComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Commit comment");
            throw new GitLabClientException();
        }
    }

    void sendCommitComment(ScanRequest request, String comment){
        JSONObject note = new JSONObject();
        note.put("note", comment);
        HttpEntity<String> httpEntity = new HttpEntity<>(note.toString(), createAuthHeaders());
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    void startBlockMerge(ScanRequest request){
        if(properties.isBlockMerge()) {
            String mergeId = request.getAdditionalMetadata("merge_id");
            if(ScanUtils.empty(request.getAdditionalMetadata("merge_id")) || ScanUtils.empty(request.getAdditionalMetadata("merge_title"))){
                log.error("merge_id and merge_title was not provided within the request object, which is required for blocking / unblocking merge requests");
                return;
            }
            String endpoint = properties.getApiUrl().concat(MERGE_PATH);
            endpoint = endpoint.replace("{id}", request.getRepoProjectId().toString());
            endpoint = endpoint.replace("{iid}", mergeId);

            HttpEntity httpEntity = new HttpEntity<>(
                    getJSONMergeTitle("WIP:CX|".concat(request.getAdditionalMetadata("merge_title"))).toString(),
                    createAuthHeaders()
            );
            restTemplate.exchange(endpoint,
                    HttpMethod.PUT, httpEntity, String.class);
        }
    }

    void endBlockMerge(ScanRequest request){
        if(properties.isBlockMerge()) {
            String mergeId = request.getAdditionalMetadata("merge_id");
            if(ScanUtils.empty(request.getAdditionalMetadata("merge_id")) || ScanUtils.empty(request.getAdditionalMetadata("merge_title"))){
                log.error("merge_id and merge_title was not provided within the request object, which is required for blocking / unblocking merge requests");
                return;
            }
            String endpoint = properties.getApiUrl().concat(MERGE_PATH);
            endpoint = endpoint.replace("{id}", request.getRepoProjectId().toString());
            endpoint = endpoint.replace("{iid}", mergeId);

            HttpEntity httpEntity = new HttpEntity<>(
                    getJSONMergeTitle(request.getAdditionalMetadata("merge_title")
                            .replace("WIP:CX|","")).toString(),
                    createAuthHeaders()
            );
            restTemplate.exchange(endpoint,
                    HttpMethod.PUT, httpEntity, String.class);
        }
    }

    private JSONObject getJSONMergeTitle(String title){
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        return requestBody;
    }
}
