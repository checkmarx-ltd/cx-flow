package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.gitlab.Note;
import com.checkmarx.flow.exception.MachinaException;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service("GitLab")
public class GitLabIssueTracker implements IssueTracker {

    private static final String TRANSITION_CLOSE = "close";
    private static final String TRANSITION_OPEN = "reopen";
    private static final String OPEN_STATE = "opened";
    private static final String ISSUES_PER_PAGE = "100";
    private static final String PROJECT = "/projects?search={repo}";
    private static final String ISSUES_PATH = "/projects/{id}/issues?per_page=".concat(ISSUES_PER_PAGE);
    private static final String NEW_ISSUE_PATH = "/projects/{id}/issues";
    private static final String ISSUE_PATH = "/projects/{id}/issues/{iid}";
    private static final String COMMENT_PATH = "/projects/{id}/issues/{iid}/notes";
    private static final Logger log = LoggerFactory.getLogger(GitLabIssueTracker.class);
    private static final int UNKNOWN_INT = -1;
    private final RestTemplate restTemplate;
    private final GitLabProperties properties;
    private final FlowProperties flowProperties;

    public GitLabIssueTracker(@Qualifier("flowRestTemplate") RestTemplate restTemplate, GitLabProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Initializing GitLab processing");
        if(ScanUtils.empty(request.getNamespace()) ||
                ScanUtils.empty(request.getRepoName()) ||
                ScanUtils.empty(request.getBranch())){
            throw new MachinaException("Namespace / RepoName / Branch are required");
        }
        if(ScanUtils.empty(properties.getApiUrl())){
            throw new MachinaException("GitLab API Url must be provided in property config");
        }
        if(request.getRepoProjectId() == null) {
            Integer projectId = getProjectId(request.getNamespace(), request.getRepoName());
            if (projectId.equals(UNKNOWN_INT)) {
                log.error("Could not obtain GitLab Project Id for {}/{}/{}", request.getNamespace(), request.getRepoName(), request.getBranch());
                throw new MachinaException("Could not obtain GitLab Project Id");
            }
            request.setRepoProjectId(projectId);
        }
    }

    private Integer getProjectId(String targetNamespace, String targetRepoName) {
        try {
            int projectId = 0;
            JSONArray candidateProjects = getProjectSearchResults(targetRepoName);
            log.debug("Projects found: {}. Looking for exact match.", candidateProjects.length());
            // The search is fuzzy, so we need to additionally filter search results here for strict match.
            for (Object project : candidateProjects) {
                JSONObject projectJson = (JSONObject) project;
                if (isTargetProject(projectJson, targetNamespace, targetRepoName)) {
                    projectId = projectJson.getInt("id");
                    log.debug("Using GitLab project ID: {}", projectId);
                    break;
                }
            }
            return projectId;
        } catch(HttpClientErrorException e) {
            log.error("Error calling gitlab project api {}", e.getResponseBodyAsString(), e);
        } catch(JSONException e) {
            log.error("Error parsing gitlab project response.", e);
        } catch(URISyntaxException e) {
            log.error("Incorrect URI", e);
        }
        return UNKNOWN_INT;
    }

    private static boolean isTargetProject(JSONObject projectJson, String targetNamespace, String targetRepo) {
        // Using paths, because they are more well-defined (this is what appears in browser's address bar).
        String repoPath = projectJson.getString("path");

        // Namespace name may look like: "My Good Old Namespace", whereas its path cannot contain spaces
        // and may look like: "my-good-old-namespace".
        String namespacePath = projectJson.getJSONObject("namespace")
                .getString("path");

        boolean result = repoPath.equals(targetRepo) && namespacePath.equals(targetNamespace);
        log.debug("Checking {}/{}... {}", namespacePath, repoPath, result ? "match!" : "no match.");
        return result;
    }

    private JSONArray getProjectSearchResults(String targetRepoName) throws URISyntaxException {
        log.debug("Searching repo by query: {}", targetRepoName);
        String url = properties.getApiUrl()
                .concat(PROJECT)
                .replace("{repo}", targetRepoName);
        URI uri = new URI(url);
        HttpEntity<Void> httpEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
        return new JSONArray(response.getBody());
    }

