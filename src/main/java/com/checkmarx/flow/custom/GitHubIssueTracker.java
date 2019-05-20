package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.ScanResults;
import com.checkmarx.flow.dto.github.LabelsItem;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service("GitHub")
public class GitHubIssueTracker implements IssueTracker {

    private static final String TRANSITION_CLOSE = "closed";
    private static final String TRANSITION_OPEN = "open";
    private static final String ISSUES_PER_PAGE = "100";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitHubIssueTracker.class);

    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private final FlowProperties flowProperties;


    public GitHubIssueTracker(RestTemplate restTemplate, GitHubProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Initializing GitHub processing");
        if(ScanUtils.empty(request.getNamespace()) ||
                ScanUtils.empty(request.getRepoName()) ||
                ScanUtils.empty(request.getBranch())){
            throw new MachinaException("Namespace / RepoName / Branch are required");
        }
        if(ScanUtils.empty(properties.getApiUrl())){
            throw new MachinaException("GitHub API Url must be provided in property config");
        }
    }

    /**
     * Get all issues for a GitHub repository
     *
     * @return List of GitHub Issues
     * @ full name (owner/repo format)
     */
    @Override
    public List<Issue> getIssues(ScanRequest request) {
        log.info("Executing getIssues GitHub API call");
        List<Issue> issues = new ArrayList<>();
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        String apiUrl = properties.getApiUrl().concat(request.getNamespace().concat("/").concat(request.getRepoName()))
                .concat("/issues?state=all&per_page=").concat(ISSUES_PER_PAGE);
        ResponseEntity<com.checkmarx.flow.dto.github.Issue[]> response = restTemplate.exchange(apiUrl,
                HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.github.Issue[].class);
        if(response.getBody() == null){
            return new ArrayList<>();
        }
        for(com.checkmarx.flow.dto.github.Issue issue: response.getBody()){
            Issue i = mapToIssue(issue);
            if(i != null && i.getTitle().startsWith(request.getProduct().getProduct())){
                issues.add(i);
            }
        }
        String next = getNextURIFromHeaders(response.getHeaders(), "link", "next");
        while (next != null) {
            ResponseEntity<com.checkmarx.flow.dto.github.Issue[]> responsePage = restTemplate.exchange(next, HttpMethod.GET,
                    httpEntity, com.checkmarx.flow.dto.github.Issue[].class);
            if(responsePage.getBody() != null) {
                for(com.checkmarx.flow.dto.github.Issue issue: response.getBody()){
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


    private Issue mapToIssue(com.checkmarx.flow.dto.github.Issue issue){
        if(issue == null){
            return null;
        }
        Issue i = new Issue();
        i.setBody(issue.getBody());
        i.setTitle(issue.getTitle());
        i.setId(String.valueOf(issue.getId()));
        List<String> labels = new ArrayList<>();
        for(LabelsItem l: issue.getLabels()){
            labels.add(l.getName());
        }
        i.setLabels(labels);
        i.setUrl(issue.getUrl());
        i.setState(issue.getState());
        return i;
    }

    /**
     * Retrieve DTO representation of GitHub Issue
     *
     * @param issueUrl URL for specific GitHub Issue
     * @return GitHub Issue
     */
    private Issue getIssue(String issueUrl) {
        log.info("Executing getIssue GitHub API call");
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<com.checkmarx.flow.dto.github.Issue> response =
                restTemplate.exchange(issueUrl, HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.github.Issue.class);

        return mapToIssue(response.getBody());
    }

    /**
     * Add a comment to an existing GitHub Issue
     *
     * @param issueUrl URL for specific GitHub Issue
     * @param comment  Comment to append to the GitHub Issue
     */
    private void addComment(String issueUrl, String comment) {
        log.debug("Executing add comment GitHub API call");
        HttpEntity<String> httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), createAuthHeaders());
        restTemplate.exchange(issueUrl.concat("/comments"), HttpMethod.POST, httpEntity, String.class);
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.debug("Executing createIssue GitHub API call");
        String apiUrl = properties.getApiUrl().concat(request.getNamespace().concat("/").concat(request.getRepoName())).concat("/issues");
        ResponseEntity<com.checkmarx.flow.dto.github.Issue> response;
        try {
            HttpEntity<String> httpEntity = new HttpEntity<>(getJSONCreateIssue(resultIssue, request).toString(), createAuthHeaders());
            response = restTemplate.exchange(apiUrl, HttpMethod.POST, httpEntity, com.checkmarx.flow.dto.github.Issue.class);
        }
        catch (HttpClientErrorException e){
            log.error("Error occurred while creating GitHub Issue");
            log.error(ExceptionUtils.getStackTrace(e));
            if(e.getStatusCode().equals(HttpStatus.GONE)){
                log.error("Issues are not enabled for this repository");
                throw new MachinaRuntimeException();
            }
            else{
                throw new MachinaRuntimeException();
            }
        }
        return mapToIssue(response.getBody());
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        log.info("Executing closeIssue GitHub API call");
        HttpEntity httpEntity = new HttpEntity<>(getJSONCloseIssue().toString(), createAuthHeaders());
        restTemplate.exchange(issue.getUrl(), HttpMethod.POST, httpEntity, Issue.class);
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.info("Executing updateIssue GitHub API call");
        HttpEntity httpEntity = new HttpEntity<>(getJSONUpdateIssue(resultIssue, request).toString(), createAuthHeaders());
        ResponseEntity<com.checkmarx.flow.dto.github.Issue> response;
        try {
            response = restTemplate.exchange(issue.getUrl(), HttpMethod.POST, httpEntity, com.checkmarx.flow.dto.github.Issue.class);
            this.addComment(Objects.requireNonNull(response.getBody()).getUrl(),"Issue still exists. ");
            return mapToIssue(response.getBody());
        } catch (HttpClientErrorException e) {
            log.error("Error updating issue.  This is likely due to the fact that another user has closed this issue.  Adding comment");
            if(e.getStatusCode().equals(HttpStatus.GONE)){
                throw new MachinaRuntimeException();
            }
            this.addComment(issue.getUrl(), "This issue still exists.  Please add label 'false-positive' to remove from scope of SAST results");
        }
        return this.getIssue(issue.getUrl());
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitHub
     *
     */
    private JSONObject getJSONUpdateIssue(ScanResults.XIssue resultIssue, ScanRequest request) {
        JSONObject requestBody = new JSONObject();
        String fileUrl = ScanUtils.getFileUrl(request, resultIssue.getFilename());
        String body = ScanUtils.getMDBody(resultIssue, request.getBranch(), fileUrl, flowProperties);
        String title = getXIssueKey(resultIssue, request);

        try {
            requestBody.put("title", title);
            requestBody.put("body", body);
            requestBody.put("state", TRANSITION_OPEN);
        } catch (JSONException e) {
            log.error("Error creating JSON Update Object - JSON object will be empty");
        }
        return requestBody;
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitHub
     *
     * @return JSON Object of create issue request
     */
    private JSONObject getJSONCreateIssue(ScanResults.XIssue resultIssue, ScanRequest request) {
        JSONObject requestBody = new JSONObject();
        String fileUrl = ScanUtils.getFileUrl(request, resultIssue.getFilename());
        String body = ScanUtils.getMDBody(resultIssue, request.getBranch(), fileUrl, flowProperties);
        String title = getXIssueKey(resultIssue, request);

        try {
            requestBody.put("title", title);
            requestBody.put("body", body);
        } catch (JSONException e) {
            log.error("Error creating JSON Create Issue Object - JSON Object will be empty");
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
            return String.format(ScanUtils.ISSUE_KEY, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename(), request.getBranch());
        }
    }

    @Override
    public boolean isIssueClosed(Issue issue) {
        if(issue.getState() == null){
            return false;
        }
        return issue.getState().equals(TRANSITION_CLOSE);
    }

    @Override
    public boolean isIssueOpened(Issue issue) {
        if(issue.getState() == null){
            return true;
        }
        return issue.getState().equals(TRANSITION_OPEN);
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing GitHub Processing");
    }

    /**
     * Create JSON http request body for adding a comment to an Issue in GitHub
     *
     * @param comment Comment to append to an issue
     * @return JSON Object for comment request
     */
    private JSONObject getJSONComment(String comment) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("body", comment);
        } catch (JSONException e) {
            log.error("Error creating JSON Comment Object - JSON object will be empty");
        }
        return requestBody;
    }

    /**
     * @return JSON Object for close issue request
     */
    private JSONObject getJSONCloseIssue() {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("state", TRANSITION_CLOSE);
        } catch (JSONException e) {
            log.error("Error creating JSON Close Issue Object - JSON object will be empty");
        }
        return requestBody;
    }

    /**
     * @return Header consisting of API token used for authentication
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Authorization", "token ".concat(properties.getToken()));
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
            linkRelation = link.substring(positionOfSeparator + 1, link.length()).trim();
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