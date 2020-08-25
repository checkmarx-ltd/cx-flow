package com.checkmarx.flow.cucumber.integration.cli.sca;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.ScaProperties;
import io.cucumber.java.*;
import io.cucumber.java.en.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@Slf4j
@RequiredArgsConstructor
public class ScaCliSteps {
    private static final String REPO_ARGS = " --repo-url=https://github.com/cxflowtestuser/public-rest-repo --repo-name=CLI-public-rest-repo --branch=master --blocksysexit";
    private static final String GITHUB_REPO_ARGS = REPO_ARGS + " --github ";
    private static final String JIRA_PROJECT = "SCIT";

    private final FlowProperties flowProperties;
    private final JiraProperties jiraProperties;
    private final CxFlowRunner cxFlowRunner;
    private Throwable cxFlowExecutionException;

    private int expectedHigh;
    private int expectedMedium;
    private int expectedLow;
    private String customScaProjectName;

    @Autowired
    private IJiraTestUtils jiraUtils;

    @Before
    public void beforeEachScenario() throws IOException {
        log.info("Setting bugTracker: Jira");
        flowProperties.setBugTracker("JIRA");

        List<String> scaOnly = Collections.singletonList(ScaProperties.CONFIG_PREFIX);
        flowProperties.setEnabledVulnerabilityScanners(scaOnly);

        log.info("Jira project key: {}", JIRA_PROJECT);
        jiraProperties.setProject(JIRA_PROJECT);
        initJiraBugTracker();
    }

    @After
    public void afterEachScenario() {
        log.info("Cleaning JIRA project: {}", jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }

    @When("running a SCA scan with break-build on {word}")
    public void runningWithBreakBuild(String issueType) {
        StringBuilder commandBuilder = new StringBuilder();

        switch (issueType) {
            case "success":
                commandBuilder.append("--scan  --severity=High --app=MyApp --cx-project=test").append(GITHUB_REPO_ARGS);
                break;
            case "missing-mandatory-parameter":
                commandBuilder.append("--severity=High --severity=Medium").append(GITHUB_REPO_ARGS);
                break;
            case "error-processing-request":
                commandBuilder.append("--scan  --severity=High --app=MyApp").append(GITHUB_REPO_ARGS);
                break;
            case "missing-project":
                commandBuilder.append("--scan  --severity=High --app=MyApp --f=nofile").append(REPO_ARGS);
                break;
            default:
                throw new PendingException("Issues type " + issueType + " isn't supported");
        }

        log.info("Running CxFlow scan with command line: {}", commandBuilder.toString());
        Throwable exception = null;
        try {
            TestUtils.runCxFlow(cxFlowRunner, commandBuilder.toString());
        } catch (Throwable e) {
            exception = e;
        }
        cxFlowExecutionException = exception;
    }

    @Then("run should exit with exit code {int}")
    public void validateExitCode(int expectedExitCode) {
        Assert.assertNotNull("Expected an exception to be thrown.", cxFlowExecutionException);
        Assert.assertEquals(InvocationTargetException.class, cxFlowExecutionException.getClass());

        Throwable targetException = ((InvocationTargetException) cxFlowExecutionException).getTargetException();
        Assert.assertTrue(targetException instanceof ExitThrowable);

        int actualExitCode = ((ExitThrowable) targetException).getExitCode();

        Assert.assertEquals("The expected exit code did not match", expectedExitCode, actualExitCode);
    }

    @Given("code has x High, y Medium and z low issues")
    public void setIssues() {
        expectedHigh = 6;
        expectedMedium = 9;
        expectedLow = 0;
    }

    @When("running sca scan {word}")
    public void runnningScanWithFilter(String filter) {
        StringBuilder commandBuilder = new StringBuilder();

        switch (filter) {
            case "no-filter":
                commandBuilder.append(" --scan --app=MyApp --cx-project=test").append(GITHUB_REPO_ARGS);
                break;
            // case "filter-High-and-Medium":
            //     commandBuilder.append(" --scan --app=MyApp --cx-project=test").append(GITHUB_REPO_ARGS);
            //     break;
            // case "filter-only-Medium":
            //     commandBuilder.append(" --scan --app=MyApp --cx-project=test").append(GITHUB_REPO_ARGS);
            //     break;
            default:
                throw new PendingException("Filter " + filter + " isn't supported");
        }

        String commandLine = commandBuilder.toString();
        tryRunCxFlow(commandLine);
    }

    @Then("bug tracker contains {word} issues")
    public void validateBugTrackerIssues(String numberOfIssues) {
        int expectedIssuesNumber;

        switch (numberOfIssues) {
            case "x+y+z":
                expectedIssuesNumber = expectedHigh + expectedMedium + expectedLow;
                break;
            case "x+y":
                expectedIssuesNumber = expectedHigh + expectedMedium;
                break;
            case "y":
                expectedIssuesNumber = expectedMedium;
                break;
            case "no":
                expectedIssuesNumber = 0;
                break;
            default:
                expectedIssuesNumber = Integer.parseInt(numberOfIssues);
        }

        int actualOfJiraIssues = jiraUtils.getNumberOfIssuesInProject(jiraProperties.getProject());
        Assert.assertEquals("Wrong issue count in bug tracker.", expectedIssuesNumber, actualOfJiraIssues);
    }

    @And("last scan for the project contains {int} findings")
    public void lastScanForProjectContainsFindings(int findingCount) {
        switch (findingCount) {
            case 0:
                customScaProjectName = "ci-2-to-0-findings-test";
                break;
            case 1:
                customScaProjectName = "ci-2-to-1-finding-test";
                break;
            case 5:
                customScaProjectName = "ci-2-to-5-findings-test";
                break;
            default:
                throw new IllegalArgumentException(String.format("The %d finding count is not supported.", findingCount));
        }
    }

    @Given("previous scan for a SCA project contains {int} findings")
    public void previousScanForASCAProjectContainsFindings(int count) {
        log.info("Assuming finding count: {} for the previous scan.", count);
    }

    @When("running CxFlow with `publish latest scan results` options")
    public void runningCxFlowWithPublishLatestScanResultsOptions() {
        String commandLine = String.format("--project --cx-project=%s --app=MyApp --blocksysexit", customScaProjectName);
        tryRunCxFlow(commandLine);
    }

    @Given("the {string} project doesn't exist in SCA")
    public void theProjectDoesnTExistInSCA(String projectName) {
        log.info("Assuming the '{}' project doesn't exist in SCA", projectName);
        customScaProjectName = projectName;
    }

    @Given("the {string} project exists in SCA but doesn't have any scans")
    public void theProjectExistsInSCAButDoesnTHaveAnyScans(String projectName) {
        log.info("Assuming the '{}' project has no scans", projectName);
        customScaProjectName = projectName;
    }

    @And("no exception is thrown")
    public void noExceptionIsThrown() {
        Assert.assertNull("Unexpected exception while running CxFlow", cxFlowExecutionException);
    }

    private void tryRunCxFlow(String commandLine) {
        try {
            TestUtils.runCxFlow(cxFlowRunner, commandLine);
        } catch (Throwable e) {
            log.info("Caught CxFlow execution exception: {}.", e.getClass().getSimpleName());
        }
    }

    private void initJiraBugTracker() throws IOException {
        log.info("Cleaning jira project before test: {}", jiraProperties.getProject());
        jiraUtils.ensureProjectExists(jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }
}