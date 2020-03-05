package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.cucumber.common.Constants;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.sdk.config.CxProperties;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SpringBootTest(classes = {CxFlowApplication.class})
public class PublishingSteps {
    private static final String PROPERTIES_FILE_PATH = "cucumber/features/integrationTests/azure/publishing.properties";
    private static final String DEFAULT_BRANCH = "master";

    @Autowired
    private FlowService flowService;

    @Autowired
    private ADOProperties adoProperties;

    @Autowired
    private CxProperties cxProperties;

    private AzureDevopsClient adoClient;
    private String projectName;
    private String sastReportFilename;

    @Before
    public void prepareEnvironment() throws IOException {
        Properties testProperties = TestUtils.getPropertiesFromResource(PROPERTIES_FILE_PATH);
        projectName = testProperties.getProperty("projectName");

        cxProperties.setOffline(true);

        adoClient = new AzureDevopsClient(adoProperties);
        adoClient.ensureProjectExists(projectName);
        adoClient.deleteProjectIssues(projectName);
    }

    @Given("Azure DevOps doesn't contain any issues")
    public void azureDevOpsDoesnTContainAnyIssues() throws IOException {
        verifyIssueCount(0);
    }

    @And("SAST report contains {int} findings with the same vulnerability type and in the same file, and not marked as false positive")
    public void sastReportContainsFindingCount(int findingCount) {
        switch (findingCount) {
            case 0:
                sastReportFilename = "empty-results.xml";
                break;
            case 1:
                sastReportFilename = "1-finding.xml";
                break;
            case 2:
                sastReportFilename = "2-findings-same-vuln-type-same-file.xml";
                break;
            default:
                fail("Unexpected finding count: " + findingCount);
                break;
        }
    }

    @When("publishing the report")
    public void publishingTheReport() throws IOException, ExitThrowable {
        BugTracker bugTracker = BugTracker.builder()
                .type(BugTracker.Type.CUSTOM)
                .customBean("Azure")
                .build();

        ScanRequest request = ScanRequest.builder()
                .bugTracker(bugTracker)
                .namespace(projectName)
                .repoName(projectName)
                .branch(DEFAULT_BRANCH)
                .product(ScanRequest.Product.CX)
                .build();

        String path = Paths.get(Constants.SAMPLE_SAST_RESULTS_DIR, sastReportFilename).toString();
        File sastReport = TestUtils.getFileFromResource(path);

        flowService.cxParseResults(request, sastReport);
    }

    @Then("Azure DevOps contains {int} issues")
    public void azureDevOpsContainsIssueCountIssues(int expectedCount) throws IOException {
        verifyIssueCount(expectedCount);
    }

    private void verifyIssueCount(int expectedCount) throws IOException {
        int actualCount = adoClient.getIssueCount(projectName);
        assertEquals("Incorrect number of issues.", expectedCount, actualCount);
    }
}
