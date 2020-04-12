package com.checkmarx.flow.cucumber.integration.azure.publishing.githubflow;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.cucumber.integration.azure.publishing.AzureDevopsClient;
import com.checkmarx.flow.cucumber.integration.azure.publishing.Utils;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest(classes = {CxFlowApplication.class})
@RequiredArgsConstructor
public class PublishingSteps {
    private final FlowProperties flowProperties;
    private final AzureDevopsClient adoClient;
    private final GitHubController controller;
    private String projectName;

    @Before
    public void init() throws IOException {
        projectName = Utils.getProjectName();
        adoClient.ensureProjectExists(projectName);
    }

    @Given("issue tracker is ADO")
    public void issueTrackerIsADO() {
        flowProperties.setBugTracker(Utils.ISSUE_TRACKER_NAME);
    }

    @And("ADO doesn't contain any issues")
    public void adoDoesnTContainAnyIssues() throws IOException {
        adoClient.deleteProjectIssues(projectName);
    }

    @When("GitHub notifies CxFlow about a {string}")
    public void githubNotifiesCxFlowAboutA(String eventName) {
    }

    @And("SAST scan returns a report with {int} finding")
    public void sastScanReturnsAReportWithFinding(int arg0) {
    }

    @And("CxFlow publishes the report")
    public void cxflowPublishesTheReport() {
    }

    @Then("ADO contains {int} issue")
    public void adoContainsIssue(int arg0) {
    }
}
