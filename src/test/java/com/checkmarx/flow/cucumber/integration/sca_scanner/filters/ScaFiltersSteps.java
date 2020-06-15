package com.checkmarx.flow.cucumber.integration.sca_scanner.filters;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.sdk.config.ScaProperties;
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
    private static final String GIT_REPO_URL = "https://github.com/cxflowtestuser/htmx.git";

    private ScanRequest scanRequest;
    private SCAResults scaResults;

    public ScaFiltersSteps(FlowProperties flowProperties, ScaProperties scaProperties, SCAScanner scaScanner) {
        super(flowProperties, scaProperties, scaScanner);
    }

    @Before("@SCA_Filtering")
    public void init() {
        initSCAConfig();
        scanRequest = getBasicScanRequest(PROJECT_NAME, GIT_REPO_URL);
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

    @When("SCA detects multiple vulnerabilities results")
    public void scanResults() {
        scaResults = Objects.requireNonNull(scaScanner.scan(scanRequest)).getScaResults();
    }

    @Then("the expected number of sanitized vulnerabilities are {int}")
    public void validateFilteredResults(int expectedResults) {
        Assert.assertEquals(expectedResults, scaResults.getFindings().size());
    }

}