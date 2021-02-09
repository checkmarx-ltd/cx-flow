package com.checkmarx.flow.cucumber.integration.sca_scanner.teams;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.flow.service.ScaConfigurationOverrider;
import com.checkmarx.sdk.config.RestClientConfig;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.RemoteRepositoryInfo;
import com.checkmarx.sdk.dto.sca.ScaConfig;
import com.checkmarx.sdk.dto.sca.Project;
import com.checkmarx.sdk.exception.CxHTTPClientException;
import com.checkmarx.sdk.utils.scanner.client.ScaClientHelper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.rmi.Remote;
import java.util.Collections;
import java.util.List;

@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class SCATeamsSteps extends ScaCommonSteps {

    private static final String PROJECT_NAME = "Team-Test-Project";
    private final ScaProperties scaProperties;

    private ScaClientHelper scaClientHelper;
    private String projectId;
    private int errorStatusCode;

    public SCATeamsSteps(FlowProperties flowProperties, SCAScanner scaScanner,
                         ScaConfigurationOverrider scaConfigOverrider, ScaProperties scaProperties) {
        super(flowProperties, scaScanner, scaConfigOverrider);
        this.scaProperties = scaProperties;
        this.scaClientHelper = new ScaClientHelper(createRestClientConfig(scaProperties, PROJECT_NAME), log, scaProperties);
    }

    @After()
    public void cleanUp() throws IOException {
        if (StringUtils.isNotEmpty(projectId)) {
            scaClientHelper.deleteProjectById(projectId);
        }
    }

    @Before()
    public void init() throws IOException {
        initSCAConfig(scaProperties);
        scaClientHelper.init();
    }

    @Given("scanner is SCA")
    public void setScanner() {
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
    }

    @When("creating a new project with associated {string} value")
    public void createNewProjectTeam(String team) throws IOException {
        if (team.equals("null")) {
            team = null;
        }
        scaProperties.setTeamForNewProjects(team);
        try {
            projectId = scaClientHelper.createRiskManagementProject(PROJECT_NAME);
        } catch (CxHTTPClientException e) {
            errorStatusCode = e.getStatusCode();
        }
    }

    @Then("project assignedTeams returned value is {string}")
    public void validateAssignedTeam(String expectedAssignedTeam) throws IOException {
        String actualTeam;
        Project projectDetails = scaClientHelper.getProjectDetailsByProjectId(projectId);
        List<String> assignedTeams = projectDetails.getAssignedTeams();

        if (assignedTeams.isEmpty()) {
            actualTeam = "";
        } else {
            actualTeam = assignedTeams.get(0);
        }

        Assert.assertEquals(expectedAssignedTeam, actualTeam);
    }

    @Then("bad request error is expected to be thrown")
    public void validateNotExistsTeam() {
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, errorStatusCode);
    }

}