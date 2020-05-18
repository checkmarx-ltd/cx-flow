package com.checkmarx.flow.cucumber.integration.sast.config;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.github.GitHubTestUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
@Slf4j
@SpringBootTest(classes = { CxFlowApplication.class, GitHubTestUtils.class})
public class OverwritingProjectConfigSteps {
    private final CxClient cxClient;
    private final CxProperties cxProperties;
    private final GitHubTestUtils gitHubTestUtils;

    private final FlowProperties flowProperties;
    private final GitHubProperties gitHubProperties;
    private final HelperService helperService;
    private final GitHubService gitHubService;
    private final FlowService flowService;

    private Integer initialPresetId;
    private Integer initialConfigId;
    private Integer newPresetId;
    private Integer newConfigId;
    private Integer projectId;

    @Given("{string} project exists in SAST")
    public void projectExistsInSAST(String projectName) throws CheckmarxException {
        this.projectId = ensureProjectExists(projectName);
    }

    @And("all of {string}, {string}, {string}, {string} exist in SAST")
    public void allOfExistInSAST(String initialPreset, String initialConfig, String newPreset, String newConfig) throws CheckmarxException {
        initialPresetId = cxClient.getPresetId(initialPreset);
        initialConfigId = cxClient.getScanConfiguration(initialConfig);
        newPresetId = cxClient.getPresetId(newPreset);
        newConfigId = cxClient.getScanConfiguration(newConfig);

        List<Integer> ids = Arrays.asList(initialPresetId, initialConfigId, newPresetId, newConfigId);
        boolean allEntitiesExist = ids.stream().allMatch(id -> id > 0);

        assertTrue(allEntitiesExist, () -> String.format("Some of the entities don't exist in SAST: %s.", ids));
    }

    @And("project preset is {string} and scan configuration is {string}")
    public void projectPresetIs(String preset, String config) {
        cxClient.createScanSetting(projectId, initialPresetId, initialConfigId);
    }

    private Integer ensureProjectExists(String projectName) throws CheckmarxException {
        Integer projectId;
        String teamId = cxClient.getTeamId(cxProperties.getTeam());
        projectId = cxClient.getProjectId(teamId, projectName);
        if (projectId <= 0) {
            projectId = cxClient.createProject(teamId, projectName);
        }
        return projectId;
    }

    @And("SAST configuration is set to {string} in CxFlow config")
    public void sastConfigurationIsSetToInCxFlowConfig(String globalConfig) {
        cxProperties.setConfiguration(globalConfig);
    }

    @And("SAST preset is set to {string} in CxFlow config")
    public void sastPresetIsSetToInCxFlowConfig(String globalPreset) {
        cxProperties.setScanPreset(globalPreset);
    }

    @When("GitHub notifies CxFlow about a pull request without overriding the 'preset' parameter")
    public void githubNotifiesCxFlowAboutAPullRequest() {
        GitHubController gitHubController = new GitHubController(gitHubProperties, flowProperties, cxProperties,
                null, flowService, helperService, gitHubService);

        gitHubTestUtils.callController(gitHubController, GitHubTestUtils.EventType.PULL_REQUEST);
    }

    @And("GitHub repository does not contain a config-as-code file")
    public void githubRepositoryDoesNotContainAConfigAsCodeFile() {
    }

    @And("CxFlow starts a SAST scan")
    public void cxflowStartsASASTScan() {
    }

    @And("project preset is still {string} and scan configuration is {string}")
    public void projectPresetIsStill(String expectedPresetName, String expectedConfigName) {
        Integer currentPresetId = cxClient.getProjectPresetId(projectId);
        assertEquals(initialPresetId, currentPresetId, "Preset has changed after starting a scan.");

        String configJson = cxClient.getScanSetting(projectId);
        assertEquals(expectedConfigName, configJson);
    }
}
