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
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GitHubOpenIssuesFromDifferentFilesSteps extends GitHubCommonSteps {

    private static final String INPUT_BASE_PATH = "cucumber/data/sample-sast-results/github-results-samples/";
    private static final String REPO_NAME = "VB_3845";

    private Filter filter;
    private ScanRequest scanRequest;

    @Before("@GitHubCreateIssuesFromDifferentFiles")
    public void init() {
        flowProperties.setBugTracker("GitHub");
        filter = Filter.builder()
                .type(Filter.Type.SEVERITY)
                .value("High")
                .build();
        cxProperties.setOffline(true);
    }

    @After("@GitHubCreateIssuesFromDifferentFiles")
    public void closeIssues() throws MachinaException {
        List<Issue> openIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");
        gitHubTestUtils.closeAllIssues(openIssues, scanRequest);
    }

    @When("publishing results from different files for input {string}")
    public void publishResults(String input) throws IOException, ExitThrowable {
        scanRequest = getBasicScanRequest();
        flowService.cxParseResults(scanRequest, getFileFromResourcePath(INPUT_BASE_PATH + input));
    }

    @Then("{int} new issues should be open")
    public void validateOpenIssues(int expectedNumberOfIssues) {
        List<Issue> actualOpenIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");

        Assert.assertEquals("Open issues in GitHub as set with different files are not as expected.",
                expectedNumberOfIssues, actualOpenIssues.size());
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