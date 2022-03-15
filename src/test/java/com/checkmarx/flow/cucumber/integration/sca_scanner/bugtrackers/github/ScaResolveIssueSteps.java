package com.checkmarx.flow.cucumber.integration.sca_scanner.bugtrackers.github;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.config.properties.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.JsonUtils;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.ResultsService;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.checkmarx.flow.cucumber.common.Constants.CUCUMBER_DATA_DIR;

@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@RequiredArgsConstructor
public class ScaResolveIssueSteps {

    private static final String INPUT_BASE_PATH = CUCUMBER_DATA_DIR + "/sample-sca-results/";
    private static final String INPUT_FILE = "16-findings-6-high-10-medium.json";
    private static final String RESOLVER_INPUT_FILE = "16-findings-6-high-10-medium-resolver.json";
    private static final String PROJECT_NAME = "CxScaTest";
    private static final String PROJECT_KEY = "NT";

    private final FlowProperties flowProperties;
    private final IJiraTestUtils jiraUtils;
    private final ResultsService resultsService;
    private final JiraProperties jiraProperties;

    private int openIssuesBeforeResolving;
    private int closedIssuesBeforeResolving;
    private int openIssuesAfterResolving;
    private int closedIssuesAfterResolving;

    @After
    public void cleanup() {
        jiraUtils.cleanProject(PROJECT_KEY);
    }

    @Given("scanner is SCA")
    public void setScanner() {
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
    }

    @And("bug tracker is Jira")
    public void setBugTracker() {
        flowProperties.setBugTracker("JIRA");
    }

    @And("bug tracker contains open vulnerabilities")
    public void createBugTrackerOpenVulnerabilities() throws IOException, MachinaException {
        processResultsByInput(INPUT_FILE);

        Map<String, Integer> issuesByStatus = jiraUtils.getIssuesByStatus(PROJECT_KEY);
        openIssuesBeforeResolving = getOpenIssues(issuesByStatus);
        closedIssuesBeforeResolving = getClosedIssues(issuesByStatus);
    }

    @When("resolving on of the vulnerabilities")
    public void resolveVulnerability() throws IOException, MachinaException {
        processResultsByInput(RESOLVER_INPUT_FILE);

        Map<String, Integer> issuesByStatus = jiraUtils.getIssuesByStatus(PROJECT_KEY);
        openIssuesAfterResolving = getOpenIssues(issuesByStatus);
        closedIssuesAfterResolving = getClosedIssues(issuesByStatus);
    }

    @Then("resolved vulnerability status should getting update to closed")
    public void validateResolveVulnerabilityStatus() {
        String errorMessage = "Total open issues before the fix: " + openIssuesBeforeResolving + ". Total open issues after the fix: " + openIssuesAfterResolving;
        Assert.assertTrue("Expected number of open issues left after vulnerabilities resolving will be reduced. " + errorMessage,
                openIssuesAfterResolving < openIssuesBeforeResolving);
        errorMessage = "Total closed issues before the fix: " + closedIssuesBeforeResolving + ". Total closed issues after the fix: " + closedIssuesAfterResolving;
        Assert.assertTrue("Expected number of closed issues after vulnerabilities resolving will be increased. " + errorMessage,
                closedIssuesBeforeResolving < closedIssuesAfterResolving);
    }

    private ScanRequest getBasicScanRequest() {
        return ScanRequest.builder()
                .product(ScanRequest.Product.CX)
                .project(PROJECT_NAME)
                .namespace("cxflowtestuser")
                .repoName("htmx")
                .repoType(ScanRequest.Repository.GITHUB)
                .branch("master")
                .bugTracker(getBasicBugTrackerToJira())
                .refs(Constants.CX_BRANCH_PREFIX.concat("refs/heads/master"))
                .build();
    }

    private BugTracker getBasicBugTrackerToJira() {
        return BugTracker.builder()
                .issueType(jiraProperties.getIssueType())
                .projectKey(PROJECT_KEY)
                .type(BugTracker.Type.JIRA)
                .issueType("Bug")
                .closedStatus(Collections.singletonList("Done"))
                .closeTransition("Done")
                .openStatus(jiraProperties.getOpenStatus())
                .priorities(jiraProperties.getPriorities())
                .build();

    }

    private void processResultsByInput(String inputFile) throws IOException, MachinaException {
        ScanResults scanResults = JsonUtils.json2Object(TestUtils.getFileFromRelativeResourcePath(INPUT_BASE_PATH + inputFile), ScanResults.class);
        resultsService.processResults(getBasicScanRequest(), scanResults, null);
    }

    private int getOpenIssues(Map<String, Integer> issues) {
        return getIssuesPerStatuses(issues, jiraProperties.getOpenStatus());
    }

    private int getClosedIssues(Map<String, Integer> issues) {
        return getIssuesPerStatuses(issues,jiraProperties.getClosedStatus());
    }

    private int getIssuesPerStatuses(Map<String, Integer> issues, List<String> statuses) {
        int result = 0;
        for (String key: issues.keySet()) {
            if (statuses.stream().map(String::toLowerCase).collect(Collectors.toList()).contains(key.toLowerCase())) {
                result += issues.get(key);
            }
        }
        return result;
    }
}