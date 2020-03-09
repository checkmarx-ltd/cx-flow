package com.checkmarx.flow.cucumber.integration.github;

import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.Filter;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GitHubCloseIssueSteps extends GitHubCommonSteps {

    private static final String INPUT_BASE_PATH = "cucumber/data/sample-sast-results/github-results-samples/";
    private static final String INPUT_VUL = "2-high-findings-same-vuln-same-file-with-not-ex-status.xml";
    private static final String INPUT_VUL_RESOLVED = "2-high-findings-same-vuln-same-file-resolved.xml";
    private static final String REPO_NAME = "VB_3845";

    private ScanRequest scanRequest;
    private Filter filter;

    @Before("@GitHubCloseIssue")
    public void init() {
        flowProperties.setBugTracker("GitHub");
        filter = Filter.builder()
                .type(Filter.Type.SEVERITY)
                .value("High")
                .build();
        cxProperties.setOffline(true);
    }

    @After("@GitHubCloseIssue")
    public void closeIssues() throws MachinaException {
        List<Issue> openIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");
        gitHubTestUtils.closeAllIssues(openIssues, scanRequest);
    }

    @And("for a given issue, with a given vulnerabilities")
    public void setIssueVulnerabilities() throws IOException, ExitThrowable {
        scanRequest = getBasicScanRequest();
        flowService.cxParseResults(scanRequest, getFileFromResourcePath(INPUT_BASE_PATH + INPUT_VUL));
        List<Issue> openIssuesList = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");

        Assert.assertTrue("Expected to find open issues, but found none", !openIssuesList.isEmpty());
    }

    @When("resolving the issue's all vulnerabilities")
    public void resolveIssueVulnerabilities() throws IOException, ExitThrowable {
        flowService.cxParseResults(scanRequest, getFileFromResourcePath(INPUT_BASE_PATH + INPUT_VUL_RESOLVED));
    }

    @Then("the issues should mark as closed")
    public void validateIssueIsClosed() {
        List<Issue> openIssuesList = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");

        Assert.assertTrue("Expected not to find open issues", openIssuesList.isEmpty());
    }

    private ScanRequest getBasicScanRequest() {
        return ScanRequest.builder()
                .application("TestApp")
                .product(ScanRequest.Product.CX)
                .project(REPO_NAME + "-master")
                .team("CxServer")
                .namespace("cxflowtestuser")
                .repoName(REPO_NAME)
                .repoUrl("http://localhost/repo.git")
                .repoUrlWithAuth("http://localhost/repo.git")
                .repoType(ScanRequest.Repository.GITHUB)
                .branch("master")
                .bugTracker(getCustomBugTrackerToGit())
                .refs(Constants.CX_BRANCH_PREFIX.concat("master"))
                .email(null)
                .incremental(false)
                .scanPreset("Checkmarx Default")
                .filters(Collections.singletonList(filter))
                .build();
    }

    @Override
    protected BugTracker getCustomBugTrackerToGit() {
        return super.getCustomBugTrackerToGit();
    }

    @Override
    protected File getFileFromResourcePath(String path) throws IOException {
        return super.getFileFromResourcePath(path);
    }
}