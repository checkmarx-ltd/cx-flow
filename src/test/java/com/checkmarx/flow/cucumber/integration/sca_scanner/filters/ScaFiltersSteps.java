package com.checkmarx.flow.cucumber.integration.sca_scanner.filters;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.flow.service.ScaConfigurationOverrider;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.SCAResults;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class ScaFiltersSteps extends ScaCommonSteps {

    private static final String PROJECT_NAME = "Filters-Tests-Repo";
    private static final String GIT_REPO_URL = "https://github.com/cxflowtestuser/public-rest-repo.git";

    private SCAResults scaResults;
    private final ScaProperties scaProperties;

    public ScaFiltersSteps(FlowProperties flowProperties,
                           ScaProperties scaProperties,
                           SCAScanner scaScanner,
                           ScaConfigurationOverrider scaConfigOverrider) {
        super(flowProperties, scaScanner, scaConfigOverrider);
        this.scaProperties = scaProperties;
    }

    @Before("@SCA_Filtering")
    public void init() {
        initSCAConfig(scaProperties);
    }

    @Given("scan initiator is SCA")
    public void setScanInitiator() {
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
    }

    @And("SCA filter severity is enabled with {string} filter")
    public void setScaSeverityFilter(String severityFilterList) {
        if (severityFilterList.equals("")) {
            scaProperties.setFilterSeverity(null);
        } else {
            List<String> filterSeverity = createFiltersListFromString(severityFilterList);
            scaProperties.setFilterSeverity(filterSeverity);
        }
    }

    @And("Sca filter score is enabled with {double} filter")
    public void setScaScoreFilter(double scoreFilter) {
        scaProperties.setFilterScore(scoreFilter);
    }

    @When("SCA runs a new scan on Filters-Tests-Repo which contains 8 vulnerabilities results")
    public void scanResults() {
        // scanRequest must be created after all the changes in scaProperties are done.
        ScanRequest scanRequest = getBasicScanRequest(PROJECT_NAME, GIT_REPO_URL);

        ScanResults scanResults = scaScanner.scan(scanRequest);
        scaResults = Objects.requireNonNull(scanResults).getScaResults();
    }

    @Then("the expected number of sanitized vulnerabilities are {int}")
    public void validateFilteredResults(int expectedResults) {
        Assert.assertEquals(expectedResults, scaResults.getFindings().size());
    }

}