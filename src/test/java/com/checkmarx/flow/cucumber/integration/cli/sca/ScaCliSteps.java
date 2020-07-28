package com.checkmarx.flow.cucumber.integration.cli.sca;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.cli.IntegrationTestContext;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.ScaProperties;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.PendingException;
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
import java.util.Collections;

@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@Slf4j
@RequiredArgsConstructor
public class ScaCliSteps {
    private static final String REPO_ARGS = " --repo-url=https://github.com/cxflowtestuser/public-rest-repo --repo-name=CLI-public-rest-repo --branch=master --blocksysexit";
    private static final String GITHUB_REPO_ARGS = REPO_ARGS.intern() + " --github ";
    private static final String JIRA_PROJECT = "SCIT";

    private final FlowProperties flowProperties;
    private final JiraProperties jiraProperties;
    private final ScaProperties scaProperties;
    private final IntegrationTestContext testContext;

    private String commandlineConstantArgs;
    private int expectedHigh;
    private int expectedMedium;
    private int expectedLow;

    @Autowired
    private IJiraTestUtils jiraUtils;

    @Before("@SCA_CLI_SCAN")
    public void beforeEachScenario() throws IOException {
        log.info("Setting bugTracker: Jira");
        flowProperties.setBugTracker("JIRA");
        initSCAConfig();
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));

        log.info("Jira project key: {}", JIRA_PROJECT);
        jiraProperties.setProject(JIRA_PROJECT);
        initJiraBugTracker();
    }

    @After()
    public void afterEachScenario() {
        log.info("Cleaning JIRA project: {}", jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }

    @Given("repository is github-sca")
    public void setRepo() {
        commandlineConstantArgs = GITHUB_REPO_ARGS;
    }

    @When("running with break-build on {word}")
    public void runningWithBreakBuild(String issueType) {
        StringBuilder commandBuilder = new StringBuilder();

        switch (issueType) {
            case "success":
                commandBuilder.append("--scan  --severity=High --app=MyApp --cx-project=test").append(commandlineConstantArgs);
                break;
            case "missing-mandatory-parameter":
                commandBuilder.append("--severity=High --severity=Medium").append(commandlineConstantArgs);
                break;
            case "error-processing-request":
                commandBuilder.append("--scan  --severity=High --app=MyApp").append(commandlineConstantArgs);
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
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), commandBuilder.toString());
        } catch (Throwable e) {
            exception = e;
        }
        testContext.setCxFlowExecutionException(exception);
    }

    @Then("run should exit with exit code {int}")
    public void validateExitCode(int expectedExitCode) {
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
        expectedHigh = 6;
        expectedMedium = 9;
        expectedLow = 0;
    }

    @When("running sca scan {word}")
    public void runnningScanWithFilter(String filter) {
        StringBuilder commandBuilder = new StringBuilder();

        switch (filter) {
            case "no-filter":
                commandBuilder.append(" --scan  --scanner=sca --app=MyApp --cx-project=test").append(commandlineConstantArgs);
                break;
            // case "filter-High-and-Medium":
            //     commandBuilder.append(" --scan  --scanner=sca --app=MyApp --cx-project=test").append(commandlineConstantArgs);
            //     break;
            // case "filter-only-Medium":
            //     commandBuilder.append(" --scan  --scanner=sca --app=MyApp --cx-project=test").append(commandlineConstantArgs);
            //     break;
            default:
                throw new PendingException("Filter " + filter + " isn't supported");
        }

        try {
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), commandBuilder.toString());
        } catch (Throwable e) {
        }
    }

    @Then("bugTracker contains {word} issues")
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

    protected void initSCAConfig() {
        scaProperties.setAppUrl("https://sca.scacheckmarx.com");
        scaProperties.setApiUrl("https://api.scacheckmarx.com");
        scaProperties.setAccessControlUrl("https://platform.checkmarx.net");
    }
}