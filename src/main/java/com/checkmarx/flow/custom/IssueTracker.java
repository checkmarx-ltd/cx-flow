package com.checkmarx.flow.custom;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.sdk.dto.ScanResults;

import java.util.List;

public interface IssueTracker {
    void init(ScanRequest request, ScanResults results) throws MachinaException;
    void complete(ScanRequest request, ScanResults results) throws MachinaException;
    String getFalsePositiveLabel() throws MachinaException;
    List<Issue> getIssues(ScanRequest request) throws MachinaException;
    Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException;
    void closeIssue(Issue issue, ScanRequest request) throws MachinaException;
    Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException;
    String getIssueKey(Issue issue, ScanRequest request);
    String getXIssueKey(ScanResults.XIssue issue, ScanRequest request);
    boolean isIssueClosed(Issue issue, ScanRequest request);
    boolean isIssueOpened(Issue issue, ScanRequest request);
}
