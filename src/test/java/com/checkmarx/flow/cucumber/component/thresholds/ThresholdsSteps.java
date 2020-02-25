package com.checkmarx.flow.cucumber.component.thresholds;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.ResultsService;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CxFlowMocksConfig.class})
@Slf4j
public class ThresholdsSteps {
    private static final String PULL_REQUEST_STATUSES_URL = "statuses url stub";
    private static final String MERGE_NOTE_URL = "merge note url stub";

    private static final ObjectMapper mapper = new ObjectMapper();

    private final CxClient cxClientMock;
    private final RestTemplate restTemplateMock;
    private final CxScanSummary scanSummaryToInject = new CxScanSummary();

    private ResultsService resultsService;
    private Boolean pullRequestWasApproved;

    public ThresholdsSteps(CxClient cxClientMock, RestTemplate restTemplateMock) {
        this.cxClientMock = cxClientMock;
        this.restTemplateMock = restTemplateMock;
    }

    @Before("@ThresholdsFeature")
    public void prepareServices() {
        initMock(cxClientMock);
        initMock(restTemplateMock);
        resultsService = createResultsService();
    }

    @Given("threshold for findings of {string} severity is {string}")
    public void thresholdForFindingsOfSeverityIs(String severity, String threshold) {

    }

    @And("^(?:SAST detects )?(.*) findings of \"(.+)\" severity$")
    public void highFindingsOfSeverityAreFound(int expectedFindingCount, String severity) {
        switch (severity) {
            case "high":
                scanSummaryToInject.setHighSeverity(expectedFindingCount);
                break;
            case "medium":
                scanSummaryToInject.setMediumSeverity(expectedFindingCount);
                break;
            case "low":
                scanSummaryToInject.setLowSeverity(expectedFindingCount);
                break;
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
            List<Filter> filters = createFilters();

            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    scanRequest, 0, 0, null, filters);

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

    private ArrayList<Filter> createFilters() {
        return new ArrayList<>();
    }

    private ScanRequest createScanRequest() {
        ScanRequest scanRequest = new ScanRequest();
        BugTracker issueTracker = BugTracker.builder().type(BugTracker.Type.GITHUBPULL).build();
        scanRequest.setBugTracker(issueTracker);
        scanRequest.setMergeNoteUri(MERGE_NOTE_URL);
        scanRequest.setProduct(ScanRequest.Product.CX);

        scanRequest.setAdditionalMetadata(new HashMap<String, String>() {{
            put("statuses_url", PULL_REQUEST_STATUSES_URL);
        }});
        return scanRequest;
    }

    private void initMock(RestTemplate restTemplateMock) {
        Answer<ResponseEntity<String>> answerer = new RestTemplateAnswerer();

        ResponseEntity<String> sendingPostRequest = restTemplateMock.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), ArgumentMatchers.<Class<String>>any());

        when(sendingPostRequest).thenAnswer(answerer);
    }

    private void initMock(CxClient cxClientMock) {
        try {
            ScanResultsAnswerer answerer = new ScanResultsAnswerer();
            when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenAnswer(answerer);
        } catch (CheckmarxException e) {
            log.error("Error initializing mock.", e);
        }
    }

    private GitHubService createGitService(FlowProperties flowProperties) {
        GitHubProperties gitHubProperties = new GitHubProperties();
        gitHubProperties.setCxSummary(false);
        gitHubProperties.setFlowSummary(false);
        gitHubProperties.setToken("token");
        gitHubProperties.setErrorMerge(true);
        gitHubProperties.setBlockMerge(true);

        return new GitHubService(restTemplateMock, gitHubProperties, flowProperties);
    }

    private ResultsService createResultsService() {
        CxProperties cxProperties = new CxProperties();
        cxProperties.setEnableOsa(false);

        FlowProperties flowProperties = new FlowProperties();
        flowProperties.setMail(null);

        GitHubService gitService = createGitService(flowProperties);

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

    private class RestTemplateAnswerer implements Answer<ResponseEntity<String>> {
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

    private class ScanResultsAnswerer implements Answer<ScanResults> {
        @Override
        public ScanResults answer(InvocationOnMock invocation) {
            ScanResults scanResultsStub = new ScanResults();
            scanResultsStub.setXIssues(createIssues());
            scanResultsStub.setScanSummary(scanSummaryToInject);
            return scanResultsStub;
        }

        private List<ScanResults.XIssue> createIssues() {
            List<ScanResults.XIssue> issues = new ArrayList<>();

            boolean anyIssuesExpected = scanSummaryToInject.getHighSeverity() > 0 ||
                    scanSummaryToInject.getMediumSeverity() > 0 ||
                    scanSummaryToInject.getLowSeverity() > 0;

            if (anyIssuesExpected) {
                Map<Integer, ScanResults.IssueDetails> details = new HashMap<>();
                ScanResults.XIssue issue = ScanResults.XIssue.builder().details(details).build();
                issues.add(issue);
            }
            return issues;
        }
    }
}
