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

public class GitHubCloseIssueSteps extends GitHubCommonSteps {

    private static final String INPUT_BASE_PATH = "cucumber/data/sample-sast-results/github-results-samples/";
    private static final String INPUT_VUL = "2-high-findings-same-vuln-same-file-with-not-ex-status.xml";
    private static final String INPUT_VUL_RESOLVED = "2-high-findings-same-vuln-same-file-resolved.xml";
    private static final String REPO_NAME = "VB_3845";

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

        Assert.assertFalse("Expected to find open issues, but found none", openIssuesList.isEmpty());
    }

    @When("resolving all vulnerabilities for an issue")
    public void resolveIssueVulnerabilities() throws IOException, ExitThrowable {
        flowService.cxParseResults(scanRequest, getFileFromResourcePath(INPUT_BASE_PATH + INPUT_VUL_RESOLVED));
    }

    @Then("the issues should be mark as closed")
    public void validateIssueIsClosed() {
        List<Issue> openIssuesList = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");

        Assert.assertTrue("Expected not to find open issues", openIssuesList.isEmpty());
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