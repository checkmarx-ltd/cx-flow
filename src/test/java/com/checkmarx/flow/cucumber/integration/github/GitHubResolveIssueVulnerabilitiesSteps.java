package com.checkmarx.flow.cucumber.integration.github;

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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GitHubResolveIssueVulnerabilitiesSteps extends GitHubCommonSteps {

    private static final String INPUT_BASE_PATH = "cucumber/data/sample-sast-results/github-results-samples/";
    private static final String INPUT_VUL_BEFORE_FIX = "5-findings-different-vuln-same-file.xml";
    private static final String INPUT_VUL_AFTER_FIX = "2-high-findings-same-vuln-same-file-with-not-ex-status.xml";

    private static final String REPO_NAME = "VB_3845";

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

    @And("for a given type, there is an opened issue with multiple vulnerabilities")
    public void setOpenIssueWithVulnerabilities() throws IOException, ExitThrowable {
        scanRequest = getBasicScanRequest();
        sastScannerService.cxParseResults(scanRequest, getFileFromResourcePath(INPUT_BASE_PATH + INPUT_VUL_BEFORE_FIX));

        List<Issue> openIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");
        int issueLinesCount = gitHubTestUtils.getIssueLinesCount(openIssues.get(0));

        Assert.assertEquals("Expected issue's code lines before fix aren't as expected",
                2, issueLinesCount);
    }

    @When("resolving a vulnerability")
    public void resolveIssue() throws IOException, ExitThrowable {
        sastScannerService.cxParseResults(scanRequest, getFileFromResourcePath(INPUT_BASE_PATH + INPUT_VUL_AFTER_FIX));
    }

    @Then("the issue's code lines should be update")
    public void validateIssueCodeLines() {
        List<Issue> openIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");
        int issueLinesCount = gitHubTestUtils.getIssueLinesCount(openIssues.get(0));

        Assert.assertEquals("Expected issue's code lines after fix aren't as expected",
                1, issueLinesCount);
    }

    private ScanRequest getBasicScanRequest() {
        return ScanRequest.builder()
                .product(ScanRequest.Product.CX)
                .project(REPO_NAME + "-" + MASTER_BRANCH_NAME)
                .team(DEFAULT_TEAM_NAME)
                .namespace(DEFAULT_TEST_NAMESPACE)
                .repoName(REPO_NAME)
                .repoType(ScanRequest.Repository.GITHUB)
                .branch(MASTER_BRANCH_NAME)
                .bugTracker(getCustomBugTrackerToGit())
                .refs(Constants.CX_BRANCH_PREFIX.concat(MASTER_BRANCH_NAME))
                .email(null)
                .incremental(false)
                .filters(Collections.singletonList(filter))
                .build();
    }
}