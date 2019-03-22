package com.custodela.machina.custom;


import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.dto.Issue;
import com.custodela.machina.dto.ScanRequest;
import com.custodela.machina.dto.ScanResults;
import com.custodela.machina.exception.MachinaException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

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

    public GitLabIssueTracker(RestTemplate restTemplate, GitLabProperties properties, MachinaProperties machinaProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.machinaProperties = machinaProperties;
    }


    @Override
    public void init(ScanRequest request) throws MachinaException {
        log.info("Running gl init");
    }

    @Override
    public void complete(ScanRequest request) throws MachinaException {
        log.info("Running gl complete");
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return null;
    }

    @Override
    public List<Issue> getIssues(ScanRequest request) throws MachinaException {
        return Collections.emptyList();
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.info("Creating gl issue");
        return null;
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        log.info("Closing gl issue");
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue) throws MachinaException {
        log.info("Update gl issue");
        return null;
    }

    @Override
    public String getIssueKeyFormat() {
        return null;
    }

    @Override
    public String getIssueKey(Issue issue, ScanRequest request) {
        return null;
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        return null;
    }

    @Override
    public boolean isIssueClosed(Issue issue) {
        return false;
    }

    @Override
    public boolean isIssueOpened(Issue issue) {
        return false;
    }
}