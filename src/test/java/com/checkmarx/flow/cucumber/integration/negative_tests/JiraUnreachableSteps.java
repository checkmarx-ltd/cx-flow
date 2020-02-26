package com.checkmarx.flow.cucumber.integration.negative_tests;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.JiraClientRunTimeException;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.JiraService;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

@SpringBootTest(classes = { CxFlowApplication.class })
public class JiraUnreachableSteps {

    private static final String INVALID_URL = "https://cxflow-jira-not-accessible.atlassian.net/";
    private static final String FINDING_PATH = "cucumber/data/sample-sast-results/1-finding.xml";
    private static final String PROJECT_KEY = "AT1";

    private ScanRequest basicScanRequest;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private FlowProperties flowProperties;

    @Autowired
    private CxProperties cxProperties;

    @Autowired
    private FlowService flowService;

    @Autowired
    private JiraService jiraService;

    private BugTracker bugTracker;

    @Before("@JiraIntegrationTests")
    public void init() {
        cxProperties.setOffline(true);
    }

    @Given("target is JIRA")
    public void setTargetToJira() {
        bugTracker = getBasicBugTrackerToJira();
        flowProperties.setBugTracker(bugTracker.getType().name());
    }

    @And("JIRA is configured with invalid URL")
    public void setUrlToInvalid() {
        jiraProperties.setUrl(INVALID_URL);
        jiraService.init();
    }

    @When("preparing a getIssues call to deliver")
    public void preparingScanRequest() {
        basicScanRequest = getBasicScanRequest();
    }

    @Then("the call execution should throw a JiraClientRunTimeException since JIRA is un-accessible")
    public void verifyExceptionWhenJiraIsUnreachable() {
        Assertions.assertThrows(JiraClientRunTimeException.class,
                () -> flowService.cxParseResults(basicScanRequest, getFileFromResourcePath()),
                "Expected to get Jira un-accessible exception error");
    }

    private File getFileFromResourcePath() throws IOException {
        return new ClassPathResource(JiraUnreachableSteps.FINDING_PATH).getFile();
    }

    private ScanRequest getBasicScanRequest() {
        return ScanRequest.builder()
                .application("TestApp")
                .product(ScanRequest.Product.CX)
                .project("TestProject")
                .team("CxServer")
                .namespace("Test")
                .repoName("TestRepo")
                .repoUrl("http://localhost/repo.git")
                .repoUrlWithAuth("http://localhost/repo.git")
                .repoType(ScanRequest.Repository.GITHUB)
                .bugTracker(bugTracker)
                .branch("master")
                .refs(Constants.CX_BRANCH_PREFIX.concat("master"))
                .email(null)
                .incremental(false)
                .scanPreset("Checkmarx Default")
                .build();
    }

    private BugTracker getBasicBugTrackerToJira() {
        return BugTracker.builder()
                .issueType(jiraProperties.getIssueType())
                .projectKey(PROJECT_KEY)
                .type(BugTracker.Type.JIRA)
                .issueType("Bug")
                .build();

    }
}