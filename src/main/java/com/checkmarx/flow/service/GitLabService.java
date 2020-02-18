package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.dto.gitlab.Note;
import com.checkmarx.flow.exception.GitLabClientException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.ScanResults;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


@Service
public class GitLabService extends RepoService {

    private static final String PROJECT = "/projects/{namespace}{x}{repo}";
    public static final String MERGE_NOTE_PATH = "/projects/{id}/merge_requests/{iid}/notes";
    public static final String MERGE_PATH = "/projects/{id}/merge_requests/{iid}";
    public static final String COMMIT_PATH = "/projects/{id}/repository/commits/{sha}/comments";
    private static final String FILE_CONTENT = "/projects/{id}/repository/files/{config}?ref={branch}";
    private static final String LANGUAGE_TYPES = "/projects/{id}/languages";
    private static final String REPO_CONTENT = "/projects/{id}/repository/tree?ref={branch}";
    private static final int UNKNOWN_INT = -1;
    private static final Logger log = LoggerFactory.getLogger(GitLabService.class);
    private final RestTemplate restTemplate;
    private final GitLabProperties properties;
    private final FlowProperties flowProperties;

    @ConstructorProperties({"restTemplate", "properties", "flowProperties"})
    public GitLabService(@Qualifier("flowRestTemplate") RestTemplate restTemplate, GitLabProperties properties, FlowProperties flowProperties) {
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
            log.error("Error calling gitlab project api {}", e.getResponseBodyAsString(), e);
        }catch (JSONException e){
            log.error("Error parsing gitlab project response.", e);
        }
        catch (URISyntaxException e){
            log.error("Incorrect URI", e);
        }

        return UNKNOWN_INT;
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
            log.error("Error occurred while creating Merge Request comment", e);
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

    void processCommit(ScanRequest request, ScanResults results) throws GitLabClientException {
        try {
            String comment = ScanUtils.getMergeCommentMD(request, results, flowProperties, properties);
            log.debug("comment: {}", comment);
            sendCommitComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Commit comment", e);
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

    @Override
    public Sources getRepoContent(ScanRequest request) {
        log.debug("Auto profiling is enabled");
        if(ScanUtils.empty(request.getBranch()) || request.getRepoProjectId() == null){
            return null;
        }
        Sources sources = getRepoLanguagePercentages(request);
        scanGitContent(sources, request);
        return sources;
    }

    private Sources getRepoLanguagePercentages(ScanRequest request) {
        Sources sources = new Sources();
        Map<String, Integer> langs = new HashMap<>();
        HttpHeaders headers = createAuthHeaders();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getApiUrl().concat(LANGUAGE_TYPES),
                    HttpMethod.GET,
                    new HttpEntity(headers),
                    String.class,
                    request.getRepoProjectId()
            );
            if(response.getBody() == null){
                log.warn("HTTP Body is null for content api ");
            }
            else {
                JSONObject json = new JSONObject(response.getBody());
                Iterator<String> keys = json.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    double bytes = json.getDouble(key);
                    langs.put(key, (int)Math.ceil(bytes));
                }
                sources.setLanguageStats(langs);
            }
        }catch (NullPointerException e){
            log.warn("Content not found in JSON response", e);
        }catch (HttpClientErrorException.NotFound e){
            log.error("Error occurred", e);
        }catch (HttpClientErrorException e){
            log.error("Error occurred", e);
        }
        return sources;
    }

    private void scanGitContent(Sources sources, ScanRequest request){
        HttpHeaders headers = createAuthHeaders();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getApiUrl().concat(REPO_CONTENT),
                    HttpMethod.GET,
                    new HttpEntity(headers),
                    String.class,
                    request.getRepoProjectId(),
                    request.getBranch()
            );
            if(response.getBody() == null){
                log.warn("HTTP Body is null for content api ");
            }
            JSONArray files = new JSONArray(response.getBody());
            for(int i = 0; i < files.length(); i++){
                JSONObject file = files.getJSONObject(i);
                String f = file.getString("name");
                String path = file.getString("path");
                sources.addSource(path, f);
            }
        }catch (NullPointerException e){
            log.warn("Content not found in JSON response", e);
        }catch (HttpClientErrorException.NotFound e){
            log.error("Error occurred", e);
        }catch (HttpClientErrorException e){
            log.error("Error occurred", e);
        }
    }

    @Override
    public CxConfig getCxConfigOverride(ScanRequest request) {
        HttpHeaders headers = createAuthHeaders();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getApiUrl().concat(FILE_CONTENT),
                    HttpMethod.GET,
                    new HttpEntity(headers),
                    String.class,
                    request.getRepoProjectId(),
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
            log.warn("Content not found in JSON response", e);
        }catch (HttpClientErrorException.NotFound e){
            log.info("No Config As code was found [{}]", properties.getConfigAsCode(), e);
        }catch (HttpClientErrorException e){
            log.error("Error occurred", e);
        }catch (Exception e){
            log.error("Error occurred", e);
        }
        return null;
    }

}
