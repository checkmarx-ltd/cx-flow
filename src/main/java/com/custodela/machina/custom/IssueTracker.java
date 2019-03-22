package com.custodela.machina.custom;
import com.custodela.machina.dto.Issue;
import com.custodela.machina.dto.ScanRequest;
import com.custodela.machina.dto.ScanResults;
import com.custodela.machina.exception.MachinaException;

import java.util.List;

public interface IssueTracker {
    public void init(ScanRequest request) throws MachinaException;
    public void complete(ScanRequest request) throws MachinaException;
    public String getFalsePositiveLabel() throws MachinaException;
    public List<Issue> getIssues(ScanRequest request) throws MachinaException;
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException;
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException;
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue) throws MachinaException;
    public String getIssueKeyFormat();
    public String getIssueKey(Issue issue, ScanRequest request);
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request);
    public boolean isIssueClosed(Issue issue);
    public boolean isIssueOpened(Issue issue);
}
