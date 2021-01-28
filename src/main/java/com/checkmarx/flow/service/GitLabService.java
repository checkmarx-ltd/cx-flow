package com.checkmarx.flow.service;

import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.RepoIssue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.dto.gitlab.Comment;
import com.checkmarx.flow.dto.gitlab.Note;
import com.checkmarx.flow.exception.GitLabClientException;
import com.checkmarx.flow.exception.GitLabClientRuntimeException;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.sast.CxConfig;
import com.checkmarx.sdk.dto.ScanResults;
import org.apache.commons.codec.binary.Base64;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class GitLabService extends RepoService {

    private static final String PROJECT = "/projects/{namespace}{x}{repo}";
    public static final String MERGE_NOTE_PATH = "/projects/%s/merge_requests/%s/notes/%s";
    public static final String MERGE_NOTES_PATH = "/projects/{id}/merge_requests/{iid}/notes";
    public static final String MERGE_PATH = "/projects/{id}/merge_requests/{iid}";
    public static final String COMMIT_PATH = "/projects/{id}/repository/commits/{sha}/comments";
    private static final String FILE_CONTENT = "/projects/{id}/repository/files/{config}?ref={branch}";
    private static final String LANGUAGE_TYPES = "/projects/{id}/languages";
    private static final String REPO_CONTENT = "/projects/{id}/repository/tree?ref={branch}";
    private static final int UNKNOWN_INT = -1;
    private static final Logger log = LoggerFactory.getLogger(GitLabService.class);
    private static final String MERGE_ID = "merge_id";
    private static final String MERGE_TITLE = "merge_title";
    private static final String HTTP_BODY_WARN_MESSAGE = "HTTP Body is null for content api ";
    private static final String CONTENT_NOT_FOUND_ERROR_MESSAGE = "Content not found in JSON response";
    private static final String ERROR_OCCURRED = "Error occurred";
    private final RestTemplate restTemplate;
    private final GitLabProperties properties;
    private final ScmConfigOverrider scmConfigOverrider;


    @ConstructorProperties({"restTemplate", "properties", "scmConfigOverrider"})
    public GitLabService(@Qualifier("flowRestTemplate") RestTemplate restTemplate, GitLabProperties properties, ScmConfigOverrider scmConfigOverrider) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.scmConfigOverrider = scmConfigOverrider;

    }


    Integer getProjectDetails(ScanRequest scanRequest, String namespace, String repoName){

        try {
            String url = scmConfigOverrider.determineConfigApiUrl(properties, scanRequest).concat(PROJECT);

            url = url.replace("{namespace}", namespace);
            url = url.replace("{x}", "%2F");
            url = url.replace("{repo}", repoName);
            URI uri = new URI(url);

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders(scanRequest));
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
    private HttpHeaders createAuthHeaders(ScanRequest scanRequest){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set("PRIVATE-TOKEN", scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance()));
        httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return httpHeaders;
    }

    void processMerge(ScanRequest request,ScanResults results) throws GitLabClientException {
        try {
            String comment = HTMLHelper.getMergeCommentMD(request, results,  properties);
            log.debug("comment: {}", comment);
            sendMergeComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Merge Request comment", e);
            throw new GitLabClientException();
        }
    }

    @Override
    public void updateComment(String commentUrl, String comment, ScanRequest scanRequest) {
        log.debug("Updating existing comment. url: {}", commentUrl);
        log.debug("Updated comment: {}" , comment);
        HttpEntity<?> httpEntity = new HttpEntity<>(RepoIssue.getJSONComment("body", comment).toString(), createAuthHeaders(scanRequest));
        restTemplate.exchange(commentUrl, HttpMethod.PUT, httpEntity, String.class);
    }

    @Override
    public void addComment(ScanRequest request, String comment) {
        Note note = Note.builder()
                .body(comment)
                .build();
        HttpEntity<Note> httpEntity = new HttpEntity<>(note, createAuthHeaders(request));
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    void processCommit(ScanRequest request, ScanResults results) throws GitLabClientException {
        try {
            String comment = HTMLHelper.getMergeCommentMD(request, results,  properties);
            log.debug("comment: {}", comment);
            sendCommitComment(request, comment);
        } catch (HttpClientErrorException e){
            log.error("Error occurred while creating Commit comment", e);
            throw new GitLabClientException();
        }
    }

    public void sendCommitComment(ScanRequest request, String comment){
        JSONObject note = new JSONObject();
        note.put("note", comment);
        HttpEntity<String> httpEntity = new HttpEntity<>(note.toString(), createAuthHeaders(request));
        restTemplate.exchange(request.getMergeNoteUri(), HttpMethod.POST, httpEntity, String.class);
    }

    public void startBlockMerge(ScanRequest request){
        if(properties.isBlockMerge()) {
            String mergeId = request.getAdditionalMetadata(MERGE_ID);
            if(ScanUtils.empty(request.getAdditionalMetadata(MERGE_ID)) || ScanUtils.empty(request.getAdditionalMetadata(MERGE_TITLE))){
                log.error("merge_id and merge_title was not provided within the request object, which is required for blocking / unblocking merge requests");
                return;
            }
            String endpoint = scmConfigOverrider.determineConfigApiUrl(properties, request).concat(MERGE_PATH);
            endpoint = endpoint.replace("{id}", request.getRepoProjectId().toString());
            endpoint = endpoint.replace("{iid}", mergeId);

            HttpEntity httpEntity = new HttpEntity<>(
                    getJSONMergeTitle("WIP:CX|".concat(request.getAdditionalMetadata(MERGE_TITLE))).toString(),
                    createAuthHeaders(request)
            );
            restTemplate.exchange(endpoint,
                                  HttpMethod.PUT, httpEntity, String.class);
        }
    }

    void endBlockMerge(ScanRequest request){
        if(properties.isBlockMerge()) {
            String mergeId = request.getAdditionalMetadata(MERGE_ID);
            if(ScanUtils.empty(request.getAdditionalMetadata(MERGE_ID)) || ScanUtils.empty(request.getAdditionalMetadata(MERGE_TITLE))){
                log.error("merge_id and merge_title was not provided within the request object, which is required for blocking / unblocking merge requests");
                return;
            }
            String endpoint = scmConfigOverrider.determineConfigApiUrl(properties, request).concat(MERGE_PATH);
            endpoint = endpoint.replace("{id}", request.getRepoProjectId().toString());
            endpoint = endpoint.replace("{iid}", mergeId);

            HttpEntity httpEntity = new HttpEntity<>(
                    getJSONMergeTitle(request.getAdditionalMetadata(MERGE_TITLE)
                                              .replace("WIP:CX|","")).toString(),
                    createAuthHeaders(request)
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
        HttpHeaders headers = createAuthHeaders(request);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    scmConfigOverrider.determineConfigApiUrl(properties, request).concat(LANGUAGE_TYPES),
                    HttpMethod.GET,
                    new HttpEntity(headers),
                    String.class,
                    request.getRepoProjectId()
            );
            if(response.getBody() == null){
                log.warn(HTTP_BODY_WARN_MESSAGE);
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
            log.warn(CONTENT_NOT_FOUND_ERROR_MESSAGE, e);
        }catch (HttpClientErrorException e){
            log.error(ERROR_OCCURRED, e);
        }
        return sources;
    }

    private void scanGitContent(Sources sources, ScanRequest request){
        HttpHeaders headers = createAuthHeaders(request);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    scmConfigOverrider.determineConfigApiUrl(properties, request).concat(REPO_CONTENT),
                    HttpMethod.GET,
                    new HttpEntity(headers),
                    String.class,
                    request.getRepoProjectId(),
                    request.getBranch()
            );
            if(response.getBody() == null){
                log.warn(HTTP_BODY_WARN_MESSAGE);
            }
            JSONArray files = new JSONArray(response.getBody());
            for(int i = 0; i < files.length(); i++){
                JSONObject file = files.getJSONObject(i);
                String f = file.getString("name");
                String path = file.getString("path");
                sources.addSource(path, f);
            }
        }catch (NullPointerException e){
            log.warn(CONTENT_NOT_FOUND_ERROR_MESSAGE, e);
        }catch (HttpClientErrorException e){
            log.error(ERROR_OCCURRED, e);
        }
    }

    @Override
    public CxConfig getCxConfigOverride(ScanRequest request) {
        HttpHeaders headers = createAuthHeaders(request);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    scmConfigOverrider.determineConfigApiUrl(properties, request).concat(FILE_CONTENT),
                    HttpMethod.GET,
                    new HttpEntity(headers),
                    String.class,
                    request.getRepoProjectId(),
                    properties.getConfigAsCode(),
                    request.getBranch()
            );
            if(response.getBody() == null) {
                log.warn(HTTP_BODY_WARN_MESSAGE);
            } else {
                JSONObject json = new JSONObject(response.getBody());
                String content = json.getString("content");
                if(ScanUtils.empty(content)) {
                    log.warn(CONTENT_NOT_FOUND_ERROR_MESSAGE);
                    return null;
                }
                String decodedContent = new String(Base64.decodeBase64(content.trim()));
                return com.checkmarx.sdk.utils.ScanUtils.getConfigAsCode(decodedContent);
            }
        } catch (NullPointerException e) {
            log.warn(CONTENT_NOT_FOUND_ERROR_MESSAGE, e);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("No Config As code was found [{}]", properties.getConfigAsCode());
        } catch (Exception e) {
            log.error(ERROR_OCCURRED, e);
        }
        return null;
    }

    @Override
    public void deleteComment(String url, ScanRequest scanRequest) {
        HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders(scanRequest));
        restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, String.class);
    }

    @Override
    public List<RepoComment> getComments(ScanRequest scanRequest) {
        HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders(scanRequest));
        ResponseEntity<Comment[]> response = restTemplate.exchange(scanRequest.getMergeNoteUri(),
                                                                   HttpMethod.GET, httpEntity ,
                                                                   Comment[].class);
        List<Comment> comments = Arrays.asList(Objects.requireNonNull(response.getBody()));
        return convertToListCxRepoComments(comments, scanRequest);
    }

    private List<RepoComment> convertToListCxRepoComments(List<Comment> comments, ScanRequest scanRequest) {
        List<RepoComment> repoComments = new ArrayList<>();
        for (Comment comment : comments) {
            RepoComment repoComment = convertToRepoComment(comment, scanRequest);
            if (PullRequestCommentsHelper.isCheckMarxComment(repoComment)) {
                repoComments.add(repoComment);
            }
        }
        return repoComments;
    }

    private RepoComment convertToRepoComment(Comment comment, ScanRequest scanRequest) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            return RepoComment.builder()
                    .id(comment.getId())
                    .comment(comment.getBody())
                    .createdAt(sdf.parse(comment.getCreatedAt()))
                    .updateTime(sdf.parse(comment.getUpdatedAt()))
                    .commentUrl(getCommentUrl(scanRequest, comment.getId()))
                    .build();
        } catch (ParseException pe) {
            throw new GitLabClientRuntimeException("Error parsing gitlab pull request created or " +
                                                           "updated date", pe);
        }
    }

    private String getCommentUrl(ScanRequest scanRequest, long commentId) {
        String path = scmConfigOverrider.determineConfigApiUrl(properties, scanRequest).concat(MERGE_NOTE_PATH);
        return String.format(path, scanRequest.getRepoProjectId().toString(),
                             scanRequest.getAdditionalMetadata(MERGE_ID), commentId);
    }

}