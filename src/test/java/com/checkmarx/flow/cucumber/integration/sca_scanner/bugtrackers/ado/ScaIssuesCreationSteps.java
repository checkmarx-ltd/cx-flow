package com.checkmarx.flow.cucumber.integration.sca_scanner.bugtrackers.ado;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.common.utils.JsonUtils;
import com.checkmarx.flow.cucumber.integration.azure.publishing.AzureDevopsClient;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.ResultsService;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@SpringBootTest(classes = {CxFlowApplication.class, AzureDevopsClient.class})
@RequiredArgsConstructor
public class ScaIssuesCreationSteps {

    private static final String INPUT_BASE_PATH = "cucumber/data/sample-sca-results/";
    private final static String NAMESPACE = "CxNamespace";
    private final static String PROJECT_NAME = "CxScaTest";
    private static final String AZURE = "Azure";

    private final FlowProperties flowProperties;
    private final ADOProperties adoProperties;
    private final AzureDevopsClient azureDevopsClient;
    private final ResultsService resultsService;

    @Before("@SCA_Issues_Creation")
    public void init() throws IOException {
        azureDevopsClient.init(NAMESPACE, PROJECT_NAME);
        azureDevopsClient.deleteProjectIssues();
    }

    @After("@SCA_Issues_Creation")
    public void deleteProjectIssues() throws IOException {
        azureDevopsClient.deleteProjectIssues();
    }

    @Given("scan initiator is SCA")
    public void setScanInitiator() {
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
    }

    @And("bug tracker is Azure")
    public void setBugTracker() {
        flowProperties.setBugTracker(AZURE);
        flowProperties.setBugTrackerImpl(Collections.singletonList(AZURE));
        adoProperties.setUrl("https://dev.azure.com/");
        adoProperties.setProjectName(PROJECT_NAME);
        adoProperties.setNamespace(NAMESPACE);
    }

    @When("publishing new known unfiltered SCA results from {string}")
    public void publishNewScaResults(String inputUrl) throws IOException, MachinaException {
        ScanResults scanResults = JsonUtils.json2Object(getFileFromResourcePath(INPUT_BASE_PATH + inputUrl), ScanResults.class);
        resultsService.processResults(getBasicScanRequest(), scanResults, null);
    }

    @Then("new {int} tickets should be created")
    public void ticketsCreationValidation(int expectedTicketsCreation) throws IOException {
        Assert.assertEquals("Azure new SCA results tickets number is not as expected",
                expectedTicketsCreation, azureDevopsClient.getIssueCount());
    }

    private ScanRequest getBasicScanRequest() {
        return ScanRequest.builder()
                .product(ScanRequest.Product.CX)
                .project(PROJECT_NAME)
                .namespace("cxflowtestuser")
                .repoName("htmx")
                .repoType(ScanRequest.Repository.GITHUB)
                .branch("master")
                .bugTracker(getAzureCustomBugTracker())
                .refs(Constants.CX_BRANCH_PREFIX.concat("refs/heads/master"))
                .build();
    }

    private File getFileFromResourcePath(String path) throws IOException {
        return new ClassPathResource(path).getFile();
    }

    private BugTracker getAzureCustomBugTracker() {
        return BugTracker.builder()
                .type(BugTracker.Type.CUSTOM)
                .customBean("Azure")
                .build();
    }
}