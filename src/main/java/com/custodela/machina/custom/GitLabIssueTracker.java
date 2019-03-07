package com.custodela.machina.custom;

import com.custodela.machina.config.GitLabProperties;
import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.dto.RepoIssue;
import com.custodela.machina.dto.ScanRequest;
import com.custodela.machina.dto.ScanResults;
import com.custodela.machina.dto.gitlab.Issue;
import com.custodela.machina.dto.gitlab.Note;
import com.custodela.machina.exception.MachinaException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


@Service
@Qualifier("GitLab")
public class GitLabIssueTracker implements IssueTracker {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitLabIssueTracker.class);

    private static final String ISSUES_PER_PAGE = "100";
    private static final String PROJECT = "projects/{namespace}{x}{repo}";
    private static final String PROJECT_PATH = "/projects/{id}";
    public static final String MERGE_PATH = "/projects/{id}/merge_requests/{iid}/notes";
    private static final String ISSUES_PATH = "/projects/{id}/issues?per_page=".concat(ISSUES_PER_PAGE);
    private static final String NEW_ISSUE_PATH = "/projects/{id}/issues";
    private static final String ISSUE_PATH = "/projects/{id}/issues/{iid}";
    private static final String COMMENT_PATH = "/projects/{id}/issues/{iid}/notes";
    private static final String PROJECT_FILES = PROJECT_PATH + "/repository/tree?ref=";
    private static final int UNKNOWN_INT = -1;
    private final RestTemplate restTemplate;
    private final GitLabProperties properties;
    private final MachinaProperties machinaProperties;


    /**
     * API call to determine additional project details, specifically the size of the code base
     *
     * @param projectId
     * @return
     */
    private String getProjectDetails(Integer projectId) {
        String endpoint = properties.getApiUrl().concat(PROJECT_PATH);
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        log.info("Calling GitLab for additional Repository/Project information of project Id", projectId);
        ResponseEntity<String> response = restTemplate.exchange(
                endpoint, HttpMethod.GET, httpEntity, String.class, projectId);

        return response.getBody();
    }

    Integer getProjectDetails(String namespace, String repoName) {

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
        } catch (HttpClientErrorException e) {
            log.error("Error calling gitlab project api {}", e.getResponseBodyAsString());
            e.printStackTrace();
        } catch (JSONException e) {
            log.error("Error parsing gitlab project response.", e);
            e.printStackTrace();
        } catch (URISyntaxException e) {
            log.error("Incorrect URI");
            e.printStackTrace();
        }

        return UNKNOWN_INT;
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return null;
    }

    /**
     * Get GitLab Issues
     *
     * @param request
     * @return
     */
    public List<Issue> getIssues(ScanRequest request) {
        log.info("Executing getIssues GitLab API call");
        List<Issue> issues = new ArrayList<>();
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        String endpoint = properties.getApiUrl().concat(ISSUES_PATH);
        ResponseEntity<Issue[]> response = restTemplate.exchange(endpoint,
                HttpMethod.GET, httpEntity, Issue[].class, request.getId());
        if (response.getBody() == null) return new ArrayList<>();
        Collections.addAll(issues, response.getBody());
        String next = RepoIssue.getNextURIFromHeaders(response.getHeaders());
        while (next != null) {
            ResponseEntity<Issue[]> responsePage = restTemplate.exchange(next, HttpMethod.GET, httpEntity, Issue[].class);
            if (responsePage.getBody() != null) {
                Collections.addAll(issues, responsePage.getBody());
            }
            next = RepoIssue.getNextURIFromHeaders(responsePage.getHeaders());
        }
        return issues;
    }

    @Override
    public com.custodela.machina.dto.Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        return null;
    }

    @Override
    public void closeIssue(com.custodela.machina.dto.Issue issue, ScanRequest request) throws MachinaException {

    }

    @Override
    public com.custodela.machina.dto.Issue updateIssue(com.custodela.machina.dto.Issue issue, ScanResults.XIssue resultIssue) throws MachinaException {
        return null;
    }

    @Override
    public String getIssueKeyFormat() {
        return null;
    }

    @Override
    public String getIssueKey(com.custodela.machina.dto.Issue issue, ScanRequest request) {
        return null;
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        return null;
    }

    @Override
    public String getOpenState() {
        return null;
    }

    @Override
    public String closedState() {
        return null;
    }

    /**
     * Retrieve GitLab Issue
     *
     * @return
     */
    private Issue getIssue(Integer projectId, Integer iid) {
        log.debug("Executing getIssue GitLab API call");
        String endpoint = properties.getApiUrl().concat(ISSUE_PATH);
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<Issue> response = restTemplate.exchange(endpoint, HttpMethod.GET, httpEntity, Issue.class, projectId, iid);

        return response.getBody();
    }


    /**
     * Adds a comment (Note) to an issue
     *
     * @param projectId
     * @param iid
     * @param comment
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


    private Issue createIssue(JSONObject issue, ScanRequest request) throws GitLabClienException {
        log.debug("Executing createIssue GitLab API call");
        String endpoint = properties.getApiUrl().concat(NEW_ISSUE_PATH);
        ResponseEntity<Issue> response;
        try {
            HttpEntity<String> httpEntity = new HttpEntity<>(issue.toString(), createAuthHeaders());
            response = restTemplate.exchange(endpoint, HttpMethod.POST, httpEntity, Issue.class, request.getId());
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating GitLab Issue");
            e.printStackTrace();
            if (e.getStatusCode().equals(HttpStatus.GONE)) {
                throw new GitLabClienException("Issues are not enabled for this repository");
            } else {
                throw new GitLabClienException("Error occurred while creating GitLab Issue");
            }
        }
        return response.getBody();
    }


    private Issue updateIssue(JSONObject issue, Integer projectId, Integer iid) throws GitLabClienException {
        log.debug("updateIssue call");
        String endpoint = properties.getApiUrl().concat(ISSUE_PATH);

        HttpEntity httpEntity = new HttpEntity<>(issue.toString(), createAuthHeaders());
        ResponseEntity<Issue> response;
        try {
            response = restTemplate.exchange(endpoint, HttpMethod.PUT, httpEntity, Issue.class, projectId, iid);
            this.addComment(projectId, iid, "Issue still exists");
            return response.getBody();
        } catch (HttpClientErrorException e) {
            this.addComment(projectId, iid, "Issue still exists");
        }
        return this.getIssue(projectId, iid);
    }

    /**
     * Close GitLab Issue
     *
     * @param projectId
     * @param iid
     * @return
     */
    private Issue closeIssue(Integer projectId, Integer iid) {
        log.info("closeIssue call");
        String endpoint = properties.getApiUrl().concat(ISSUE_PATH);
        HttpEntity httpEntity = new HttpEntity<>(getJSONCloseIssue().toString(), createAuthHeaders());
        ResponseEntity<Issue> response = restTemplate.exchange(endpoint, HttpMethod.PUT, httpEntity, Issue.class, projectId, iid);
        return response.getBody();
    }


    /**
     * @return JSON Object for close issue request
     */
    private JSONObject getJSONCloseIssue() {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("state_event", properties.getCloseTransition());
        } catch (JSONException e) {
            log.error("Error creating JSON Close Issue Object - JSON object will be empty");
        }
        return requestBody;
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitLab
     *
     * @param title
     * @param body
     * @return
     */
    private JSONObject getJSONUpdateIssue(String title, String body) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("title", title);
            requestBody.put("description", body);
            requestBody.put("state_event", properties.getOpenTransition());
        } catch (JSONException e) {
            log.error("Error creating JSON Update Object - JSON object will be empty");
        }
        return requestBody;
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitLab
     *
     * @param title Issue title
     * @param body  Issue content
     * @return JSON Object of create issue request
     */
    private JSONObject getJSONCreateIssue(String title, String body) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("title", title);
            requestBody.put("description", body);
        } catch (JSONException e) {
            log.error("Error creating JSON Create Issue Object - JSON Object will be empty");
        }
        return requestBody;
    }

    private HttpHeaders createAuthHeaders() {
        return new HttpHeaders() {{
            set("Content-Type", "application/json");
            set("PRIVATE-TOKEN", properties.getToken());
            set("Accept", "application/json");
        }};
    }


}