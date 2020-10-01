package com.checkmarx.flow.cucumber.integration.cli.sca;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.ScaProperties;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * This step implementation relies on specific projects that already exist in SCA (see customScaProjectName).
 */
@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@Slf4j
@RequiredArgsConstructor
public class ScaCliSteps {
    private static final String REPO_ARGS = " --repo-url=https://github.com/cxflowtestuser/public-rest-repo --repo-name=CLI-public-rest-repo --branch=master --blocksysexit";
    private static final String GITHUB_REPO_ARGS = REPO_ARGS + " --github ";
    private static final String JIRA_PROJECT = "SCIT";
    private static final String DIRECTORY_TO_SCAN = "input-code-for-sca";
    private static final String NO_FILTERS = "none";
    private static final int AT_LEAST_ONE = Integer.MAX_VALUE;

    private final FlowProperties flowProperties;
    private final JiraProperties jiraProperties;
    private final ScaProperties scaProperties;

    private final CxFlowRunner cxFlowRunner;
    private Throwable cxFlowExecutionException;
    private String customScaProjectName;
    private Path directoryToScan;

    @Autowired
    private IJiraTestUtils jiraUtils;

    @Before
    public void beforeEachScenario() throws IOException {
        log.info("Setting bugTracker: Jira");
        flowProperties.setBugTracker("JIRA");
        ScaCommonSteps.initSCAConfig(scaProperties);

        List<String> scaOnly = Collections.singletonList(ScaProperties.CONFIG_PREFIX);
        flowProperties.setEnabledVulnerabilityScanners(scaOnly);

        log.info("Jira project key: {}", JIRA_PROJECT);
        jiraProperties.setProject(JIRA_PROJECT);
        initJiraBugTracker();
        log.info("reset sca filters");
        scaProperties.setFilterSeverity(Collections.emptyList());
        resetThresholds();
    }

    @After
    public void afterEachScenario() throws IOException {
        log.info("Cleaning JIRA project: {}", jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());

        if (directoryToScan != null) {
            log.info("Deleting temp directory: {}", directoryToScan);
            PathUtils.deleteDirectory(directoryToScan);
        }
    }

    @Given("source directory contains vulnerable files")
    public void sourceDirectoryContainsVulnerableFiles() throws IOException {
        directoryToScan = getTempDir();
        copyTestProjectTo(directoryToScan);
    }

    @Given("code has 6 High, 11 Medium and 1 low issues")
    public void setProject() {
        customScaProjectName = "test";
    }

    @When("running a SCA scan with break-build on {}")
    public void runningWithBreakBuild(String issueType) {
        StringBuilder commandBuilder = new StringBuilder();
        setFilters("High");
        switch (issueType) {
            case "success":
                commandBuilder.append("--scan  --severity=High --app=MyApp --cx-project=test").append(GITHUB_REPO_ARGS);
                break;
            case "missing-mandatory-parameter":
                commandBuilder.append(GITHUB_REPO_ARGS);
                break;
            case "error-processing-request":
                commandBuilder.append("--scan --app=MyApp").append(GITHUB_REPO_ARGS);
                break;
            case "missing-project":
                commandBuilder.append("--scan --app=MyApp --f=nofile").append(REPO_ARGS);
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

    @Then("run should exit with exit code {}")
    public void validateExitCode(int expectedExitCode) {
        Assert.assertNotNull("Expected an exception to be thrown.", cxFlowExecutionException);
        Assert.assertEquals(InvocationTargetException.class, cxFlowExecutionException.getClass());

        Throwable targetException = ((InvocationTargetException) cxFlowExecutionException).getTargetException();
        Assert.assertTrue(targetException instanceof ExitThrowable);

        int actualExitCode = ((ExitThrowable) targetException).getExitCode();

        Assert.assertEquals("The expected exit code did not match", expectedExitCode, actualExitCode);
    }

    @Given("last scan for a project {string} contains 49 High, 3 Medium and 1 Low-severity findings")
    public void setProjectWithFindings(String projectName){
        customScaProjectName = projectName;
    }

    @When("running sca scan {word}")
    public void runnningScanWithFilter(String filters) {
        StringBuilder commandLine = new StringBuilder();
        commandLine.append(" --scan --app=MyApp --cx-project=test").append(GITHUB_REPO_ARGS);

        setFilters(filters);

        tryRunCxFlow(commandLine.toString());
    }

    @Then("bug tracker contains {} issues")
    public void validateBugTrackerIssues(String description) {
        int expectedIssueCount = getExpectedIssueCount(description);
        int actualIssueCount = jiraUtils.getNumberOfIssuesInProject(jiraProperties.getProject());

        log.info("comparing expected number of issues: {}, to actual bug tracker issues; {}", expectedIssueCount, actualIssueCount);
        if (expectedIssueCount == AT_LEAST_ONE) {
            Assert.assertTrue("Expected at least one issue in bug tracker.", actualIssueCount > 0);
        } else {
            Assert.assertEquals("Wrong issue count in bug tracker.", expectedIssueCount, actualIssueCount);
        }
    }

    @And("last scan for the project contains {} findings")
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
    public void runCxFlowWithPublishLatestScanResultsOptions() {
        String commandLine = String.format("--project --cx-project=%s --app=MyApp --blocksysexit", customScaProjectName);
        tryRunCxFlow(commandLine);
    }

    @When("run CxFlow with `publish latest scan results` options and {}")
    public void runCxFlowGetLatestProjectResultsWithFilters(String filters){
        setFilters(filters);
        runCxFlowWithPublishLatestScanResultsOptions();
    }

    private void setFilters(String filters){
        String[] filtersList = filters.split(",");
        if (!filters.equals(NO_FILTERS)){
            scaProperties.setFilterSeverity(Arrays.asList(filtersList));
        }
        else {
            scaProperties.setFilterSeverity(Collections.emptyList());
        }
    }

    @When("running CxFlow with `scan local sources` options")
    public void runningCxFlowWithScanLocalSourcesOptions() {
        customScaProjectName = "ci-local-scan-test";

        String commandLine = String.format("--scan --cx-project=%s --app=MyApp --f=%s --blocksysexit",
                customScaProjectName,
                directoryToScan);

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

    private int getExpectedIssueCount(String countDescription) {
        int result;
        switch (countDescription) {
            case "some":
                result = AT_LEAST_ONE;
                break;
            case "no":
                result = 0;
                break;
            default:
                result = Integer.parseInt(countDescription);
        }

        return result;
    }

    private static Path getTempDir() {
        String systemTempDir = FileUtils.getTempDirectoryPath();
        String subdir = String.format("sca-cli-test-%s", UUID.randomUUID());
        return Paths.get(systemTempDir, subdir);
    }

    private void copyTestProjectTo(Path targetDir) throws IOException {
        log.info("Copying test project files from resources ({}) into a temp directory: {}", DIRECTORY_TO_SCAN, targetDir);
        File directory = TestUtils.getFileFromResource(DIRECTORY_TO_SCAN);
        FileUtils.copyDirectory(directory, targetDir.toFile());
    }

    private void resetThresholds() {
        flowProperties.setThresholds(null);
        scaProperties.setThresholdsSeverity(null);
        scaProperties.setThresholdsScore(null);
    }
}