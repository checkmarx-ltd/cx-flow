package com.checkmarx.flow.cucumber.integration.azure.publishing.githubflow;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.cucumber.common.Constants;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.azure.publishing.AzureDevopsClient;
import com.checkmarx.flow.cucumber.integration.azure.publishing.PublishingStepsBase;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.github.GitHubTestUtils;
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
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;

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
    @MockBean
    private final CxClient cxClientMock;
    private final ResultsService resultsService;
    private final ADOService adoService;
    private final EmailService emailService;

    private ScanResults scanResultsToInject;
    private String webhookRequestBody;
    private GitHubTestUtils.EventType currentGitHubEventType;

    @Before
    public void init() throws IOException, CheckmarxException {
        adoClient.ensureProjectExists();

        when(cxClientMock.getReportContentByScanId(anyInt(), any()))
                .thenAnswer(invocation -> scanResultsToInject);

        when(cxClientMock.getTeamId(anyString()))
                .thenReturn("dummyTeamId");
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
    public void githubNotifiesCxFlowAboutEvent(String eventName) throws IOException {
        currentGitHubEventType = determineEventType(eventName);
        String filename = determineRequestFilename(currentGitHubEventType);
        webhookRequestBody = loadWebhookRequestBody(filename);
    }

    @And("SAST scan returns a report with 1 finding")
    public void sastScanReturnsAReportWithFinding() {
        scanResultsToInject = new ScanResultsBuilder().getScanResultsWithSingleFinding(getProjectName());
    }

    @And("CxFlow publishes the report")
    public void cxFlowPublishesReport() {
        GitHubController gitHubController = getGitHubControllerInstance();
        String signature = testUtils.createSignature(webhookRequestBody);
        if (currentGitHubEventType == GitHubTestUtils.EventType.PULL_REQUEST) {
            submitPullRequest(gitHubController, signature);
        } else {
            submitPush(gitHubController, signature);
        }
    }

    @Then("ADO contains {int} issue")
    public void adoContainsIssueCount(int expectedIssueCount) {
        Duration timeout = Duration.ofMinutes(1);
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

    private void submitPush(GitHubController gitHubController, String signature) {
        gitHubController.pushRequest(webhookRequestBody, signature,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null);
    }

    private void submitPullRequest(GitHubController gitHubController, String signature) {
        gitHubController.pullRequest(webhookRequestBody, signature,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null);
    }

    private GitHubController getGitHubControllerInstance() {
        FlowService flowService = new FlowService(
                cxClientMock,
                null,
                resultsService,
                gitHubService,
                null,
                null,
                adoService,
                emailService,
                helperService,
                cxProperties,
                flowProperties);

        return new GitHubController(gitHubProperties, flowProperties, cxProperties,
                null, flowService, helperService, gitHubService);
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

    private static String loadWebhookRequestBody(String filename) throws IOException {
        String path = Paths.get(Constants.WEBHOOK_REQUEST_DIR, filename).toString();
        File requestFile = TestUtils.getFileFromResource(path);
        return FileUtils.readFileToString(requestFile, StandardCharsets.UTF_8);
    }

    private static String determineRequestFilename(GitHubTestUtils.EventType eventType) {
        return eventType == GitHubTestUtils.EventType.PULL_REQUEST ?
                "github-pull-request.json" : "github-push.json";
    }
}
