package com.checkmarx.flow.cucumber.integration.azure.publishing.issueprocessing;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.cucumber.common.Constants;
import com.checkmarx.flow.cucumber.common.utils.JsonUtils;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.azure.publishing.AzureDevopsClient;
import com.checkmarx.flow.cucumber.integration.azure.publishing.PublishingStepsBase;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.sdk.config.CxProperties;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.HtmlUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class PublishingSteps extends PublishingStepsBase {
    private static final String REPORT_WITH_ONE_FINDING = "1-finding.xml";

    // Description cannot be null or empty during issue creation.
    private static final String DESCRIPTION_STUB = "any description";

    @Autowired
    private FlowService flowService;

    @Autowired
    private ADOProperties adoProperties;

    @Autowired
    private CxProperties cxProperties;

    @Autowired
    private AzureDevopsClient adoClient;

    private String sastReportFilename;

    // Used to avoid duplicate ADO requests in different steps.
    private List<Issue> issuesAfterPublish;

    // For the 'unreachable' scenario.
    private boolean expectingException;
    private Throwable lastCxFlowException;

    @Before
    public void prepareEnvironment() throws IOException {
        cxProperties.setOffline(true);
        adoClient.ensureProjectExists();
        adoClient.deleteProjectIssues();
    }

    @Given("Azure DevOps initially contains {int} {string} issue with title: {string}")
    public void azureDevOpsInitiallyContainsIssueWithState(int issueCount, String stateDescription, String title) throws IOException {
        String desiredState = stateDescription.equals("open") ? adoProperties.getOpenStatus() : adoProperties.getClosedStatus();
        String stateAllowedForCreation = adoProperties.getOpenStatus();

        boolean needToUpdateAfterCreation = !desiredState.equals(stateAllowedForCreation);
        for (int i = 0; i < issueCount; i++) {
            Issue issue = createIssue(title, DESCRIPTION_STUB);
            if (needToUpdateAfterCreation) {
                adoClient.updateIssueState(issue, desiredState);
            }
        }
    }

    @Given("Azure DevOps initially contains {int} open issue with title: {string} and description containing link: {string}")
    public void azureDevOpsInitiallyContainsIssue(int issueCount, String title, String link) throws IOException {
        String description = String.format("Description containing %s link", link);
        for (int i = 0; i < issueCount; i++) {
            createIssue(title, description);
        }
    }

    @Given("Azure DevOps initially contains 2 open issues with titles {string} and {string}")
    public void azureDevOpsInitiallyContainsOpenIssues(String title1, String title2) throws IOException {
        createIssue(title1, DESCRIPTION_STUB);
        createIssue(title2, DESCRIPTION_STUB);
    }

    @Given("Azure DevOps doesn't contain any issues")
    public void azureDevOpsDoesnTContainAnyIssues() throws IOException {
        List<Issue> issues = getIssues();
        verifyIssueCount(issues, 0);
    }

    @Then("Azure DevOps contains {int} issues")
    public void azureDevOpsContainsIssueCountIssues(int expectedCount) throws IOException {
        List<Issue> issues = getIssues();
        verifyIssueCount(issues, expectedCount);
    }

    @Given("Azure DevOps contains {int} open issue with title: {string} and description containing link: {string}")
    public void azureDevOpsContainsIssue(int issueCount, String title, String link) throws IOException {
        List<Issue> issues = getIssues();
        verifyIssueCount(issues, issueCount);
        String linkInHtmlAttribute = HtmlUtils.htmlEscape(link);
        for (Issue issue : issues) {
            assertEquals(adoProperties.getOpenStatus(), issue.getState(), "Invalid issue state.");
            assertEquals(title, issue.getTitle(), "Invalid issue title.");

            String description = issue.getBody();
            assertNotNull(description, "Issue is missing description.");
            assertTrue(description.contains(linkInHtmlAttribute), "Description doesn't contain the link: " + link);
        }
    }

    @Then("Azure DevOps contains {int} issue with the title {string} and {string} state")
    public void azureDevOpsContainsClosedIssueWithTheTitle(int issueCount, String title, String state) throws IOException {
        List<Issue> issues = getIssues();
        verifyIssueCount(issues, issueCount);
        boolean allIssuesAreCorrect = issues.stream().allMatch(issueHas(title, state));
        assertTrue(allIssuesAreCorrect, getIssueError(issues));
    }

    @When("^publishing (?:the|a SAST) report$")
    public void publishingTheReport() throws Exception, ExitThrowable {
        ScanRequest request = prepareScanRequest();

        String sastReportPath = Paths.get(Constants.SAMPLE_SAST_RESULTS_DIR, sastReportFilename).toString();
        File sastReport = TestUtils.getFileFromResource(sastReportPath);

        try {
            flowService.cxParseResults(request, sastReport);
        } catch (Throwable e) {
            if (expectingException) {
                lastCxFlowException = e;
            } else {
                throw e;
            }
        }
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
                fail("Unexpected finding count: " + findingCount);
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

    @And("SAST report contains 1 finding with vulnerability type {string} and filename {string}, not marked as false positive")
    public void sastReportContainsFinding(String vulnerabilityType, String filename) {
        sastReportFilename = REPORT_WITH_ONE_FINDING;
    }

    @And("SAST report contains 1 finding with vulnerability type {string}, filename {string}, link {string}, and not marked as false positive")
    public void sastReportContainsFindingWithLink(String vulnerabilityType, String filename, String link) {
        sastReportFilename = REPORT_WITH_ONE_FINDING;
    }

    @And("SAST report contains 2 findings with vulnerability type {string}, filename {string} and marked as false positive")
    public void sastReportContainsFindingsMarkedAsFalsePositive(String vulnerabilityType, String filename) {
        sastReportFilename = "2-findings-same-vuln-type-same-file-false-positive.xml";
    }

    @And("^(?:one of the issues|the other issue) has the title: \"(.*)\" and is in \"(.*)\" state$")
    public void oneOfTheIssuesHasTheTitleAndIsInState(String title, String state) {
        List<Issue> matchingIssues = issuesAfterPublish.stream()
                .filter(issue -> issue.getTitle().equals(title) &&
                        issue.getState().equals(state))
                .collect(Collectors.toList());

        verifyIssueCount(matchingIssues, 1);
    }

    @And("an issue with the title {string} is in {string} state")
    public void anIssueWithTheTitleIsInState(String title, String state) {
        Optional<Issue> targetIssue = issuesAfterPublish.stream()
                .filter(issue -> StringUtils.equals(issue.getTitle(), title))
                .findFirst();

        assertTrue(targetIssue.isPresent(), "Unable to find an issue with the title: " + title);

        String actualState = targetIssue.get().getState();
        String message = String.format("Unexpected issue state (issue ID: %s).", targetIssue.get().getId());
        assertEquals(state, actualState, message);
    }

    @Given("invalid Azure DevOps URL is provided in configuration")
    public void invalidAzureDevOpsURLIsProvidedInConfiguration() {
        adoProperties.setUrl("http://invalid.url");
        expectingException = true;
        sastReportFilename = REPORT_WITH_ONE_FINDING;
    }

    @Then("CxFlow should throw {string}")
    public void cxflowShouldThrowAnException(String exceptionClassName) {
        assertNotNull(lastCxFlowException, "Expected CxFlow to throw an exception.");

        Class<?> expectedExceptionClass = null;
        try {
            expectedExceptionClass = Class.forName(exceptionClassName);
        } catch (ClassNotFoundException e) {
            fail("Invalid class name: " + exceptionClassName);
        }
        Class<? extends Throwable> actualExceptionClass = lastCxFlowException.getClass();
        log.info("Caught a {}: {}", actualExceptionClass.getName(), lastCxFlowException.getMessage());

        assertEquals(expectedExceptionClass, actualExceptionClass, "Unexpected exception type.");
    }

    private void verifyIssueCount(List<Issue> issues, int expectedCount) {
        assertEquals(expectedCount, issues.size(), "Incorrect number of issues.");
    }

    private Issue createIssue(String title, String description) throws IOException {
        Issue issue = new Issue();
        issue.setTitle(title);
        issue.setBody(description);
        issue.setState(adoProperties.getOpenStatus());
        String id = adoClient.createIssue(issue);
        issue.setId(id);
        return issue;
    }

    private static Supplier<String> getIssueError(List<Issue> issues) {
        final String MESSAGE = "Unexpected issue fields. ";
        return () -> {
            try {
                return MESSAGE + JsonUtils.object2json(issues);
            } catch (IOException e) {
                return MESSAGE;
            }
        };
    }

    private static Predicate<? super Issue> issueHas(String title, String state) {
        return issue -> issue.getTitle().equals(title) &&
                issue.getState().equals(state);
    }

    private List<Issue> getIssues() throws IOException {
        issuesAfterPublish = adoClient.getIssues();
        return issuesAfterPublish;
    }

    private ScanRequest prepareScanRequest() {
        BugTracker bugTracker = BugTracker.builder()
                .type(BugTracker.Type.CUSTOM)
                .customBean(ISSUE_TRACKER_NAME)
                .build();

        return ScanRequest.builder()
                .bugTracker(bugTracker)
                .namespace(getOrganizationName())
                .project(getProjectName())
                .repoName(getProjectName())
                .branch(AzureDevopsClient.DEFAULT_BRANCH)
                .product(ScanRequest.Product.CX)
                .build();
    }
}
