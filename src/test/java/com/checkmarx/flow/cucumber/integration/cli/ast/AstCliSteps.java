package com.checkmarx.flow.cucumber.integration.cli.ast;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.service.ASTScanner;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.AstProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.sast.Filter;
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
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@Slf4j
@RequiredArgsConstructor
public class AstCliSteps  {

    private static final String REPO_ARGS = " --repo-url=https://github.com/cxflowtestuser/testsAST.git --repo-name=CLI-public-rest-repo --branch=master --blocksysexit";

    private static final String GITHUB_REPO_ARGS = REPO_ARGS + " --github ";
    private static final String JIRA_PROJECT = "ASTCLITEST";
    private static final String DIRECTORY_TO_SCAN = "input-code-for-sca";
    private static final String NO_FILTERS = "none";
    private static final int AT_LEAST_ONE = Integer.MAX_VALUE;

    private final FlowProperties flowProperties;
    private final JiraProperties jiraProperties;
    private final ScaProperties scaProperties;
    private final AstProperties astProperties;
    private final CxFlowRunner cxFlowRunner;
    private Throwable cxFlowExecutionException;
    private String customAstProjectName;
    private Path directoryToScan;

    private static final String SEPARATOR = ",";

    @Autowired
    private IJiraTestUtils jiraUtils;

    @Before
    public void beforeEachScenario() throws IOException {
        log.info("Setting bugTracker: Jira");
        flowProperties.setBugTracker("JIRA");
        ScaCommonSteps.initSCAConfig(scaProperties);

        log.info("Jira project key: {}", JIRA_PROJECT);
        jiraProperties.setProject(JIRA_PROJECT);
        initJiraBugTracker();
        log.info("reset sca filters");
        scaProperties.setFilterSeverity(Collections.emptyList());

        astProperties.setPreset("Checkmarx Default");
        astProperties.setIncremental("false");
    }

    @After
    public void afterEachScenario() throws IOException {
        try {
            log.info("Cleaning JIRA project: {}", jiraProperties.getProject());
            jiraUtils.cleanProject(jiraProperties.getProject());

            if (directoryToScan != null) {
                log.info("Deleting temp directory: {}", directoryToScan);
                PathUtils.deleteDirectory(directoryToScan);
            }
        }catch(Exception e){
            log.warn("AstCliSteps.afterEachScenario(): Failed to clean issues: " + e.getMessage());
        }
    }

    @Given("scanner is {}")
    public void setScanInitiator(String initiatorList) {

        List<String> scanners = Arrays.asList(initiatorList.toLowerCase().split(SEPARATOR));
        flowProperties.setEnabledVulnerabilityScanners(scanners);

    }
    @And("source directory contains vulnerable files")
    public void sourceDirectoryContainsVulnerableFiles() throws IOException {
        directoryToScan = getTempDir();
        copyTestProjectTo(directoryToScan);
    }


    @When("running a AST scan with break-build on {}")
    public void runningWithBreakBuild(String issueType) {
        StringBuilder commandBuilder = new StringBuilder();
        setScanInitiator("AST");
        
        switch (issueType) {
            case "success":
                commandBuilder.append("--scan --app=MyApp --cx-project=test").append(GITHUB_REPO_ARGS);
                break;
            case "missing-mandatory-parameter":
                commandBuilder.append(GITHUB_REPO_ARGS);
                break;
            case "missing-project":
                commandBuilder.append("--scan --app=MyApp --f=nofile").append(REPO_ARGS);
                break;
            default:
                throw new PendingException("Issues type " + issueType + " isn't supported");
        }

        log.info("Running CxFlow scan with command line: {}", commandBuilder.toString());
  
        try {
            TestUtils.runCxFlow(cxFlowRunner, commandBuilder.toString());
            cxFlowExecutionException = null;
        } catch (Throwable e) {
            cxFlowExecutionException = e;
        }

        
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
    

    @When("repository is github")
    public void runnningScan() {
        
        customAstProjectName = "test";
        
        StringBuilder commandLine = new StringBuilder();
        commandLine.append(" --scan --app=MyApp --cx-project=test").append(GITHUB_REPO_ARGS);

        tryRunCxFlow(commandLine.toString());
    }

    @Then("bug tracker contains {} issues")
    public void validateBugTrackerIssues(int expectedIssuesCount) {

        int actualIssueCount = jiraUtils.getNumberOfIssuesInProject(jiraProperties.getProject());
        Iterable<Issue> actualIssues = jiraUtils.searchForAllIssues(jiraProperties.getProject()).getIssues();

        actualIssues.forEach(issue -> {
            
            if(flowProperties.getEnabledVulnerabilityScanners().size() ==1 && flowProperties.getEnabledVulnerabilityScanners().get(0).equalsIgnoreCase("AST")) {
                
                //validate state
                Assert.assertTrue(issue.getDescription().contains("TO_VERIFY"));

                //validate description
                if (issue.getDescription().contains("Cross_Site_History_Manipulation")) {
                    Assert.assertTrue(issue.getDescription().contains("Method @SourceMethod at line @SourceLine of @SourceFile may leak server-side conditional values, enabling user tracking from another website. This may constitute a Privacy Violation."));
                }
//                if (issue.getDescription().contains("Hardcoded_password_in_Connection_String")) {
//                    Assert.assertTrue(issue.getDescription().contains("The application contains hardcoded connection details, @SourceElement, at line @SourceLine of @SourceFile. This connection string contains a hardcoded password, which is used in @DestinationMethod at line @DestinationLine of @DestinationFile to connect to a database server with @DestinationElement. This can expose the database password, and impede proper password management."));
//                }
            }
        });

        log.info("comparing expected number of issues: {}, to actual bug tracker issues; {}", expectedIssuesCount, actualIssueCount);
        Assert.assertEquals("Wrong issue count in bug tracker.", expectedIssuesCount, actualIssueCount);
        
    }


    @When("running CxFlow with `scan local sources` options")
    public void runningCxFlowWithScanLocalSourcesOptions() {
        customAstProjectName = "ci-local-scan-test";

        String commandLine = String.format("--scan --cx-project=%s --app=MyApp --f=%s --blocksysexit",
                customAstProjectName,
                directoryToScan);

        tryRunCxFlow(commandLine);
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

   

    private static Path getTempDir() {
        String systemTempDir = FileUtils.getTempDirectoryPath();
        String subdir = String.format("ast-cli-test-%s", UUID.randomUUID());
        return Paths.get(systemTempDir, subdir);
    }

    private void copyTestProjectTo(Path targetDir) throws IOException {
        log.info("Copying test project files from resources ({}) into a temp directory: {}", DIRECTORY_TO_SCAN, targetDir);
        File directory = TestUtils.getFileFromResource(DIRECTORY_TO_SCAN);
        FileUtils.copyDirectory(directory, targetDir.toFile());
    }
}