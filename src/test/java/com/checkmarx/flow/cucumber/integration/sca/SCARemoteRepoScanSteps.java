package com.checkmarx.flow.cucumber.integration.sca;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.sca.SCAParams;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.service.ScaClient;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

@SpringBootTest(classes = {CxFlowApplication.class})
public class SCARemoteRepoScanSteps {

    private static final String APP_URL = "https://sca.scacheckmarx.com";
    private static final String API_URL = "https://api.scacheckmarx.com";
    private static final String AC_URL = "https://v2.ac-checkmarx.com";
    private static final String PROJECT_NAME = "Test Project";

    @Autowired
    private FlowProperties flowProperties;

    @Autowired
    private ScaProperties scaProperties;

    @Autowired
    private ScaClient scaClient;

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
            remoteRepoUrl = "https://e7d5332437f66d6249a8f1825a6079da10156aa2@github.com/cxflowtestuser/EndToEndTests.git";
        }
    }

    @When("scan is finished")
    public void startScan() throws IOException {
        SCAParams scaParams = getScaParams();
        scaResults = scaClient.scanRemoteRepo(scaParams);
    }

    @Then("the returned results are not null")
    public void validateResults() {
        Assert.assertNotNull("Expected scan-Id of remote repo scan not to return null", scaResults.getScanId());
        Assert.assertNotNull("Expected findings counts of remote repo scan not to return null", scaResults.getFindingCounts());
        Assert.assertNotNull("Expected report link of remote repo scan not to return null", scaResults.getWebReportLink());
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