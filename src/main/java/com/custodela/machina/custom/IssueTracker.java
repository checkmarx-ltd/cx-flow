package com.custodela.machina.custom;
import com.custodela.machina.dto.Issue;
import com.custodela.machina.dto.ScanRequest;
import com.custodela.machina.dto.ScanResults;
import com.custodela.machina.exception.MachinaException;

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
    boolean isIssueClosed(Issue issue);
    boolean isIssueOpened(Issue issue);
}
