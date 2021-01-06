package com.checkmarx.flow.cucumber.integration.sca_scanner.zip_scan;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.flow.service.ScaConfigurationOverrider;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.SCAResults;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.Objects;

@SpringBootTest(classes = {CxFlowApplication.class})
public class ScaZipScanSteps extends ScaCommonSteps {

    private static final String PROJECT_NAME = "SCA-Zip-Scan";
    private static final String GIT_REPO_URL = "https://github.com/cxflowtestuser/public-rest-repo.git";

    private final ScaProperties scaProperties;

    private SCAResults scaResults;

    public ScaZipScanSteps(FlowProperties flowProperties, SCAScanner scaScanner,
                           ScaConfigurationOverrider scaConfigOverrider, ScaProperties scaProperties) {
        super(flowProperties, scaScanner, scaConfigOverrider);
        this.scaProperties = scaProperties;
    }

    @Before("@SCA_Zip_Scan")
    public void init() {
        initSCAConfig(scaProperties);
    }

    @Given("scanner is SCA")
    public void setScanner() {
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
    }

    @And("enabledZipScan property is set with true")
    public void setEnabledZipScanProperty() {
        scaProperties.setEnabledZipScan(true);
    }

    @When("initiating a new scan")
    public void initScan() {
        ScanRequest scanRequest = getBasicScanRequest(PROJECT_NAME, GIT_REPO_URL);

        ScanResults scanResults = scaScanner.scan(scanRequest);
        scaResults = Objects.requireNonNull(scanResults).getScaResults();
    }

    @Then("returned scan high and medium results are bigger than zero")
    public void validateResults() {
        Assert.assertTrue("Expected scan total high results to be a positive number", getTotalHighFindings(scaResults) > 0);
        Assert.assertTrue("Expected scan total medium results to be a positive number", getTotalMediumFindings(scaResults) > 0);
    }

}