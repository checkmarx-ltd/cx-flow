package com.checkmarx.flow.cucumber.component.batch;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.config.*;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.component.scan.ScanFixture;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.CxGoProperties;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.scanner.CxClient;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {CxFlowMocksConfig.class, CxFlowApplication.class})
@RequiredArgsConstructor
public class BatchComponentSteps {
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final CxGoProperties cxgoProperties;
    private final JiraProperties jiraProperties;
    private final GitHubProperties gitHubProperties;
    private final IastService iastService;
    private final GitLabProperties gitLabProperties;
    private final ADOProperties adoProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final CxClient cxClient;
    private final List<ThreadPoolTaskExecutor> executors;
    private final ResultsService resultsService;
    private final OsaScannerService osaScannerService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;
    private final ThresholdValidator thresholdValidator;
    private final BuildProperties buildProperties;

    private CxFlowRunner cxFlowRunner;
    private String projectName;
    private String teamName;
    private final List<VulnerabilityScanner> scanners;

    @Given("SAST client is mocked - to allow tests to pass without active SAST environment")
    public void sastClientIsMocked() throws CheckmarxException {
        when(cxClient.getTeamId(anyString())).thenReturn(ScanFixture.TEAM_ID);
        when(cxClient.getProjects(anyString())).thenReturn(ScanFixture.getProjects());

        FilterConfiguration filter = FilterConfiguration.fromSimpleFilters(ScanFixture.getScanFilters());
        when(cxClient.getReportContentByScanId(ScanFixture.SCAN_ID, filter))
                .thenReturn(ScanFixture.getScanResults());

        CxScannerService cxScannerService = new CxScannerService(cxProperties,null, null, null, null );
        
        cxFlowRunner = new CxFlowRunner(flowProperties,
                cxScannerService,
                jiraProperties,
                gitHubProperties,
                gitLabProperties,
                iastService,
                adoProperties,
                helperService,
                executors,
                resultsService,
                osaScannerService,
                filterFactory,
                configOverrider,
                buildProperties,
                scanners,
                thresholdValidator);
    }

    @Given("project is provided: {string} and team: {string}")
    public void projectIsProvidedProjectNameAndTeamTeamName(String projectName, String teamName) {
        this.projectName = projectName;
        this.teamName = teamName;
    }

    @When("scan project in batch mode")
    public void scanProjectInBatchMode() throws Exception {
        TestUtils.runCxFlow(cxFlowRunner, "--" + CxFlowRunner.THROW_INSTEAD_OF_EXIT_OPTION +
                " --batch --project --cx-project=" + this.projectName +
                " --cx-team=" + this.teamName);
    }

    @Then("Json file is created in path: {string}")
    public void jsonFileIsCreatedInPath(String path) {
        boolean exists = Files.exists(Paths.get(path));
        assertTrue(exists);
    }
}
