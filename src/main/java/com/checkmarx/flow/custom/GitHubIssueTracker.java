package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.github.IssueStatus;
import com.checkmarx.flow.dto.github.LabelsItem;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.google.common.collect.Sets;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("GitHub")
public class GitHubIssueTracker implements IssueTracker {

    private static final String TRANSITION_CLOSE = "closed";
    private static final String TRANSITION_OPEN = "open";
    private static final String ISSUES_PER_PAGE = "100";
    private static final Logger log = LoggerFactory.getLogger(GitHubIssueTracker.class);

    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private final FlowProperties flowProperties;


    public GitHubIssueTracker(@Qualifier("flowRestTemplate") RestTemplate restTemplate, GitHubProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("======== Initializing GitHub processing ========");
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
        String apiUrl = String.format("%s/%s/%s/issues?state=all&per_page=%s",
                properties.getApiUrl(),
                request.getNamespace(),
                request.getRepoName(),
                ISSUES_PER_PAGE);

        log.info("Executing getIssues GitHub API call: {}", apiUrl);
        List<Issue> issues = new ArrayList<>();
        HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<com.checkmarx.flow.dto.github.Issue[]> response = restTemplate.exchange(apiUrl,
                HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.github.Issue[].class);

        if (response.getBody() == null) {
            log.info("No issues found.");
            return new ArrayList<>();
        }

        for (com.checkmarx.flow.dto.github.Issue issue : response.getBody()) {
            Issue i = mapToIssue(issue);
            if (i != null && i.getTitle().startsWith(request.getProduct().getProduct())) {
                issues.add(i);
            }
        }

        String next = getNextURIFromHeaders(response.getHeaders(), "link", "next");
        while (next != null) {
            log.debug("Getting issue from {}", next);
            ResponseEntity<com.checkmarx.flow.dto.github.Issue[]> responsePage = restTemplate.exchange(next, HttpMethod.GET,
                    httpEntity, com.checkmarx.flow.dto.github.Issue[].class);

            if (responsePage.getBody() != null) {
                for (com.checkmarx.flow.dto.github.Issue issue : response.getBody()) {
                    Issue i = mapToIssue(issue);
                    if (i != null && i.getTitle().startsWith(request.getProduct().getProduct())) {
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
        String apiUrl = properties.getApiUrl().concat("/").concat(request.getNamespace().concat("/").concat(request.getRepoName())).concat("/issues");
        ResponseEntity<com.checkmarx.flow.dto.github.Issue> response;
        try {
            HttpEntity<String> httpEntity = new HttpEntity<>(getJSONCreateIssue(resultIssue, request).toString(), createAuthHeaders());
            response = restTemplate.exchange(apiUrl, HttpMethod.POST, httpEntity, com.checkmarx.flow.dto.github.Issue.class);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating GitHub Issue", e);
            if (e.getStatusCode().equals(HttpStatus.GONE)) {
                log.error("Issues are not enabled for this repository");
            }
            throw new MachinaRuntimeException(e);
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
            addCommentToAnUpdatedIssue(Objects.requireNonNull(response.getBody()).getUrl(), createNewIssueStatus(issue, resultIssue, response.getBody()));
            return mapToIssue(response.getBody());
        } catch (HttpClientErrorException e) {
            handleIssueUpdateError(e);
            this.addComment(issue.getUrl(), "This issue still exists.  Please add label 'false-positive' to remove from scope of SAST results");
        }
        return this.getIssue(issue.getUrl());
    }

    private IssueStatus createNewIssueStatus(Issue issueBeforeFixing, ScanResults.XIssue resultIssue, com.checkmarx.flow.dto.github.Issue issueAfterFixing) {
        Map<Integer, ScanResults.IssueDetails> sastFalsePositiveIssuesFromResult = getSASTFalsePositiveIssuesFromResult(resultIssue);
        Map<String, String> sastResolvedIssuesFromResults = getSASTResolvedIssuesFromResults(issueBeforeFixing.getBody(), resultIssue);

        IssueStatus issueStatus = IssueStatus.builder()
                .sastResolvedIssuesFromResults(sastResolvedIssuesFromResults)
                .openFalsePositiveLinesAsADescription(getNewFalsePositiveLines(sastFalsePositiveIssuesFromResult))
                .totalOpenLinesForIssueBeforeFixing((extractGitHubIssueVulnerabilityCodeLines(issueAfterFixing.getBody()).size()))
                .totalResolvedFalsePositiveLines(sastFalsePositiveIssuesFromResult.size())
                .totalResolvedLinesFromResults(sastResolvedIssuesFromResults.size())
                .build();

        issueStatus.setTotalLinesToFixLeft(issueStatus.getTotalOpenLinesForIssueBeforeFixing());

        return issueStatus;
    }

    private void handleIssueUpdateError(HttpClientErrorException e) {
        log.error("Error updating issue. This is likely due to the fact that another user has closed this issue. Adding comment", e);
        boolean isForbidden = e.getStatusCode().equals(HttpStatus.FORBIDDEN);
        if (isForbidden) {
            log.error("Please check the scopes of your personal access token and make sure you have the necessary permissions for the repo.");
        }
        if (e.getStatusCode().equals(HttpStatus.GONE) || isForbidden) {
            throw new MachinaRuntimeException(e);
        }
    }

    private void addCommentToAnUpdatedIssue(String issueUrl, IssueStatus issueStatus) {
        StringBuilder commentFormat = setIssueUpdatedDescription(issueStatus);
        this.addComment(issueUrl, commentFormat.toString());
    }

    private StringBuilder setIssueUpdatedDescription(IssueStatus issueStatus) {
        StringBuilder commentFormat = new StringBuilder();
        commentFormat.append("Issue still exists.\n");

        if (!issueStatus.getSastResolvedIssuesFromResults().isEmpty()) {
            commentFormat.append("The following code lines snippets were resolved from the issue:\n\n");
            issueStatus.getSastResolvedIssuesFromResults().forEach((key, value) -> commentFormat.append("`Code line: ").append(key).append("`")
                    .append("\n`Snippet: ").append(value).append("`").append("\n"));
        }

        if (!issueStatus.getOpenFalsePositiveLinesAsADescription().isEmpty()) {
            commentFormat.append("\n");
            commentFormat.append(issueStatus.getOpenFalsePositiveLinesAsADescription()).append("\n\n");
        }

        commentFormat.append("\n### **SUMMARY**\n\n");
        if ((issueStatus.getTotalResolvedLinesFromResults() + issueStatus.getTotalResolvedFalsePositiveLines()) == 0) { // No Resolved vulnerabilities
            commentFormat.append("Issue have total **").append(issueStatus.getTotalOpenLinesForIssueBeforeFixing()).append("** vulnerabilities left to be fix (Please scroll to the top for more information)");
        } else {
            commentFormat.append("- Total of vulnerabilities resolved on the last scan: **").append(issueStatus.getTotalResolvedLinesFromResults()).append("**");
            commentFormat.append("\n- Total of vulnerabilities set as 'false positive' on the last scan: **").append(issueStatus.getTotalResolvedFalsePositiveLines()).append("**");
            commentFormat.append("\n- Total of vulnerabilities left to fix for this issue: **").append(issueStatus.getTotalLinesToFixLeft()).append("**").append(" (Please scroll to the top for more information)");
        }
        return commentFormat;
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
            log.error("Error creating JSON Update Object - JSON object will be empty", e);
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
            return String.format(ScanUtils.ISSUE_KEY, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename(), request.getBranch());
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
        return issue.getState().equals(TRANSITION_OPEN);
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("======== Finalizing GitHub Processing ========");
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
            log.error("Error creating JSON Comment Object - JSON object will be empty", e);
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
            log.error("Error creating JSON Close Issue Object - JSON object will be empty", e);
        }
        return requestBody;
    }

    /**
     * @return Header consisting of API token used for authentication
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "token ".concat(properties.getToken()));
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

    private Map<Integer, ScanResults.IssueDetails> getSASTFalsePositiveIssuesFromResult(ScanResults.XIssue resultIssue) {
        Map<Integer, ScanResults.IssueDetails> sastIssuesDetails = resultIssue.getDetails();
        if (sastIssuesDetails != null) {
            return sastIssuesDetails.entrySet()
                    .stream()
                    .filter(detailsEntry -> detailsEntry.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return new HashMap<>();
        }
    }

    private String getNewFalsePositiveLines(Map<Integer, ScanResults.IssueDetails> newFalsePositiveIssuesMap) {
        StringBuilder sb = new StringBuilder();

        if (!newFalsePositiveIssuesMap.isEmpty()) {
            sb.append("Following code lines were resolved by being defined as false-positive:\n\n");

            newFalsePositiveIssuesMap.forEach((key, value) -> {
                sb.append("`Code line: ").append(key).append("`").append("\n");
                sb.append("`Snippet: ").append(value.getCodeSnippet().trim()).append("`").append("\n");
            });
        }
        return sb.toString();
    }

    private Map<String, String> getSASTResolvedIssuesFromResults(String issueBody, ScanResults.XIssue resultIssue) {
        Sets.SetView<String> differentCodeLinesSet;

        Set<String> currentIssueCodeLines = extractGitHubIssueVulnerabilityCodeLines(issueBody);
        Set<String> currentSASTResultCodeLines = extractSASTResultCodeLines(resultIssue);
        if (currentIssueCodeLines.size() != currentSASTResultCodeLines.size()) {
            differentCodeLinesSet = Sets.difference(currentIssueCodeLines, currentSASTResultCodeLines);// leaves only the different code lines which resolved
            return extractGitHubIssueVulnerabilityCodeSnippet(differentCodeLinesSet, issueBody);
        } else {
            return new HashMap<>();
        }

    }

    private Set<String> extractSASTResultCodeLines(ScanResults.XIssue resultIssue) {
        return resultIssue.getDetails().entrySet()
                .stream()
                .map(e -> e.getKey().toString())
                .collect(Collectors.toSet());
    }

    private Map<String, String> extractGitHubIssueVulnerabilityCodeSnippet(Set<String> resolvedIssueCodeLines, String issueBody) {
        String codeSnippetForCodeLinePattern = "\\(Line #%s\\).*\\W\\`{3}\\W+(.*)(?=\\W+\\`{3})";
        Map<String, String> sastResolvedIssuesMap = new HashMap<>();

        for (String currentResolvedIssue : resolvedIssueCodeLines) {
            String currentCodeLinePattern = String.format(codeSnippetForCodeLinePattern, currentResolvedIssue);

            Pattern pattern = Pattern.compile(currentCodeLinePattern, Pattern.UNIX_LINES);
            Matcher matcher = pattern.matcher(issueBody);

            while (matcher.find()) {
                sastResolvedIssuesMap.put(currentResolvedIssue, matcher.group(1));
            }
        }
        return sastResolvedIssuesMap;
    }

    private Set<String> extractGitHubIssueVulnerabilityCodeLines(String issueBody) {
        final String allNumbersAfterLinePattern = "Line #[0-9]+";
        final String allNumbersPattern = "[0-9]+";

        final Pattern allNumAfterLinePattern = Pattern.compile(allNumbersAfterLinePattern, Pattern.MULTILINE);
        final Matcher allNumAfterLineMatcher = allNumAfterLinePattern.matcher(issueBody);

        Set<String> codeLinesSet = new HashSet<>();

        while(allNumAfterLineMatcher.find()) {
            Pattern allNumPattern = Pattern.compile(allNumbersPattern, Pattern.MULTILINE);
            Matcher allNumMatcher = allNumPattern.matcher(allNumAfterLineMatcher.group());

            if (allNumMatcher.find()) {
                codeLinesSet.add(allNumMatcher.group());
            }
        }
        return codeLinesSet;
    }
}