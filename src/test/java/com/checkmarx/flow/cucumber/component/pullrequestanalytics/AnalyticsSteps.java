package com.checkmarx.flow.cucumber.component.pullrequestanalytics;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.cucumber.common.JsonLoggerTestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.MergeResultEvaluator;
import com.checkmarx.flow.service.ResultsService;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@RequiredArgsConstructor
@SpringBootTest(classes = {CxFlowMocksConfig.class, CxFlowApplication.class})
public class AnalyticsSteps {
    private static final String PULL_REQUEST_STATUSES_URL = "statuses url stub";
    private static final String MERGE_NOTE_URL = "merge note url stub";

    private static final ObjectMapper jsonReader = new ObjectMapper();

    private final GitHubProperties gitHubProperties;
    private final FlowProperties flowProperties;
    private final MergeResultEvaluator mergeResultEvaluator;

    private final CxClient cxClientMock;
    private final CxProperties cxProperties;
    private final RestTemplate restTemplateMock;

    private static class State {
        public ScanResults scanResultsToInject;
        public int fakeScanId;
        public ScanRequest scanRequest;
        public PullRequestReport lastAnalyticsReport;
        public List<Filter> filters;
        Map<FindingSeverity, Integer> findingsPerSeverity;
    }

    private final State state = new State();

    private ResultsService resultsService;

    @Before("@PullRequestAnalyticsFeature")
    public void prepareServices() {
        initMock(cxClientMock);
        initMock(restTemplateMock);
        resultsService = createResultsService();
    }

    @Given("thresholds are configured as HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void thresholdsAreConfiguredAs(int high, int medium, int low) {
        Map<FindingSeverity, Integer> thresholds = toSeverityMap(high, medium, low);
        flowProperties.setThresholds(thresholds);
    }

    @And("filters are disabled")
    public void filtersAreDisabled() {
        state.filters = new ArrayList<>();
    }

    @When("pull request is created for a repo with URL: {string} in GitHub")
    public void pullRequestIsCreated(String repoUrl) {
        state.scanRequest = createScanRequest(repoUrl);
    }

    @And("SAST returns scan ID: {int} and finding count per severity: HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void sastReturnsScanID(int scanId, int high, int medium, int low) {
        state.fakeScanId = scanId;
        state.findingsPerSeverity = toSeverityMap(high, medium, low);
        state.scanResultsToInject = createFakeScanResults(state.findingsPerSeverity);
        processScanResultsInCxFlow();
    }

    @Then("in analytics report, the operation is {string}")
    public void inAnalyticsReportTheOperationIs(String operation) throws CheckmarxException {
        JsonLoggerTestUtils utils = new JsonLoggerTestUtils();
        state.lastAnalyticsReport = (PullRequestReport) utils.getReportNode(operation, PullRequestReport.class);
        utils.deleteLoggerContents();

        Assert.assertNotEquals(String.format("JSON node not found for the '%s' operation", operation),
                null,
                state.lastAnalyticsReport);
    }

    @And("pullRequestStatus is {string}")
    public void pullrequeststatusIs(String expectedStatus) {
        String actualStatus = state.lastAnalyticsReport.getPullRequestStatus();
        Assert.assertEquals("Unexpected pull request status.", expectedStatus, actualStatus);
    }

    @And("repoUrl is encrypted as {string}")
    public void repoUrlIsEncryptedAs(String expectedRepoUrl) {
        Assert.assertEquals("Incorrect encrypted repo URL.", expectedRepoUrl, state.lastAnalyticsReport.getRepoUrl());
    }

    @And("scanInitiator is {string}, scanId is {string}, pullRequestStatus is {string}")
    public void scanInitiatorIs(String initiator, String scanId, String status) {
        Assert.assertEquals("Unexpected initiator.", initiator, state.lastAnalyticsReport.getScanInitiator());
        Assert.assertEquals("Unexpected scan ID.", scanId, state.lastAnalyticsReport.getScanId());

        Assert.assertEquals("Unexpected pull request status.",
                status,
                state.lastAnalyticsReport.getPullRequestStatus());
    }

