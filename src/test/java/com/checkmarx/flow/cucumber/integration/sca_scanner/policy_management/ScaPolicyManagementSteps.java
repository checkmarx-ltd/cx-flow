package com.checkmarx.flow.cucumber.integration.sca_scanner.policy_management;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.flow.service.ScaConfigurationOverrider;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.*;
import com.checkmarx.sdk.dto.sca.report.PolicyAction;
import com.checkmarx.sdk.utils.scanner.client.ScaClientHelper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class ScaPolicyManagementSteps extends ScaCommonSteps {

    private static final String PROJECT_NAME = "Policy-Management-Test-Project";
    private static final String GIT_REPO_URL = "https://github.com/cxflowtestuser/public-rest-repo.git";

    private final ScaProperties scaProperties;

    private ScaClientHelper scaClientHelper;
    private String projectId;
    private String policyId;
    private SCAResults scaResults;

    public ScaPolicyManagementSteps(FlowProperties flowProperties, SCAScanner scaScanner,
                                    ScaConfigurationOverrider scaConfigOverrider, ScaProperties scaProperties) {
        super(flowProperties, scaScanner, scaConfigOverrider);
        this.scaProperties = scaProperties;
        this.scaClientHelper = new ScaClientHelper(createRestClientConfig(scaProperties, PROJECT_NAME), log, scaProperties);
    }

    @Before("@SCA_Policy_Management")
    public void init() {
        initSCAConfig(scaProperties);
        scaClientHelper.init();
    }

    @After
    public void cleanUp() throws IOException {
        if (StringUtils.isNotEmpty(projectId)) {
            scaClientHelper.deleteProjectById(projectId);
        }

        if (StringUtils.isNotEmpty(policyId)) {
            scaClientHelper.deletePolicy(policyId);
        }
    }

    @Given("scanner is SCA")
    public void setScanner() {
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
    }

    @And("a violated policy is assigned to an existing project")
    public void assignPolicyToProject() throws IOException {
        projectId = scaClientHelper.createRiskManagementProject(PROJECT_NAME);

        PolicyAction policyAction = PolicyAction.builder()
                .breakBuild(true)
                .build();

        RuleCondition ruleCondition = RuleCondition.builder()
                .operator("Equal")
                .parameterValue(Collections.singletonList("High"))
                .parameter("VulnerabilitySeverity")
                .build();

        ConditionGroups conditionGroups = ConditionGroups.builder()
                .conditions(Collections.singletonList(ruleCondition))
                .build();

        PolicyRule policyRule = PolicyRule.builder()
                .name("No High Severity Rule")
                .conditionGroups(Collections.singletonList(conditionGroups))
                .build();

        Policy policy = Policy.builder()
                .name("No-High-Severity-Policy-Test")
                .rules(Collections.singletonList(policyRule))
                .projectIds(Collections.singletonList(projectId))
                .actions(policyAction)
                .build();

        policyId = scaClientHelper.createNewPolicy(policy);
    }

    @When("initiating a new scan")
    public void initNewScan() {
        ScanRequest scanRequest = getBasicScanRequest(PROJECT_NAME, GIT_REPO_URL);

        ScanResults scanResults = scaScanner.scan(scanRequest);
        scaResults = Objects.requireNonNull(scanResults).getScaResults();
        log.info("scaResults are : {}", scanResults);
    }

    @Then("isPolicyViolated flag in SCA results should be positive")
    public void validateIsPolicyViolatedFlag() {
        Assert.assertTrue("Expected policy to be marked as violated",
                scaResults.isPolicyViolated());
    }
}