    /**
     * Get list of issues associated with the project in GitLab
     */
    @Override
    public List<Issue> getIssues(ScanRequest request) {
        log.info("Executing getIssues GitLab API call");
        List<Issue> issues = new ArrayList<>();
        HttpEntity<Void> httpEntity = new HttpEntity<>(createAuthHeaders());
        String endpoint = properties.getApiUrl().concat(ISSUES_PATH);
        ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue[]> response = restTemplate.exchange(endpoint,
                HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.gitlab.Issue[].class, request.getRepoProjectId());
        if(response.getBody() == null) return new ArrayList<>();
        for (com.checkmarx.flow.dto.gitlab.Issue issue : response.getBody()) {
            Issue i = mapToIssue(issue);
            if (i != null) {
                issues.add(i);
            }
        }
        String next = getNextURIFromHeaders(response.getHeaders(), "link", "next");
        while (next != null) {
            ResponseEntity<Issue[]> responsePage = restTemplate.exchange(next, HttpMethod.GET, httpEntity, Issue[].class);
            if(responsePage.getBody() != null) {
                for(com.checkmarx.flow.dto.gitlab.Issue issue: response.getBody()){
                    Issue i = mapToIssue(issue);
                    if(i != null && i.getTitle().startsWith(request.getProduct().getProduct())){
                        issues.add(i);
                    }
                }
            }
            next = getNextURIFromHeaders(responsePage.getHeaders(), "link", "next");
        }
        return issues;
    }

    private Issue mapToIssue(com.checkmarx.flow.dto.gitlab.Issue issue){
        if(issue == null){
            return null;
        }
        Issue i = new Issue();
        i.setBody(issue.getDescription());
        i.setTitle(issue.getTitle());
        i.setId(issue.getIid().toString());
        i.setLabels(issue.getLabels());
        i.setUrl(issue.getWebUrl());
        i.setState(issue.getState());
        return i;
    }

    /**
     * Retrieve DTO representation of GitLab Issue
     *
     * @return GitLab Issue
     */
    private Issue getIssue(Integer projectId, Integer iid) {
        log.debug("Executing getIssue GitLab API call");
        String endpoint = properties.getApiUrl().concat(ISSUE_PATH);
        HttpEntity<Void> httpEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue> response =
                restTemplate.exchange(endpoint, HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.gitlab.Issue.class, projectId, iid);

        return mapToIssue(response.getBody());
    }


    /**
     *  Adds a comment (Note) to an issue
     */
    private void addComment(Integer projectId, Integer iid, String comment) {
        log.debug("Executing add comment GitLab API call");
        String endpoint = properties.getApiUrl().concat(COMMENT_PATH);
        Note note = Note.builder()
                .body(comment)
                .build();
        HttpEntity<Note> httpEntity = new HttpEntity<>(note, createAuthHeaders());
        restTemplate.exchange(endpoint, HttpMethod.POST, httpEntity, String.class, projectId, iid);
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.debug("Executing createIssue GitLab API call");

        String endpoint = properties.getApiUrl().concat(NEW_ISSUE_PATH);
        ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue> response;

        try {
            HttpEntity<String> httpEntity = new HttpEntity<>(getJSONCreateIssue(resultIssue, request).toString(), createAuthHeaders());
            response = restTemplate.exchange(endpoint, HttpMethod.POST, httpEntity, com.checkmarx.flow.dto.gitlab.Issue.class, request.getRepoProjectId());
        }
        catch (HttpClientErrorException e){
            log.error("Error occurred while creating GitLab Issue", e);
            if(e.getStatusCode().equals(HttpStatus.GONE)){
                throw new MachinaException("Issues are not enabled for this repository");
            }
            else{
                throw new MachinaException("Error occurred while creating GitLab Issue");
            }
        }
        return mapToIssue(response.getBody());
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        closeIssue(request.getRepoProjectId(), Integer.parseInt(issue.getId()));
    }

    private com.checkmarx.flow.dto.gitlab.Issue closeIssue(Integer projectId, Integer iid) {
        log.debug("Executing closeIssue GitHub API call");
        String endpoint = properties.getApiUrl().concat(ISSUE_PATH);
        HttpEntity<String> httpEntity = new HttpEntity<>(getJSONCloseIssue().toString(), createAuthHeaders());
        ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue> response = restTemplate.exchange(endpoint, HttpMethod.PUT, httpEntity,
                com.checkmarx.flow.dto.gitlab.Issue.class, projectId, iid);
        return response.getBody();
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        return  updateIssue(getJSONUpdateIssue(resultIssue, request), request.getRepoProjectId(), Integer.parseInt(issue.getId()));
    }

