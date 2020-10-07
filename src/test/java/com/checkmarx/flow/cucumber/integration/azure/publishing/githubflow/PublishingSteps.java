package com.checkmarx.flow.cucumber.integration.azure.publishing.githubflow;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.cucumber.integration.azure.publishing.AzureDevopsClient;
import com.checkmarx.flow.cucumber.integration.azure.publishing.PublishingStepsBase;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.github.GitHubTestUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * The following occurs here:
 * - create a CxClient mock that always returns 1 finding
 * - create an instance of GitHub controller
 * - call pull or push method on the controller instance
 * - poll ADO API until it has 1 issue (or fail after a timeout).
 */
@Slf4j
@SpringBootTest(classes = {CxFlowApplication.class, GitHubTestUtils.class, AzureDevopsClient.class})
@RequiredArgsConstructor
public class PublishingSteps extends PublishingStepsBase {
    private final FlowProperties flowProperties;
    private final AzureDevopsClient adoClient;
    private final GitHubTestUtils testUtils;
    private final GitHubProperties gitHubProperties;
    private final CxProperties cxProperties;
    private final HelperService helperService;
    private final GitHubService gitHubService;
    private final GitHubAppAuthService gitHubAppAuthService;
    private final ResultsService resultsService;
    private final VulnerabilityScanner sastScanner;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;
    private final ScmConfigOverrider scmConfigOverrider;

    @MockBean
    private final CxClient cxClientMock;

    @MockBean
    private final ProjectNameGenerator projectNameGenerator;

    private ScanResults scanResultsToInject;
    private GitHubTestUtils.EventType currentGitHubEventType;

    @Before
    public void init() throws IOException, CheckmarxException {
        adoClient.ensureProjectExists();

        initCxClientMock();

        when(projectNameGenerator.determineProjectName(any()))
                .thenReturn(getProjectName());
    }

    @Given("issue tracker is ADO")
    public void issueTrackerIsADO() {
        flowProperties.setBugTracker(ISSUE_TRACKER_NAME);
    }

    @And("ADO doesn't contain any issues")
    public void adoDoesnTContainAnyIssues() throws IOException {
        adoClient.deleteProjectIssues();
    }

    @And("CxFlow filters are disabled")
    public void cxflowFiltersAreDisabled() {
        flowProperties.setFilterSeverity(new ArrayList<>());
    }

    @When("GitHub notifies CxFlow about a {string}")
    public void githubNotifiesCxFlowAboutEvent(String eventName) {
        currentGitHubEventType = determineEventType(eventName);
    }

    @And("SAST scan returns a report with 1 finding")
    public void sastScanReturnsAReportWithFinding() {
        scanResultsToInject = new ScanResultsBuilder().getScanResultsWithSingleFinding(getProjectName());
    }

    @And("CxFlow publishes the report")
    public void cxFlowPublishesReport() {
        GitHubController gitHubController = getGitHubControllerInstance();
        testUtils.callController(gitHubController, currentGitHubEventType, null);
    }

    @Then("ADO contains {int} issues")
    public void adoContainsIssueCount(int expectedIssueCount) {
        Duration timeout = Duration.ofSeconds(30);
        Duration pollInterval = Duration.ofSeconds(5);
        try {
            Awaitility.await().atMost(timeout)
                    .pollInterval(pollInterval)
                    .until(() -> adoClient.getIssueCount() == expectedIssueCount);
        } catch (ConditionTimeoutException e) {
            String message = String.format("Waited for %s but didn't get the expected ADO issue count (%d).",
                    timeout, expectedIssueCount);

            Assert.fail(message);
        }
    }

    private void initCxClientMock() throws CheckmarxException {
        when(cxClientMock.getReportContentByScanId(anyInt(), any()))
                .thenAnswer(invocation -> scanResultsToInject);

        when(cxClientMock.getTeamId(anyString()))
                .thenReturn("dummyTeamId");

        // Prevent an error related to scan resubmission.
        when(cxClientMock.getScanIdOfExistingScanIfExists(any()))
                .thenReturn(Constants.UNKNOWN_INT);
    }

    private GitHubController getGitHubControllerInstance() {
        List<VulnerabilityScanner> vulnerabilityScannerList = Collections.singletonList(sastScanner);
        FlowService flowService = new FlowService(vulnerabilityScannerList, projectNameGenerator, resultsService);

        return new GitHubController(gitHubProperties, flowProperties, cxProperties,
                null, flowService, helperService, gitHubAppAuthService, filterFactory, configOverrider,
                scmConfigOverrider);
    }

    private static GitHubTestUtils.EventType determineEventType(String eventName) {
        GitHubTestUtils.EventType result;
        if (eventName.equals("pull request")) {
            result = GitHubTestUtils.EventType.PULL_REQUEST;
        } else if (eventName.equals("push")) {
            result = GitHubTestUtils.EventType.PUSH;
        } else {
            throw new IllegalArgumentException("Bad event name.");
        }
        return result;
    }
}