    @And("findingsMap is HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void findingsPerSeverity(int high, int medium, int low) {
        Map<FindingSeverity, Integer> expectedMap = toSeverityMap(high, medium, low);
        assertMapsAreEqual(expectedMap, state.lastAnalyticsReport.getFindingsMap(), "Incorrect findingsMap");
    }

    @And("thresholds are HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void thresholdsAre(int high, int medium, int low) {
        Map<FindingSeverity, Integer> actualThresholds = state.lastAnalyticsReport.getThresholds();
        Map<FindingSeverity, Integer> expectedThresholds = toSeverityMap(high, medium, low);
        assertMapsAreEqual(expectedThresholds, actualThresholds, "Incorrect thresholds");
    }

    private static void assertMapsAreEqual(Map<FindingSeverity, Integer> expected,
                                           Map<FindingSeverity, Integer> actual,
                                           String errorMessage) {
        Assert.assertEquals(errorMessage, jsonReader.valueToTree(expected), jsonReader.valueToTree(actual));
    }

    private static Map<FindingSeverity, Integer> toSeverityMap(int high, int medium, int low) {
        return new HashMap<FindingSeverity, Integer>() {{
            put(FindingSeverity.HIGH, high);
            put(FindingSeverity.MEDIUM, medium);
            put(FindingSeverity.LOW, low);
        }};
    }

    private void processScanResultsInCxFlow() {
        try {
            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    state.scanRequest, 0, 0, null, state.filters);

            task.get(1, TimeUnit.MINUTES);
        } catch (MachinaException | InterruptedException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
        }
    }

    private static ScanRequest createScanRequest(String repoUrl) {
        ScanRequest scanRequest = new ScanRequest();
        BugTracker issueTracker = BugTracker.builder().type(BugTracker.Type.GITHUBPULL).build();
        scanRequest.setBugTracker(issueTracker);
        scanRequest.setMergeNoteUri(MERGE_NOTE_URL);
        scanRequest.setProduct(ScanRequest.Product.CX);
        scanRequest.setRepoUrl(repoUrl);

        scanRequest.setAdditionalMetadata(new HashMap<String, String>() {{
            put("statuses_url", PULL_REQUEST_STATUSES_URL);
        }});
        return scanRequest;
    }


    private void initMock(RestTemplate restTemplateMock) {
        ResponseEntity<String> sendingPostRequest = restTemplateMock.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), ArgumentMatchers.<Class<String>>any());

        when(sendingPostRequest).thenAnswer(invocation -> new ResponseEntity<>(HttpStatus.OK));
    }

    private void initMock(CxClient cxClientMock) {
        try {
            when(cxClientMock.createScan(any(), any())).thenAnswer(invocation -> state.fakeScanId);
            when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenAnswer(invocation -> state.scanResultsToInject);
        } catch (CheckmarxException e) {
            Assert.fail("Error initializing mock." + e);
        }
    }

    private ResultsService createResultsService() {
        GitHubService gitService = new GitHubService(restTemplateMock,
                gitHubProperties,
                flowProperties,
                mergeResultEvaluator);

        return new ResultsService(
                cxClientMock,
                null,
                null,
                null,
                gitService,
                null,
                null,
                null,
                null,
                cxProperties,
                flowProperties);
    }

    private static ScanResults createFakeScanResults(Map<FindingSeverity, Integer> findingsPerSeverity) {
        CxScanSummary summary = new CxScanSummary();
        summary.setHighSeverity(findingsPerSeverity.get(FindingSeverity.HIGH));
        summary.setMediumSeverity(findingsPerSeverity.get(FindingSeverity.MEDIUM));
        summary.setLowSeverity(findingsPerSeverity.get(FindingSeverity.LOW));
        summary.setInfoSeverity(0);

        HashMap<String, Object> flowSummary = new HashMap<>();
        findingsPerSeverity.forEach((severity, count) -> flowSummary.put(severity.toString(), count));

        Map<String, Object> details = new HashMap<>();
        details.put(Constants.SUMMARY_KEY, flowSummary);

        return ScanResults.builder()
                .scanSummary(summary)
                .additionalDetails(details)
                .xIssues(new ArrayList<>())
                .build();
    }
}
