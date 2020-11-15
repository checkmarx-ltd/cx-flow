package com.checkmarx.flow.cucumber.integration.sca_scanner.scans;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.cucumber.common.JsonLoggerTestUtils;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.dto.OperationStatus;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.AnalyticsReport;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.flow.service.ScaConfigurationOverrider;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ast.SCAResults;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.Objects;

import static org.junit.Assert.*;

@CucumberContextConfiguration
@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class SCARemoteRepoScanSteps extends ScaCommonSteps {

    private static final String PUBLIC_PROJECT_NAME = "Public-Test-Test-Repo";
    private static final String PRIVATE_PROJECT_NAME = "Private-Test-Test-Repo";
    private static final String PUBLIC_REPO = "https://github.com/cxflowtestuser/public-rest-repo.git";
    private static final String PRIVATE_REPO = "https://%s@github.com/cxflowtestuser/TestAlgorithms-.git";

    private final GitHubProperties gitHubProperties;

    private ScanRequest scanRequest;
    private SCAResults scaResults;
    private ScaProperties scaProperties;

    public SCARemoteRepoScanSteps(FlowProperties flowProperties, ScaProperties scaProperties,
                                  SCAScanner scaScanner, GitHubProperties gitHubProperties,
                                  ScaConfigurationOverrider scaConfigOverrider) {
        super(flowProperties, scaScanner, scaConfigOverrider);
        this.gitHubProperties = gitHubProperties;
        this.scaProperties = scaProperties;
    }

    @Before("@SCARemoteRepoScan")
    public void init() {
        initSCAConfig(scaProperties);
    }

    @Given("scan initiator is SCA")
    public void setScanInitiator() {
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
    }

    @And("scan is configured as a {string} GIT remote repository source")
    public void setScanSourceAsPublicRemoteRepo(String repoVisibilityType) {
        if (repoVisibilityType.equals("public")) {
            scanRequest = getBasicScanRequest(PUBLIC_PROJECT_NAME, PUBLIC_REPO);
        } else if (repoVisibilityType.equals("private")){
            String token = gitHubProperties.getToken();
            String remoteRepoWithAuth = String.format(PRIVATE_REPO, token);
            scanRequest = getBasicScanRequest(PRIVATE_PROJECT_NAME, remoteRepoWithAuth);
        }
    }

    @And("scan is configured using invalid GIT remote repository source {string}")
    public void invalidRepo(String invalidRepoUrl){
        scanRequest = getBasicScanRequest(PUBLIC_PROJECT_NAME, invalidRepoUrl);
    }
    @Then("the returned results are not null")
    public void validateResults() {
        assertNotNull("SCA results are null.", scaResults);
        assertTrue("Scan ID is empty.", StringUtils.isNotEmpty(scaResults.getScanId()));
        assertNotNull("Summary is null.", scaResults.getSummary());
        assertNotNull("Finding counts are null.", scaResults.getSummary().getFindingCounts());
        assertNotNull("Expected report link of remote repo scan not to return null", scaResults.getWebReportLink());

        assertNotNull("Finding list is null.", scaResults.getFindings());
        assertFalse("Finding list is empty.", scaResults.getFindings().isEmpty());

        assertNotNull("Package list is null.", scaResults.getPackages());
        assertFalse("Package list is empty.", scaResults.getPackages().isEmpty());
    }

    @When("scan is finished")
    public void startScan() {
        scaResults = Objects.requireNonNull(scaScanner.scan(scanRequest)).getScaResults();
    }

    @And("SCA scan report entry is created in Json Logger with corresponding error message")
    public void validateLoggerError() {
        
        try {

            scaScanner.scan(scanRequest);
        } 
        //exception is expected
        catch (Exception e1) {
            
            try{
                ScanReport report = getReportObject();
                assertEquals(AnalyticsReport.SCA, report.getScanInitiator());
                assertEquals(OperationStatus.FAILURE, report.getScanResult().getStatus());
                return;
            } catch (CheckmarxException | JsonProcessingException e) {
                fail(e.getMessage());
            }
        }
        fail("no exception is thrown when invalid GIT url is supplied");
    }

    @And("SCA scan report entry is created in Json Logger")
    public void validateLogger() {


        try {

            ScanReport report = getReportObject();
            assertEquals(AnalyticsReport.SCA, report.getScanInitiator());
            assertEquals(scaResults.getScanId(), report.getScanId());
            assertEquals(OperationStatus.SUCCESS, report.getScanResult().getStatus());

        } catch (CheckmarxException | JsonProcessingException e) {
            fail(e.getMessage());
        }
    }

    private ScanReport getReportObject() throws CheckmarxException, JsonProcessingException {
        JsonLoggerTestUtils utils = new JsonLoggerTestUtils();

        String lastLine = utils.getLastLine();
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode jsonNode = objectMapper.readTree(lastLine).get(ScanReport.OPERATION);
        if (jsonNode != null) {
            return (ScanReport)utils.getAnalyticsReport(ScanReport.class, objectMapper, jsonNode);
        }else{
            return null;
        }
    }

  
}