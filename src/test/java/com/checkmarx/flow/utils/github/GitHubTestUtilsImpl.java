package com.checkmarx.flow.utils.github;

import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import org.springframework.http.HttpEntity;

import java.io.IOException;
import java.util.List;

public interface GitHubTestUtilsImpl {

    List<Issue> getIssues(ScanRequest request);

    List<Issue> filterIssuesByState(List<Issue> issuesList, String state);

    List<Issue> filterIssuesByStateAndByVulnerabilityName(List<Issue> issuesList, String state, String vulnerabilityName);

    int getIssueLinesCount(Issue issue);

    void closeIssue(Issue issue, ScanRequest request) throws MachinaException;

    void closeAllIssues(List<Issue> issuesList, ScanRequest request) throws MachinaException;

    String createSignature(String requestBody);

    String loadWebhookRequestBody(GitHubTestUtils.EventType eventType) throws IOException;

    HttpEntity<String> prepareWebhookRequest(GitHubTestUtils.EventType eventType);
}