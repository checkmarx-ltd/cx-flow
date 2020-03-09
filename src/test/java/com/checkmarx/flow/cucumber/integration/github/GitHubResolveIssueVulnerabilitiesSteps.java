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

public class GitHubResolveIssueVulnerabilitiesSteps extends GitHubCommonSteps {

    private static final String INPUT_BASE_PATH = "cucumber/data/sample-sast-results/github-results-samples/";
    private static final String INPUT_VUL_BEFORE_FIX = "5-findings-different-vuln-same-file.xml";
    private static final String INPUT_VUL_AFTER_FIX = "2-high-findings-same-vuln-same-file-with-not-ex-status.xml";
    private static final String REPO_NAME = "VB_3845";
    private static final int EXPECTED_CODE_LINES_BEFORE_FIX = 2;
    private static final int EXPECTED_CODE_LINES_after_FIX = 1;

    private ScanRequest scanRequest;
    private Filter filter;

    @Before("@GitHubResolveIssueVulnerabilities")
    public void init() {
        flowProperties.setBugTracker("GitHub");
        filter = Filter.builder()
                .type(Filter.Type.SEVERITY)
                .value("High")
                .build();
        cxProperties.setOffline(true);
    }

    @After("@GitHubResolveIssueVulnerabilities")
    public void closeIssues() throws MachinaException {
        List<Issue> openIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");
        gitHubTestUtils.closeAllIssues(openIssues, scanRequest);
    }

    @And("for a given type, there is an open issue with multiple vulnerabilities")
    public void setOpenIssueWithVulnerabilities() throws IOException, ExitThrowable {
        scanRequest = getBasicScanRequest();
        flowService.cxParseResults(scanRequest, getFileFromResourcePath(INPUT_BASE_PATH + INPUT_VUL_BEFORE_FIX));

        List<Issue> openIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");
        int issueLinesCount = gitHubTestUtils.getIssueLinesCount(openIssues.get(0));

        Assert.assertEquals("Expected issue's code lines before fix aren't as expected",
                EXPECTED_CODE_LINES_BEFORE_FIX, issueLinesCount);
    }

    @When("resolving a vulnerability")
    public void resolveIssue() throws IOException, ExitThrowable {
        flowService.cxParseResults(scanRequest, getFileFromResourcePath(INPUT_BASE_PATH + INPUT_VUL_AFTER_FIX));
    }

    @Then("the issue's code lines should be update")
    public void validateIssueCodeLines() {
        List<Issue> openIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");
        int issueLinesCount = gitHubTestUtils.getIssueLinesCount(openIssues.get(0));

        Assert.assertEquals("Expected issue's code lines after fix aren't as expected",
                EXPECTED_CODE_LINES_after_FIX, issueLinesCount);
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