    /**
     * Update existing issue in GitLab
     */
    private Issue updateIssue(JSONObject issue, Integer projectId, Integer iid) {
        log.debug("Executing updateIssue GitLab API call");
        String endpoint = properties.getApiUrl().concat(ISSUE_PATH);

        HttpEntity<String> httpEntity = new HttpEntity<>(issue.toString(), createAuthHeaders());
        ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue> response;
        try {
            response = restTemplate.exchange(endpoint, HttpMethod.PUT, httpEntity, com.checkmarx.flow.dto.gitlab.Issue.class, projectId, iid);
            this.addComment(projectId, iid,"Issue still exists. ");
            return mapToIssue(response.getBody());
        } catch (HttpClientErrorException e) {
            this.addComment(projectId, iid, "This issue still exists.  Please add label 'false-positive' to remove from scope of SAST results");
        }
        return this.getIssue(projectId, iid);
    }

    private String getFileUrl(ScanRequest request, String filename) {
        if(ScanUtils.empty(request.getRepoUrl())){
            return null;
        }
        String repoUrl = request.getRepoUrl().replace(".git", "/");
        if (Optional.ofNullable(filename).isPresent()) {
            return repoUrl.concat("/blob/").concat(request.getBranch()).concat("/").concat(filename);
        } else {
            return null;
        }
    }


    /**
     * @return JSON Object for close issue request
     */
    private JSONObject getJSONCloseIssue() {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("state_event", TRANSITION_CLOSE);
        } catch (JSONException e) {
            log.error("Error creating JSON Close Issue Object - JSON object will be empty", e);
        }
        return requestBody;
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitLab
     *
     */
    private JSONObject getJSONUpdateIssue(ScanResults.XIssue resultIssue, ScanRequest request) {
        JSONObject requestBody = new JSONObject();
        String fileUrl = getFileUrl(request, resultIssue.getFilename());
        String body = ScanUtils.getMDBody(resultIssue, request.getBranch(), fileUrl, flowProperties);
        String title = getXIssueKey(resultIssue, request);

        try {
            requestBody.put("title", title);
            requestBody.put("description", body);
            requestBody.put("state_event", TRANSITION_OPEN);
        } catch (JSONException e) {
            log.error("Error creating JSON Update Object - JSON object will be empty", e);
        }
        return requestBody;
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitLab
     *
     * @return JSON Object of create issue request
     */
    private JSONObject getJSONCreateIssue(ScanResults.XIssue resultIssue, ScanRequest request) {
        JSONObject requestBody = new JSONObject();
        String fileUrl = getFileUrl(request, resultIssue.getFilename());
        String body = ScanUtils.getMDBody(resultIssue, request.getBranch(), fileUrl, flowProperties);
        String title = getXIssueKey(resultIssue, request);

        try {
            requestBody.put("title", title);
            requestBody.put("description", body);
        } catch (JSONException e) {
            log.error("Error creating JSON Create Issue Object - JSON Object will be empty", e);
        }
        return requestBody;
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return properties.getFalsePositiveLabel();
    }

    @Override
    public String getIssueKey(Issue issue, ScanRequest request) {
        return issue.getTitle();
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        if(flowProperties.isTrackApplicationOnly() || ScanUtils.empty(request.getBranch())){
            return String.format(ScanUtils.ISSUE_KEY_2, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename());
        }
        else {
            return issue.getScaDetails() == null
                    ? String.format(ScanUtils.ISSUE_KEY, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename(), request.getBranch())
                    : ScanUtils.getScaSummaryCustomIssueKey(request, issue);
        }
    }

    @Override
    public boolean isIssueClosed(Issue issue, ScanRequest request) {
        if(issue.getState() == null){
            return false;
        }
        return issue.getState().equals(TRANSITION_CLOSE);
    }

    @Override
    public boolean isIssueOpened(Issue issue, ScanRequest request) {
        if(issue.getState() == null){
            return true;
        }
        return issue.getState().equals(OPEN_STATE);
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing GitLab Processing");
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

    private static String getNextURIFromHeaders(HttpHeaders headers, final String headerName, final String rel) {
        if (headerName == null) {
            return null;
        }
        if (headers == null || headers.get(headerName) == null) {
            return null;
        }
        String linkHeader = Objects.requireNonNull(headers.get(headerName)).get(0);
        String uriWithSpecifiedRel = null;
        final String[] links = linkHeader.split(", ");
        String linkRelation;
        for (final String link : links) {
            final int positionOfSeparator = link.indexOf(';');
            linkRelation = link.substring(positionOfSeparator + 1).trim();
            if (extractTypeOfRelation(linkRelation).equals(rel)) {
                uriWithSpecifiedRel = link.substring(1, positionOfSeparator - 1);
                break;
            }
        }

        return uriWithSpecifiedRel;
    }

    private static String extractTypeOfRelation(final String linkRelation) {
        int positionOfEquals = linkRelation.indexOf('=');
        return linkRelation.substring(positionOfEquals + 2, linkRelation.length() - 1).trim();
    }

}