package com.checkmarx.flow.cucumber.component.batch;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.config.*;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.component.scan.ScanFixture;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { CxFlowMocksConfig.class, CxFlowApplication.class })
public class BatchComponentSteps {
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final GitHubProperties gitHubProperties;
    private final GitLabProperties gitLabProperties;
    private final ADOProperties adoProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final CxClient cxClient;
    private final List<ThreadPoolTaskExecutor> executors;
    private CxFlowRunner cxFlowRunner;
    private String projectName;
    private String teamName;

    public BatchComponentSteps(FlowProperties flowProperties, CxProperties cxProperties, JiraProperties jiraProperties, GitHubProperties gitHubProperties, GitLabProperties gitLabProperties,
                               ADOProperties adoProperties, FlowService flowService, HelperService helperService, CxClient cxClient, List<ThreadPoolTaskExecutor> executors) {
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.gitHubProperties = gitHubProperties;
        this.gitLabProperties = gitLabProperties;
        this.adoProperties = adoProperties;
        this.flowService = flowService;
        this.helperService = helperService;
        this.cxClient = cxClient;
        this.executors = executors;
    }

    @Given("SAST client is mocked - to allow tests to pass without active SAST environment")
    public void sastClientIsMocked() throws CheckmarxException {
        when(cxClient.getTeamId(anyString())).thenReturn(ScanFixture.TEAM_ID);
        when(cxClient.getProjects(anyString())).thenReturn(ScanFixture.getProjects());
        when(cxClient.getReportContentByScanId(ScanFixture.SCAN_ID, ScanFixture.getScanFilters())).thenReturn(ScanFixture.getScanResults());
        cxFlowRunner = new CxFlowRunner(flowProperties, cxProperties, jiraProperties, gitHubProperties, gitLabProperties, adoProperties, flowService, helperService, executors);
    }

    @Given("project is provided: {string} and team: {string}")
    public void projectIsProvidedProjectNameAndTeamTeamName(String projectName, String teamName) {
        this.projectName = projectName;
        this.teamName = teamName;
    }

    @When("scan project in batch mode")
    public void scanProjectInBatchMode() throws Exception {
        TestUtils.runCxFlow(cxFlowRunner,"--blocksysexit --batch --project --cx-project=" + this.projectName + " --cx-team=" + this.teamName);
    }

    @Then("Json file is created in path: {string}")
    public void jsonFileIsCreatedInPath(String path) {
        boolean exists = Files.exists(Paths.get(path));
        assertTrue(exists);
    }
}
