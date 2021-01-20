package com.checkmarx.flow.cucumber.component.cxintegrations;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.CxIntegrationsProperties;
import com.checkmarx.flow.config.external.CxGoConfigFromWebService;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.ConfigurationOverrider;
import com.checkmarx.flow.service.ReposManagerService;
import com.checkmarx.sdk.dto.CxConfig;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {CxFlowApplication.class})
@RequiredArgsConstructor
public class CxIntegrationSteps {

    private static final String CLIENT_SECRET = "clientSecret-Test";
    private static final String TEAM = "team-Test";
    private static final String SCM_ACCESS_TOKEN = "scmAccessToken-Test";

    @MockBean
    private final ReposManagerService reposManagerService;

    private final CxIntegrationsProperties cxIntegrationsProperties;
    private final ConfigurationOverrider configOverrider;
    private ScanRequest scanRequest;

    @Before("@Cx-integrations")
    public void init() {
        setMocks();
        scanRequest = initScanRequest();
    }

    @Given("read-multi-tenant-configuration flag is set to true")
    public void setMultiTenantConf() {
        cxIntegrationsProperties.setReadMultiTenantConfiguration(true);
    }

    @When("cx-flow getting a new event")
    public void simulateNewRequestEvent() {
        scanRequest = configOverrider.overrideScanRequestProperties(new CxConfig(), this.scanRequest);
    }

    @Then("scanRequest is getting populated with cx-go new configuration")
    public void validateCxGoConfigurationOverride() {
        Assert.assertEquals(CLIENT_SECRET, scanRequest.getScannerApiSec());
        Assert.assertEquals(TEAM, scanRequest.getTeam());
        Assert.assertTrue(scanRequest.getRepoUrlWithAuth().contains(SCM_ACCESS_TOKEN));
    }

    private void setMocks() {
        when(reposManagerService.getCxGoDynamicConfig(anyString(), anyString())).thenReturn(getMockedCxGoConfig());
    }

    private CxGoConfigFromWebService getMockedCxGoConfig() {
        return CxGoConfigFromWebService.builder()
                .cxgoToken(CLIENT_SECRET)
                .scmAccessToken(SCM_ACCESS_TOKEN)
                .team(TEAM)
                .build();
    }

    private ScanRequest initScanRequest() {
        return ScanRequest.builder()
                .repoType(ScanRequest.Repository.GITHUB)
                .organizationId("organization-test")
                .gitUrl("https://www.github.com")
                .build();
    }
}