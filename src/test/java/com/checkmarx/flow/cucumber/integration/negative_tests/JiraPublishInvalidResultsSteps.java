package com.checkmarx.flow.cucumber.integration.negative_tests;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.JiraClientRunTimeException;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.JiraService;
import com.checkmarx.flow.service.SastScanner;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

public class JiraPublishInvalidResultsSteps {

    private static final String JIRA_URL = "https://cxflow.atlassian.net/";
    private static final String FINDING_PATH = "cucumber/data/sample-sast-results/2-findings-different-vuln-type-same-file.xml";
    private static final String INVALID_PROJECT_KEY = "INVALID-PROJECT-KEY";

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

    @Autowired
    private SastScanner sastScanner;

    private BugTracker bugTracker;

    @Before("@JiraIntegrationTests")
    public void init() {
        cxProperties.setOffline(true);
    }


    @And("Cx-Flow is configured with invalid project key")
    public void setInvalidProjectKey() {
        jiraProperties.setUrl(JIRA_URL);
        jiraService.init();

        bugTracker = getBasicBugTrackerToJira();
        flowProperties.setBugTracker(bugTracker.getType().name());
        bugTracker.setProjectKey(INVALID_PROJECT_KEY);
    }

    @When("preparing results to deliver")
    public void prepareResultsToDeliver() {
        basicScanRequest = getBasicScanRequest();
    }

    @Then("the call execution should throw a JiraClientRunTimeException since an error occurred when published new tickets")
    public void verifyExceptionWhenPublishInvalidResults() {
        Assertions.assertThrows(JiraClientRunTimeException.class,
                () -> sastScanner.cxParseResults(basicScanRequest, getFileFromResourcePath()),
                "Expected to get Jira un-published tickets exception error");
    }

    private File getFileFromResourcePath() throws IOException {
        return new ClassPathResource(JiraPublishInvalidResultsSteps.FINDING_PATH).getFile();
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
                .type(BugTracker.Type.JIRA)
                .issueType("Bug")
                .build();

    }
}