package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.ScanResults;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;

@Service("Azure")
public class AzureIssueTracker implements IssueTracker {

    private static final String TRANSITION_ACTIVE = "closed";
    private static final String TRANSITION_CLOSED = "open";
    private static final String ISSUES_PER_PAGE = "100";
    private static final String WORKITEMS="%s/{o}/{p}/_apis/wit/wiql?api-version=%s";
    private static final String CREATEWORKITEMS="%s/{o}/{p}/_apis/wit/workitems/$%s?api-version=%s";
    private static final String WIQ_REPO_BRANCH = "Select [System.Id], [System.Title], " +
            "[System.State], [System.State], [System.WorkItemType] From WorkItems Where " +
            "[System.TeamProject] = @project AND [Tags] Contains '%s' AND [Tags] Contains '%s:%s'" +
            "AND [Tags] Contains '%s:%s' AND [Tags] Contains '%s:%s'";
    private static final String WIQ_APP = "Select [System.Id], [System.Title], " +
            "[System.State], [System.State], [System.WorkItemType] From WorkItems Where " +
            "[System.TeamProject] = @project AND [Tags] Contains '%s' AND [Tags] Contains '%s:%s'";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AzureIssueTracker.class);

    private final RestTemplate restTemplate;
    private final AzureProperties properties;
    private final FlowProperties flowProperties;


    public AzureIssueTracker(RestTemplate restTemplate, AzureProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Initializing Azure processing");
        if(ScanUtils.empty(request.getNamespace()) ||
                ScanUtils.empty(request.getRepoName()) ||
                ScanUtils.empty(request.getBranch())){
            throw new MachinaException("Namespace / RepoName / Branch are required");
        }
        if(ScanUtils.empty(properties.getApiPath())){
            throw new MachinaException("Azure API Url must be provided in property config");
        }
    }

    /**
     * Get all issues for a Azure repository
     *
     * @return List of Azure Issues
     * @ full name (owner/repo format)
     */
    @Override
    public List<Issue> getIssues(ScanRequest request) {
        log.info("Executing getIssues Azure API call");
        List<Issue> issues = new ArrayList<>();
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        return issues;
    }


    /*private Issue mapToIssue(com.checkmarx.flow.dto.Azure.Issue issue){
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
    }*/

    /**
     * Retrieve DTO representation of Azure Issue
     *
     * @param issueUrl URL for specific Azure Issue
     * @return Azure Issue
     */
    private Issue getIssue(String issueUrl) {
        return null;
    }

    /**
     * Add a comment to an existing Azure Issue
     *
     * @param issueUrl URL for specific Azure Issue
     * @param comment  Comment to append to the Azure Issue
     */
    private void addComment(String issueUrl, String comment) {
        log.debug("Executing add comment Azure API call");
        //HttpEntity<String> httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), createAuthHeaders());
        //restTemplate.exchange(issueUrl.concat("/comments"), HttpMethod.POST, httpEntity, String.class);
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.debug("Executing createIssue Azure API call");
        return null;
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        log.info("Executing closeIssue Azure API call");
        //HttpEntity httpEntity = new HttpEntity<>(getJSONCloseIssue().toString(), createAuthHeaders());
        //restTemplate.exchange(issue.getUrl(), HttpMethod.POST, httpEntity, Issue.class);
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.info("Executing updateIssue Azure API call");
        return this.getIssue(issue.getUrl());
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
        return issue.getState().equals(TRANSITION_CLOSED); //TODO property
    }

    @Override
    public boolean isIssueOpened(Issue issue) {
        if(issue.getState() == null){
            return true;
        }
        return issue.getState().equals(TRANSITION_ACTIVE); // TODO property
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing Azure Processing");
    }

    /**
     * @return Header consisting of API token used for authentication
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Authorization", "token ".concat(properties.getToken()));
        return httpHeaders;
    }

}