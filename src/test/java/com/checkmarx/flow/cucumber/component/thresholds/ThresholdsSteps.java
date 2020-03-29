package com.checkmarx.flow.cucumber.component.thresholds;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
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
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CxFlowMocksConfig.class, CxFlowApplication.class})
@Slf4j
public class ThresholdsSteps {
    private static final String PULL_REQUEST_STATUSES_URL = "statuses url stub";
    private static final String MERGE_NOTE_URL = "merge note url stub";

    private static final ObjectMapper mapper = new ObjectMapper();

    private final CxClient cxClientMock;
    private final RestTemplate restTemplateMock;
    private final MergeResultEvaluator mergeResultEvaluator;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final GitHubProperties gitHubProperties;
    private ScanResults scanResultsToInject;

    private ResultsService resultsService;
    private Boolean pullRequestWasApproved;
    private Filter filter;

    public ThresholdsSteps(CxClient cxClientMock, RestTemplate restTemplateMock, FlowProperties flowProperties,
                           CxProperties cxProperties, GitHubProperties gitHubProperties, MergeResultEvaluator mergeResultEvaluator) {

        this.cxClientMock = cxClientMock;
        this.restTemplateMock = restTemplateMock;

        flowProperties.setThresholds(new HashMap<>());
        this.flowProperties = flowProperties;

        this.cxProperties = cxProperties;

        gitHubProperties.setCxSummary(false);
        gitHubProperties.setFlowSummary(false);
        this.gitHubProperties = gitHubProperties;

        this.mergeResultEvaluator = mergeResultEvaluator;
    }

    @Before("@ThresholdsFeature")
    public void prepareServices() {
        initMock(cxClientMock);
        initMock(restTemplateMock);
        scanResultsToInject = createFakeScanResults();
        resultsService = createResultsService();
    }

    @Given("threshold for findings of {string} severity is {string}")
    public void thresholdForFindingsOfSeverityIs(String severityName, String threshold) {
        if (!threshold.equals("<omitted>")) {
            FindingSeverity severity = parseFindingSeverity(severityName);
            int numericThreshold = Integer.parseInt(threshold);
            flowProperties.getThresholds().put(severity, numericThreshold);
        }
    }

    @Given("the whole 'thresholds' section is omitted from config")
    public void theWholeThresholdsSectionIsOmittedFromConfig() {
        flowProperties.setThresholds(null);
    }

    @And("severity filter is set to {string}")
    public void severityFilterIsSetTo(String severity) {
        filter = new Filter(Filter.Type.SEVERITY, severity);
    }

    @And("no severity filter is specified")
    public void noSeverityFilterIsSpecified() {
        filter = null;
    }

    @And("^(?:SAST detects )?(.*) findings of \"(.+)\" severity$")
    public void sastDetectsFindings(int expectedFindingCount, String severity) {
        addFindingsTo(scanResultsToInject, expectedFindingCount, severity);
    }

    private void addFindingsTo(ScanResults target, int count, String severityName) {
        if (filter == null || filter.getValue().equalsIgnoreCase(severityName)) {
            Object summary = target.getAdditionalDetails().get(Constants.SUMMARY_KEY);
            assertTrue(summary instanceof Map);
            ((Map) summary).put(severityName, count);
        }
    }

    @Then("CxFlow {string} the pull request")
    public void cxflowApprovesOrFailsThePullRequest(String approvesOrFails) {
        processScanResultsInCxFlow();

        boolean expectingApproval = approvesOrFails.equals("approves");
        verifyPullRequestState(expectingApproval);
    }

    private void processScanResultsInCxFlow() {
        try {
            ScanRequest scanRequest = createScanRequest();

            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    scanRequest, 0, 0, null, null);

            task.get(1, TimeUnit.MINUTES);
        } catch (MachinaException | InterruptedException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
        }
    }

    private void verifyPullRequestState(boolean expectingApproval) {
        Assert.assertNotNull("pullRequestWasApproved is not initialized.", pullRequestWasApproved);

        if (expectingApproval) {
            Assert.assertTrue("Expected pull request to be approved, but it was failed.", pullRequestWasApproved);
        } else {
            Assert.assertFalse("Expected pull request to be failed, but it was approved.", pullRequestWasApproved);
        }
    }

    private ScanRequest createScanRequest() {
        ScanRequest scanRequest = new ScanRequest();
        BugTracker issueTracker = BugTracker.builder().type(BugTracker.Type.GITHUBPULL).build();
        scanRequest.setBugTracker(issueTracker);
        scanRequest.setMergeNoteUri(MERGE_NOTE_URL);
        scanRequest.setProduct(ScanRequest.Product.CX);

        Map<String, String> additionalMetadata = new HashMap<String, String>();
        additionalMetadata.put("statuses_url", PULL_REQUEST_STATUSES_URL);
        scanRequest.setAdditionalMetadata(additionalMetadata);
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
            ScanResultsAnswerer answerer = new ScanResultsAnswerer();
            when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenAnswer(answerer);
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

    private static ScanResults createFakeScanResults() {
        ScanResults result = new ScanResults();
        
        result.setScanSummary(new CxScanSummary());
        
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
                pullRequestWasApproved = wasApproved(interceptedRequest);
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

    private static FindingSeverity parseFindingSeverity(String severityName) {
        return FindingSeverity.valueOf(severityName.toUpperCase(Locale.ROOT));
    }

    /**
     * Returns scan results as if they were produced by SAST.
     */
    private class ScanResultsAnswerer implements Answer<ScanResults> {
        @Override
        public ScanResults answer(InvocationOnMock invocation) {
            return scanResultsToInject;
        }
    }
}
