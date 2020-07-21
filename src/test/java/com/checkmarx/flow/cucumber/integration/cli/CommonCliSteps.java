package com.checkmarx.flow.cucumber.integration.cli;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
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
public class CommonCliSteps {

    private final IntegrationTestContext testContext;
    private final FlowProperties flowProperties;
    private static final String GITHUB_REPO_ARGS = " --repo-url=https://github.com/cxflowtestuser/CLI-Integration-Tests --repo-name=CLI-Integration-Tests --github --branch=master --namespace=cxflowtestuser --blocksysexit";
    private static final String JIRA_PROJECT = "CIT";
    private String commandlineConstantArgs;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private IJiraTestUtils jiraUtils;

    @Before("@SAST_CLI_SCAN")
    public void beforeEachScenario() throws IOException {
        log.info("Setting bugTracker: Jira");
        flowProperties.setBugTracker("JIRA");
        log.info("Jira project key: {}", JIRA_PROJECT);
        jiraProperties.setProject(JIRA_PROJECT);
        initJiraBugTracker();
        testContext.reset();
    }

    @Given("repository is github")
    public void initGithubArguments(){
        commandlineConstantArgs = GITHUB_REPO_ARGS;
    }

    @Given("repository is github-sca")
    public void initGithubSCAArguments(){
        // Need to add the correct SCA repo
    }

    @After("@SAST_CLI_SCAN")
    public void afterEachScenario() {
        log.info("cleaning jira project: {}", jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }

    @When("running CxFlow scan with command line: {string}")
    public void runningCxFlowWithCommandLineCommandLine(String commandLine) {
        String fullCommandLine = commandLine + commandlineConstantArgs;
        log.info("Running CxFlow scan with command line: {}", fullCommandLine);
        Throwable exception = null;
        try {
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), fullCommandLine);
        } catch (Throwable e) {
            exception = e;
        }
        testContext.setCxFlowExecutionException(exception);
    }

    @Then("CxFlow exits with exit code {}")
    public void cxflowExitsWithStatusCodeStatusCode(int expectedExitCode) {

        Throwable exception = testContext.getCxFlowExecutionException();

        Assert.assertNotNull("Expected an exception to be thrown.", exception);
        Assert.assertEquals(InvocationTargetException.class, exception.getClass());

        Throwable targetException = ((InvocationTargetException) exception).getTargetException();
        Assert.assertTrue(targetException instanceof ExitThrowable);

        int actualExitCode = ((ExitThrowable) targetException).getExitCode();
        Assert.assertEquals(expectedExitCode, actualExitCode);
        log.info("CxFlow exited with expected exit code: {}", expectedExitCode);
    }

    @And("bugTracker contains {} issues")
    public void validateNumberOfissuesInBugTracker(int expectedNumOfIssues) {

        int numberOfJiraIssues = jiraUtils.getNumberOfIssuesInProject(jiraProperties.getProject());
        log.info("validating number of issues. expected: {}, numboer of issues in bug rtacker: {}", expectedNumOfIssues, numberOfJiraIssues);
        Assert.assertEquals(expectedNumOfIssues, numberOfJiraIssues);
    }

    private void initJiraBugTracker() throws IOException {

        log.info("Cleaning jira project before test: {}", jiraProperties.getProject());
        jiraUtils.ensureProjectExists(jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }


}