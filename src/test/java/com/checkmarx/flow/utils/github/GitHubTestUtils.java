package com.checkmarx.flow.utils.github;

import com.checkmarx.flow.custom.GitHubIssueTracker;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@TestComponent
public class GitHubTestUtils implements GitHubTestUtilsImpl {

    @Autowired
    private GitHubIssueTracker gitHubIssueTracker;

    @Override
    public List<Issue> getIssues(ScanRequest request) {
        return gitHubIssueTracker.getIssues(request);
    }

    @Override
    public List<Issue> filterIssuesByState(List<Issue> issuesList, String state) {
        return issuesList.stream()
                .filter(issue -> issue.getState().equals(state))
                .collect(Collectors.toList());
    }

    @Override
    public List<Issue> filterIssuesByStateAndByVulnerabilityName(List<Issue> issuesList, String state, String vulnerabilityName) {
        return issuesList.stream()
                .filter(issue -> issue.getState().equals(state) && issue.getTitle().contains(vulnerabilityName))
                .collect(Collectors.toList());
    }

    @Override
    public int getIssueLinesCount(Issue issue) {
        String bodyLinePattern = "Line #[0-9]+";
        Pattern pattern = Pattern.compile(bodyLinePattern);
        Matcher matcher = pattern.matcher(issue.getBody());

        int count = 0;
        while (matcher.find()) {
            count++;
        }

        return count;
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request)  {
        try {
            gitHubIssueTracker.closeIssue(issue, request);
        } catch (MachinaException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeAllIssues(List<Issue> issuesList, ScanRequest request) {
        if (issuesList !=null) {
            issuesList.forEach(issue ->
                    closeIssue(issue, request));
        }
    }
}