package com.checkmarx.flow.cucumber.integration.sast.config;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.service.SastScanner;
import com.checkmarx.flow.utils.github.GitHubTestUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.cx.CxScanParams;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@RequiredArgsConstructor
@Slf4j
@SpringBootTest(classes = {CxFlowApplication.class, GitHubTestUtils.class})
public class OverwritingProjectConfigSteps {
    public static final Duration SCAN_CREATION_TIMEOUT = Duration.ofSeconds(30);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    @SpyBean
    private final CxClient cxClientSpy;

    private final CxProperties cxProperties;
    private final FlowProperties flowProperties;
    private final GitHubProperties gitHubProperties;
    private final GitHubTestUtils gitHubTestUtils;
    private final HelperService helperService;
    private final GitHubService gitHubService;
    private final FlowService flowService;
    private final SastScanner sastScanner;

    private Integer projectId;
    private Integer interceptedScanId;

    // Name to ID mappings.
    private final Map<String, Integer> presetMapping = new HashMap<>();
    private final Map<String, Integer> configMapping = new HashMap<>();

    @Before()
    public void ensureCorrectTeam() {
        // Make sure we use the same team in the test and during scan (without the namespace postfix).
        // Otherwise we'll get 2 projects with the same name but in different teams.
        cxProperties.setMultiTenant(false);
    }

    @Given("{string} project exists in SAST")
    public void projectExistsInSAST(String projectName) throws CheckmarxException {
        this.projectId = ensureProjectExists(projectName);
        enableScanIdInterception();
    }

    @And("all of {string}, {string}, {string}, {string} exist in SAST")
    public void allOfExistInSAST(String initialPreset, String initialConfig, String globalPreset, String globalConfig) throws CheckmarxException {
        Integer initialPresetId = cxClientSpy.getPresetId(initialPreset);
        Integer globalPresetId = cxClientSpy.getPresetId(globalPreset);
        Integer initialConfigId = cxClientSpy.getScanConfiguration(initialConfig);
        Integer globalConfigId = cxClientSpy.getScanConfiguration(globalConfig);

        List<Integer> ids = Arrays.asList(initialPresetId, globalPresetId, initialConfigId, globalConfigId);
        boolean allEntitiesExist = ids.stream().allMatch(id -> id > 0);

        assertTrue(allEntitiesExist, () -> String.format("Some of the entities don't exist in SAST: %s.", ids));

        presetMapping.put(initialPreset, initialPresetId);
        presetMapping.put(globalPreset, globalPresetId);

        configMapping.put(initialConfig, initialConfigId);
        configMapping.put(globalConfig, globalConfigId);
    }

    @And("project has the {string} preset and the {string} scan configuration")
    public void projectPresetIs(String preset, String config) {
        cxClientSpy.createScanSetting(projectId, presetMapping.get(preset), configMapping.get(config));
    }

    @And("CxFlow config has the {string} preset and the {string} scan configuration")
    public void sastConfigurationIsSetToInCxFlowConfig(String globalPreset, String globalConfig) {
        cxProperties.setScanPreset(globalPreset);
        cxProperties.setConfiguration(globalConfig);
    }

    @And("GitHub repository does not contain a config-as-code file")
    public void githubRepositoryDoesNotContainAConfigAsCodeFile() {
        gitHubProperties.setConfigAsCode(null);
    }

    @When("GitHub notifies CxFlow about a pull request for the {string} project")
    public void githubNotifiesCxFlowAboutAPullRequest(String projectName) {

        GitHubController gitHubController = new GitHubController(gitHubProperties, flowProperties, cxProperties,
                null, flowService, helperService, gitHubService, sastScanner);

        gitHubTestUtils.callController(gitHubController, GitHubTestUtils.EventType.PULL_REQUEST, projectName);
    }

    @And("CxFlow starts a SAST scan")
    public void cxflowStartsASASTScan() {
        log.info("Waiting until scan is created.");
        Awaitility.await()
                .atMost(SCAN_CREATION_TIMEOUT)
                .until(() -> interceptedScanId != null);
        log.info("Scan is created, ID: {}.", interceptedScanId);
    }

    @Then("project preset is still {string} and scan configuration is {string}")
    public void projectPresetIsStill(String expectedPreset, String expectedConfigName) throws JsonProcessingException {
        String configJson = cxClientSpy.getScanSetting(projectId);
        JsonNode config = jsonMapper.readTree(configJson);

        int currentPresetId = config.at("/preset/id").asInt();
        assertEquals(presetMapping.get(expectedPreset), currentPresetId, "Preset has changed after starting a scan.");

        int currentConfigId = config.at("/engineConfiguration/id").asInt();
        assertEquals(configMapping.get(expectedConfigName), currentConfigId, "Configuration has changed after starting a scan.");
    }

    private void enableScanIdInterception() throws CheckmarxException {
        doAnswer(invocation -> {
            interceptedScanId = (Integer) invocation.callRealMethod();
            return interceptedScanId;
        }).when(cxClientSpy).createScan(any(CxScanParams.class), anyString());
    }

    private Integer ensureProjectExists(String projectName) throws CheckmarxException {
        Integer projectId;
        String teamId = cxClientSpy.getTeamId(cxProperties.getTeam());
        projectId = cxClientSpy.getProjectId(teamId, projectName);
        if (projectId <= 0) {
            projectId = cxClientSpy.createProject(teamId, projectName);
        }
        return projectId;
    }
}
