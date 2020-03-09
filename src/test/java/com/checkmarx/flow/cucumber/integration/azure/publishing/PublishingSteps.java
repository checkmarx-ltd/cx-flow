package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.cucumber.common.Constants;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.sdk.config.CxProperties;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.HtmlUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.*;

@SpringBootTest(classes = {CxFlowApplication.class})
public class PublishingSteps {
    private static final String PROPERTIES_FILE_PATH = "cucumber/features/integrationTests/azure/publishing.properties";

    private static final String REPORT_WITH_ONE_FINDING = "1-finding.xml";

    @Autowired
    private FlowService flowService;

    @Autowired
    private ADOProperties adoProperties;

    @Autowired
    private CxProperties cxProperties;

    @Autowired
    private AzureDevopsClient adoClient;

    private String projectName;
    private String sastReportFilename;

    @Before
    public void prepareEnvironment() throws IOException {
        Properties testProperties = TestUtils.getPropertiesFromResource(PROPERTIES_FILE_PATH);
        projectName = testProperties.getProperty("projectName");

        cxProperties.setOffline(true);

        adoClient.ensureProjectExists(projectName);
        adoClient.deleteProjectIssues(projectName);
    }

    @Given("Azure DevOps initially contains {int} open issue with title: {string}")
    public void azureDevOpsInitiallyContainsIssue(int issueCount, String title) {
        createIssues(issueCount, title, "any description");
    }

    @Given("Azure DevOps initially contains {int} open issue with title: {string} and description containing link: {string}")
    public void azureDevOpsInitiallyContainsIssue(int issueCount, String title, String link) {
        createIssues(issueCount, title, link);
    }

    @Given("Azure DevOps doesn't contain any issues")
    public void azureDevOpsDoesnTContainAnyIssues() throws IOException {
        verifyIssueCount(0);
    }

    @Then("Azure DevOps contains {int} issues")
    public void azureDevOpsContainsIssueCountIssues(int expectedCount) throws IOException {
        verifyIssueCount(expectedCount);
    }

    @Given("Azure DevOps contains {int} open issue with title: {string} and description containing link: {string}")
    public void azureDevOpsContainsIssue(int issueCount, String title, String link) throws IOException {
        List<Issue> issues = adoClient.getIssues(projectName);
        assertEquals("Unexpected issue count after publishing.", issueCount, issues.size());
        String linkInHtmlAttribute = HtmlUtils.htmlEscape(link);
        for (Issue issue : issues) {
            assertEquals("Invalid issue state.", adoProperties.getOpenStatus(), issue.getState());
            assertEquals("Invalid issue title.", title, issue.getTitle());

            String description = issue.getDescription();
            assertNotNull("Issue is missing description.", description);
            assertTrue("Description doesn't contain the link: " + link, description.contains(linkInHtmlAttribute));
        }
    }

    @When("publishing the report")
    public void publishingTheReport() throws IOException, ExitThrowable {
        BugTracker bugTracker = BugTracker.builder()
                .type(BugTracker.Type.CUSTOM)
                .customBean("Azure")
                .build();

        ScanRequest request = ScanRequest.builder()
                .bugTracker(bugTracker)
                .namespace(projectName)
                .repoName(projectName)
                .branch(AzureDevopsClient.DEFAULT_BRANCH)
                .product(ScanRequest.Product.CX)
                .build();

        String path = Paths.get(Constants.SAMPLE_SAST_RESULTS_DIR, sastReportFilename).toString();
        File sastReport = TestUtils.getFileFromResource(path);

        flowService.cxParseResults(request, sastReport);
    }

    @And("SAST report contains {int} findings with the same vulnerability type and in the same file, and not marked as false positive")
    public void sastReportContainsFindingsSameVulnAndFile(int findingCount) {
        switch (findingCount) {
            case 0:
                sastReportFilename = "empty-results.xml";
                break;
            case 1:
                sastReportFilename = REPORT_WITH_ONE_FINDING;
                break;
            case 2:
                sastReportFilename = "2-findings-same-vuln-type-same-file.xml";
                break;
            default:
                throwFindingCountError(findingCount);
                break;
        }
    }

    @And("SAST report contains 2 findings, each with a different vulnerability type and filename, and not marked as false positive")
    public void sastReportContainsNumberOfFindingsDifferentVulnAndFile() {
        sastReportFilename = "2-findings-different-vuln-type-different-files.xml";
    }

    @And("SAST report contains 3 findings, all marked as false positive")
    public void sastReportContainsFindingsAllMarkedAsFalsePositive() {
        sastReportFilename = "3-findings-all-false-positive.xml";
    }

    @And("SAST report contains 1 finding with vulnerability type {string} and filename {string}")
    public void sastReportContainsFinding(String vulnerabilityType, String filename) {
        sastReportFilename = REPORT_WITH_ONE_FINDING;
    }

    @And("SAST report contains 1 finding with vulnerability type {string}, filename {string}, link {string}, and not marked as false positive")
    public void sastReportContainsFindingWithLink(String vulnerabilityType, String filename, String link) {
        sastReportFilename = REPORT_WITH_ONE_FINDING;
    }

    @And("an issue with the title {string} is in {string} state")
    public void anIssueWithTheTitleIsInState(String title, String state) throws IOException {
        List<Issue> issues = adoClient.getIssues(projectName);

        Optional<Issue> targetIssue = issues.stream()
                .filter(issue -> StringUtils.equals(issue.getTitle(), title))
                .findFirst();

        assertTrue("Unable to find an issue with the title: " + title, targetIssue.isPresent());

        String actualState = targetIssue.get().getState();
        String message = String.format("Unexpected issue state (issue ID: %s).", targetIssue.get().getId());
        assertEquals(message, state, actualState);
    }

    private void throwFindingCountError(int findingCount) {
        fail("Unexpected finding count: " + findingCount);
    }

    private void verifyIssueCount(int expectedCount) throws IOException {
        int actualCount = adoClient.getIssueCount(projectName);
        assertEquals("Incorrect number of issues.", expectedCount, actualCount);
    }

    private void createIssues(int issueCount, String title, String description) {
        Issue issue = Issue.builder()
                .title(title)
                .description(description)
                .projectName(projectName)
                .state(adoProperties.getOpenStatus())
                .build();

        for (int i = 0; i < issueCount; i++) {
            adoClient.createIssue(issue);
        }
    }
}
