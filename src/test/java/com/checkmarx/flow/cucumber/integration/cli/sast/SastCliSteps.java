package com.checkmarx.flow.cucumber.integration.cli.sast;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.config.properties.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.cli.IntegrationTestContext;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.scanner.CxClient;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@Slf4j
@RequiredArgsConstructor
public class SastCliSteps {

    private static final String GITHUB_REPO_ARGS = " --repo-url=https://github.com/cxflowtestuser/CLI-Integration-Tests --repo-name=CLI-Integration-Tests --github --branch=master --blocksysexit ";
    private static final String STANDARD_CLI_SCAN = " --scan  --scanner=sast  --app=MyApp";
    private static final String NONE__BUG_TRACKER_TEST_PROJECT_NAME =  "Test-bugTracker-None";
    private static final String PROJECT_NAME_ARG =  "--cx-project=" + NONE__BUG_TRACKER_TEST_PROJECT_NAME;
    private static final String JIRA_PROJECT = "CIT";

    private final FlowProperties flowProperties;
    private final JiraProperties jiraProperties;
    private final IntegrationTestContext testContext;
    private final ScaProperties scaProperties;
    private final CxProperties cxProperties;
    private String commandlineConstantArgs;
    private int expectedHigh;
    private int expectedMedium;
    private int expectedLow;
    private final CxClient cxService;

    @Autowired
    private IJiraTestUtils jiraUtils;

    @Before("@SAST_CLI_SCAN")
    public void beforeEachScenario() throws IOException {
        log.info("Setting bugTracker: Jira");
        flowProperties.setBugTracker("JIRA");
        flowProperties.setBreakBuild(true);
        resetThresholds();

        log.info("Jira project key: {}", JIRA_PROJECT);
        jiraProperties.setProject(JIRA_PROJECT);
        initJiraBugTracker();
    }

    @After()
    public void afterEachScenario() {
        log.info("Cleaning JIRA project: {}", jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }

    @Given("repository is github-sast")
    public void setRepo() {
        commandlineConstantArgs = GITHUB_REPO_ARGS;
    }

    @When("running with break-build on {word}")
    public void runningWithBreakBuild(String issueType) {
        StringBuilder commandBuilder = new StringBuilder();


        switch (issueType) {
            case "success":
                commandBuilder.append("--scan  --severity=High --cwe=1 --app=MyApp").append(commandlineConstantArgs);
                break;
            case "missing-mandatory-parameter":
                commandBuilder.append("--severity=High --severity=Medium").append(commandlineConstantArgs);
                break;
            case "error-processing-request":
                commandBuilder.append("--scan  --severity=High --severity=Medium").append(commandlineConstantArgs);
                break;

            default:
                throw new PendingException("Issues type " + issueType + " isn't supported");
        }

        log.info("Running CxFlow scan with command line: {}", commandBuilder.toString());
        Throwable exception = null;
        try {
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), commandBuilder.toString());
        } catch (Throwable e) {
            exception = e;
        }
        testContext.setCxFlowExecutionException(exception);
    }

    @When("bug tracker is set to 'None'")
    public void setNoneBugTracker() {
        flowProperties.setBugTracker(BugTracker.Type.NONE.toString());
    }

    @Then("running cxflow to execute SAST scan")
    public void runCxFlowScan() {
        Throwable exception = null;
        try {
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), STANDARD_CLI_SCAN + GITHUB_REPO_ARGS + PROJECT_NAME_ARG);
        } catch (Throwable e) {
            exception = e;
        }
        testContext.setCxFlowExecutionException(exception);
    }

    @Then("cxflow should not wait for scan results")
    public void validateScanNotFinished() throws CheckmarxException {

        String teamId = cxProperties.getTeam();
        String ownerId = cxService.getTeamId(teamId);
        Integer projectId = cxService.getProjectId(ownerId, NONE__BUG_TRACKER_TEST_PROJECT_NAME);

        log.info("Checking if there is any existing scan for Project:");
        Integer existingScanId = cxService.getScanIdOfExistingScanIfExists(projectId);

        log.info("found scanId {}", existingScanId);
        Assert.assertNotEquals(-1, (long)existingScanId);
    }

    @Then("cxflow should exit with exit code: {}")
    public void checkReturnCode(int expectedExitCode) {
        Throwable exception = testContext.getCxFlowExecutionException();

        Assert.assertNotNull("Expected an exception to be thrown.", exception);
        Assert.assertEquals(InvocationTargetException.class, exception.getClass());

        Throwable targetException = ((InvocationTargetException) exception).getTargetException();
        Assert.assertTrue(targetException instanceof ExitThrowable);

        int actualExitCode = ((ExitThrowable) targetException).getExitCode();

        Assert.assertEquals("The expected exist code did not match",
                expectedExitCode, actualExitCode);
    }


    @Given("code has x High, y Medium and z low issues")
    public void setIssues() {
        expectedHigh = 2;
        expectedMedium = 4;
        expectedLow = 7;
    }

    @When("running sast scan {word}")
    public void runnningScanWithFilter(String filter) {
        StringBuilder commandBuilder = new StringBuilder();

        switch (filter) {
            case "no-filter":
                commandBuilder.append(" --scan  --scanner=sast --severity=High --severity=Medium --severity=Low --app=MyApp").append(commandlineConstantArgs);
                break;
            case "filter-High-and-Medium":
                commandBuilder.append(" --scan  --scanner=sast --severity=High --severity=Medium --app=MyApp").append(commandlineConstantArgs);
                break;
            case "filter-only-Medium":
                commandBuilder.append(" --scan  --scanner=sast --severity=Medium --app=MyApp").append(commandlineConstantArgs);
                break;
            case "filter-invalid-cwe":
                commandBuilder.append(" --scan  --scanner=sast --cwe=1 --app=MyApp").append(commandlineConstantArgs);
                break;
            default:
                throw new PendingException("Filter " + filter + " isn't supported");
        }

        Throwable exception = null;
        try {
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), commandBuilder.toString());
        } catch (Throwable e) {
            exception = e;
        }
        testContext.setCxFlowExecutionException(exception);
    }

    @Then("bugTracker contains {} issues")
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
            case "0":
                expectedIssuesNumber = 0;
                break;

            default:
                throw new PendingException("Number of issues parameter " + numberOfIssues + " isn't supported");
        }

        int actualOfJiraIssues = jiraUtils.getNumberOfIssuesInProject(jiraProperties.getProject());
        Assert.assertEquals(expectedIssuesNumber, actualOfJiraIssues);
    }

    private void initJiraBugTracker() throws IOException {
        log.info("Cleaning jira project before test: {}", jiraProperties.getProject());
        jiraUtils.ensureProjectExists(jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }

    private void resetThresholds() {
        flowProperties.setThresholds(null);
        scaProperties.setThresholdsSeverity(null);
        scaProperties.setThresholdsScore(null);
    }

    @And("and enabledZipScan is set to {string}")
    public void andEnabledZipScanIsSetTo(String enabledZipScan) {
         if(enabledZipScan.equals("true")){
             cxProperties.setEnabledZipScan(true);
         }else if(enabledZipScan.equals("false")){
             cxProperties.setEnabledZipScan(false);
         }
    }
}