package com.checkmarx.jira;

import com.checkmarx.sdk.dto.Filter;

import java.io.IOException;
import java.util.Map;

public interface IJiraTestUtils {

    void deleteIssue(String issueKey);

    void cleanProject(String projectName);

    int getNumberOfIssuesInProject(String projectKey);

    Map<Filter.Severity, Integer> getIssuesPerSeverity(String projectKey);

    int getNumberOfVulnerabilites(String projectKey);

    String getIssueFilename(String projectKey);

    String getIssueVulnerability(String projectKey);

    int getFirstIssueNumOfFindings(String projectKey);

    void ensureProjectExists(String project) throws IOException;

    void ensureIssueTypeExists(String issueType) throws IOException;

    String getIssuePriority(String projectKey);

    Long getIssueUpdatedTime(String projectKey);

    String getIssueStatus(String projectKey);

    Long getFirstIssueId(String projectKey);

    Map<String, Integer> getIssuesByStatus(String projectKey);
}
