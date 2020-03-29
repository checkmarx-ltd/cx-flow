package com.checkmarx.flow.cucumber.component.pullrequestanalytics;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.cucumber.common.JsonLoggerTestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = {CxFlowMocksConfig.class, CxFlowApplication.class})
public class AnalyticsSteps {
    private static final String PULL_REQUEST_STATUSES_URL = "statuses url stub";
    private static final String MERGE_NOTE_URL = "merge note url stub";

    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonLoggerTestUtils testUtils = new JsonLoggerTestUtils();
    private static final ObjectMapper jsonReader = new ObjectMapper();
    private final FlowProperties flowProperties;
    private ResultsService resultsService;
    private final MergeResultEvaluator mergeResultEvaluator;
    private final CxProperties cxProperties;
    private final GitHubProperties gitHubProperties;

    private final CxClient cxClientMock;
    private final RestTemplate restTemplateMock;

    private static class State {
        public Boolean pullRequestWasApproved;
        public ScanResults scanResultsToInject;
        public int fakeScanId;
        public ScanRequest scanRequest;
        public JsonNode lastAnalyticsEvent;
        public List<Filter> filters;
        Map<FindingSeverity, Integer> findingsPerSeverity;
    }

    private final State state = new State();

    public AnalyticsSteps(FlowProperties flowProperties, ResultsService resultsService, CxClient cxClientMock, RestTemplate restTemplateMock, GitHubProperties gitHubProperties, MergeResultEvaluator mergeResultEvaluator, CxProperties cxProperties) {
        this.flowProperties = flowProperties;
        this.resultsService = resultsService;
        this.cxClientMock = cxClientMock;
        this.restTemplateMock = restTemplateMock;
        this.gitHubProperties = gitHubProperties;
        this.mergeResultEvaluator = mergeResultEvaluator;
        this.cxProperties = cxProperties;
    }

    @Before("@PullRequestAnalyticsFeature")
    public void prepareServices() throws CheckmarxException {
        initMock(cxClientMock);
        initMock(restTemplateMock);
        resultsService = createResultsService();

        testUtils.deleteLoggerContents();
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
        state.lastAnalyticsEvent = utils.getOperationNode(operation);
        boolean nodeExists = state.lastAnalyticsEvent != null && !state.lastAnalyticsEvent.isNull();
        Assert.assertTrue(String.format("Event node not found for the '%s' operation", operation), nodeExists);
    }

    @And("pullRequestStatus is {string}")
    public void pullrequeststatusIs(String expectedStatus) {
        String actualStatus = state.lastAnalyticsEvent.get("pullRequestStatus").textValue();
        Assert.assertEquals("Unexpected pull request status.", expectedStatus, actualStatus);
    }

    @And("repoUrl is encrypted as {string}")
    public void repourlIsEncryptedAs(String expectedRepoUrl) {
        String actualRepoUrl = state.lastAnalyticsEvent.get("repoUrl").textValue();
        Assert.assertEquals("Incorrect encrypted repo URL.", expectedRepoUrl, actualRepoUrl);
    }

    @And("scanInitiator is {string}, scanId is {int}, pullRequestStatus is {string}")
    public void scanInitiatorIs(String initiator, int scanId, String status) {
        String actualInitiator = state.lastAnalyticsEvent.get("scanInitiator").textValue();
        Assert.assertEquals("Unexpected initiator.", initiator, actualInitiator);

        int actualScanId = state.lastAnalyticsEvent.get("scanInitiator").intValue();
        Assert.assertEquals("Unexpected scan ID.", scanId, actualScanId);

        String actualStatus = state.lastAnalyticsEvent.get("pullRequestStatus").textValue();
        Assert.assertEquals("Unexpected pull request status.", status, actualStatus);
    }

    @And("findingsPerSeverity are HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void findingsPerSeverity(int high, int medium, int low) {
        JsonNode actualNode = state.lastAnalyticsEvent.get("findingsPerSeverity");
        Assert.assertEquals("Invalid node type.", JsonNodeType.OBJECT, actualNode.getNodeType());

        Map<FindingSeverity, Integer> expectedMap = toSeverityMap(high, medium, low);
        JsonNode expectedNode = jsonReader.valueToTree(expectedMap);

        Assert.assertEquals("Incorrect value of findingsPerSeverity object.", expectedNode, actualNode);
    }

    @And("thresholds are HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void thresholdsAre(int high, int medium, int low) {
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
//            ScanRequest scanRequest = createScanRequest(null);

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
        Answer<ResponseEntity<String>> interceptor = new HttpRequestInterceptor();

        ResponseEntity<String> sendingPostRequest = restTemplateMock.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), ArgumentMatchers.<Class<String>>any());

        when(sendingPostRequest).thenAnswer(interceptor);
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
        ScanResults result = new ScanResults();

        CxScanSummary summary = new CxScanSummary();
        summary.setHighSeverity(findingsPerSeverity.get(FindingSeverity.HIGH));
        summary.setMediumSeverity(findingsPerSeverity.get(FindingSeverity.MEDIUM));
        summary.setLowSeverity(findingsPerSeverity.get(FindingSeverity.LOW));
        result.setScanSummary(summary);

        Map<String, Object> details = new HashMap<>();
        details.put(Constants.SUMMARY_KEY, new HashMap<>());
        result.setAdditionalDetails(details);

        result.setXIssues(new ArrayList<>());

        return result;
    }

    /**
     * Intercepts requests that CxFlow sends to GitHub.
     * This allows to detect whether CxFlow has approved or failed a pull request.
     */
    private class HttpRequestInterceptor implements Answer<ResponseEntity<String>> {
        @Override
        public ResponseEntity<String> answer(InvocationOnMock invocation) {
            String url = invocation.getArgument(0);
            if (url.equals(PULL_REQUEST_STATUSES_URL)) {
                HttpEntity<String> interceptedRequest = invocation.getArgument(2);
                state.pullRequestWasApproved = wasApproved(interceptedRequest);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        }

        private Boolean wasApproved(HttpEntity<String> interceptedRequest) {
            Boolean result = null;
            String body = interceptedRequest.getBody();
            Assert.assertNotNull("Status request body is null.", body);
            try {
                JsonNode requestJson = mapper.readTree(body);
                String state = requestJson.get("state").textValue();
                if (state.equals("success")) {
                    result = true;
                } else if (state.equals("failure")) {
                    result = false;
                }
            } catch (JsonProcessingException e) {
                Assert.fail("Error parsing request. " + e);
            }
            return result;
        }
    }
}
