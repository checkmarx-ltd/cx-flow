package com.checkmarx.flow.cucumber.integration.github;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.github.GitHubTestUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest(classes = { CxFlowApplication.class, GitHubTestUtils.class})
public class GitHubOpenIssuesBySeverityFiltersSteps extends GitHubCommonSteps {

    private static final String INPUT_BASE_PATH = "cucumber/data/sample-sast-results/github-results-samples/";
    private static final String REPO_NAME = "VB_3845";

    private List<Filter> severityFilters;

    @Before("@GitHubCreateIssuesByFilter")
    public void init() {
        cxProperties.setOffline(true);
    }

    @After("@GitHubCreateIssuesByFilter")
    public void closeIssues() throws MachinaException {
        List<Issue> openIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");
        gitHubTestUtils.closeAllIssues(openIssues, scanRequest);
    }

    @Given("bug tracker is GitHub")
    public void setTagetToGitHub() {
        flowProperties.setBugTracker("GitHub");
    }

    @And("setting filter severity as {string}")
    public void setSeverityFilter(String severityFilter) {
        severityFilters = createFiltersListFromString(severityFilter);
    }

    @When("publishing {string}")
    public void publishResults(String input) throws IOException, ExitThrowable {
        scanRequest = getBasicScanRequest();
        sastScanner.cxParseResults(scanRequest, getFileFromResourcePath(INPUT_BASE_PATH + input));
    }

    @Then("{int} new issues should be opened according filters severity")
    public void validateOpenIssues(int expectedNumberOfIssues) {
        List<Issue> actualOpenIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");

        Assert.assertEquals("Open issues in GitHub as defined by filter severity are not as expected.",
                expectedNumberOfIssues, actualOpenIssues.size());
    }

    private List<Filter> createFiltersListFromString(String filters) {
        String[] filtersArr = filters.split(",");
        return Arrays.stream(filtersArr)
                .map(filterVal -> new Filter(Filter.Type.SEVERITY, filterVal))
                .collect(Collectors.toList());
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
                .filter(FilterConfiguration.fromSimpleFilters(severityFilters))
                .build();
    }
}