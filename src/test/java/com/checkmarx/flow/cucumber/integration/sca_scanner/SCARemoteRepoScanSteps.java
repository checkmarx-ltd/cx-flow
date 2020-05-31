package com.checkmarx.flow.cucumber.integration.sca_scanner;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.sca.SCAResults;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.Objects;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SpringBootTest(classes = {CxFlowApplication.class})
@RequiredArgsConstructor
@Slf4j
public class SCARemoteRepoScanSteps {

    private static final String APP_URL = "https://sca.scacheckmarx.com";
    private static final String API_URL = "https://api.scacheckmarx.com";
    private static final String AC_URL = "https://v2.ac-checkmarx.com";

    private static final String PUBLIC_PROJECT_NAME = "Public-Test-Test-Repo";
    private static final String PRIVATE_PROJECT_NAME = "Private-Test-Test-Repo";

    private static final String PUBLIC_REPO = "https://github.com/checkmarx-ltd/cx-flow.git";
    private static final String PRIVATE_REPO = "https://%s@github.com/cxflowtestuser/TestAlgorithms-.git";

    private final FlowProperties flowProperties;
    private final ScaProperties scaProperties;
    private final GitHubProperties gitHubProperties;
    private final SCAScanner scaScanner;

    private ScanRequest scanRequest;
    private SCAResults scaResults;

    @Before("@SCARemoteRepoScan")
    public void init() {
        initSCAConfig();

        String sca_username = System.getenv("SCA_USERNAME");
        if (sca_username != null) {
            log.info("SCA username env variable has been found with value: {}", sca_username);
        } else {
            log.info("SCA username env variable wasn't been found");
        }

        String sca_password = System.getenv("SCA_PASSWORD");
        if (sca_password != null) {
            log.info("SCA password env variable has been found with value: {}", sca_password);
        } else {
            log.info("SCA password env variable wasn't been found");
        }

        String sca_tenant = System.getenv("SCA_TENANT");
        if (sca_tenant != null) {
            log.info("SCA tenant env variable has been found with value: {}", sca_tenant);
        } else {
            log.info("SCA tenant env variable wasn't been found");
        }
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

    private void initSCAConfig() {
        scaProperties.setAppUrl(APP_URL);
        scaProperties.setApiUrl(API_URL);
        scaProperties.setAccessControlUrl(AC_URL);
    }


    private ScanRequest getBasicScanRequest(String projectName, String repoWithAuth) {
        return ScanRequest.builder()
                .project(projectName)
                .repoUrlWithAuth(repoWithAuth)
                .build();
    }
}