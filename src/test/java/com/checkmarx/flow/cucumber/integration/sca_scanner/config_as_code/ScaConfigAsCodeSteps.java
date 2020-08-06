package com.checkmarx.flow.cucumber.integration.sca_scanner.config_as_code;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.ConfigurationOverrider;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.CxConfig;
import io.cucumber.java.Before;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;

@SpringBootTest(classes = {CxFlowApplication.class})
@RequiredArgsConstructor
public class ScaConfigAsCodeSteps {

    private final GitHubProperties gitHubProperties;
    private final GitHubService gitHubService;
    private final ConfigurationOverrider configOverrider;

    private ScanRequest scanRequest;

    @Before("@Sca_cx_config")
    public void init() {
        setGitHubProperties();
    }

    @Given("cx-flow has a scan request")
    public void getScanRequest() {
        scanRequest = initCxConfigScanRequest();
    }

    @When("target repo contains a configuration file")
    public void getRepoCxConfig() {
        CxConfig cxConfigOverride = gitHubService.getCxConfigOverride(scanRequest);
        configOverrider.overrideScanRequestProperties(cxConfigOverride, scanRequest);
    }

    @Then("cx-flow configurations properties are getting overridden with the following parameters:")
    public void validatePropertiesAreOverridden(List<String> listOfParameters) {
        listOfParameters.forEach(currentParameter -> {
            String failMsg;
            Object expected;
            Object actual;

            switch (currentParameter) {
                case "vulnerabilityScanners":
                    failMsg = "vulnerability scanners from configuration as code are not as expected";
                    Assert.assertTrue(failMsg, scanRequest.getVulnerabilityScanners().toString().contains("SCAScanner"));
                    break;
                case "thresholdsSeverity":
                    failMsg = "SCA thresholds severity from configuration as code is not as expected";
                    expected = "{HIGH=10, MEDIUM=6, LOW=3}";
                    actual = StringUtils.join(scanRequest.getScaConfig().getThresholdsSeverity());
                    Assert.assertEquals(failMsg , expected , actual);
                    break;
                case "thresholdsScore":
                    failMsg = "SCA thresholds score from configuration as code is not as expected";
                    expected = new Double(8.5);
                    actual = scanRequest.getScaConfig().getThresholdsScore();
                    Assert.assertEquals(failMsg , expected , actual);
                    break;
                case "filterSeverity":
                    failMsg = "SCA filter severity from configuration as code is not as expected";
                    expected = "[high, medium, low]";
                    actual = scanRequest.getScaConfig().getFilterSeverity().toString();
                    Assert.assertEquals(failMsg , expected , actual);
                    break;
                case "filterScore":
                    failMsg = "SCA filter score from configuration as code is not as expected";
                    expected = new Double(7.5);
                    actual = scanRequest.getScaConfig().getFilterScore();
                    Assert.assertEquals(failMsg , expected , actual);
                    break;
                default:
                    throw new PendingException("The support for this parameter is not yet implemented: " + currentParameter);
            }
        });
    }

    private void setGitHubProperties() {
        gitHubProperties.setConfigAsCode("cx.config");
        gitHubProperties.setApiUrl("https://api.github.com/repos");
    }

    private ScanRequest initCxConfigScanRequest() {
        return ScanRequest.builder()
                .namespace("cxflowtestuser")
                .repoName("SCA-CxConfig-Tests")
                .branch("master")
                .build();
    }
}