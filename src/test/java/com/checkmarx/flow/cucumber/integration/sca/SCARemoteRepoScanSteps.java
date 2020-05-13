package com.checkmarx.flow.cucumber.integration.sca;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.sca.SCAParams;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.service.ScaClient;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import static org.junit.Assert.*;

@SpringBootTest(classes = {CxFlowApplication.class})
@RequiredArgsConstructor
public class SCARemoteRepoScanSteps {

    private static final String APP_URL = "https://sca.scacheckmarx.com";
    private static final String API_URL = "https://api.scacheckmarx.com";
    private static final String AC_URL = "https://v2.ac-checkmarx.com";
    private static final String PROJECT_NAME = "Test Project";

    private final FlowProperties flowProperties;
    private final ScaProperties scaProperties;
    private final ScaClient scaClient;
    private final GitHubProperties gitHubProperties;

    private String remoteRepoUrl;
    private SCAResults scaResults;

    @Before("@SCARemoteRepoScan")
    public void init() {
        initSCAConfig();
    }

    @Given("scan initiator is SCA")
    public void setScanInitiator() {
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
    }

    @And("scan is configured as a {string} GIT remote repository source")
    public void setScanSourceAsPublicRemoteRepo(String repoVisibilityType) {
        if (repoVisibilityType.equals("public")) {
            remoteRepoUrl = "https://github.com/checkmarx-ltd/cx-flow.git";
        } else if (repoVisibilityType.equals("private")){
            String token = gitHubProperties.getToken();
            remoteRepoUrl = "https://" + token + "@github.com/cxflowtestuser/TestAlgorithms-.git";
        }
    }

    @When("scan is finished")
    public void startScan() throws IOException {
        SCAParams scaParams = getScaParams();
        scaResults = scaClient.scanRemoteRepo(scaParams);
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

    private void initSCAConfig() {
        scaProperties.setAppUrl(APP_URL);
        scaProperties.setApiUrl(API_URL);
        scaProperties.setAccessControlUrl(AC_URL);
    }

    private SCAParams getScaParams() throws MalformedURLException {
        return SCAParams.builder()
                .projectName(PROJECT_NAME)
                .remoteRepoUrl(new URL(remoteRepoUrl))
                .build();
    }
}