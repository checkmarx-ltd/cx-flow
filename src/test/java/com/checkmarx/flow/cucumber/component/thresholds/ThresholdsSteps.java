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
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CxFlowMocksConfig.class})
@Slf4j
public class ThresholdsSteps {
    private final CxClient cxClientMock;
    private final RestTemplate restTemplateMock;
    private ResultsService resultsService;

    public ThresholdsSteps(CxClient cxClientMock, RestTemplate restTemplateMock) {
        this.cxClientMock = cxClientMock;
        this.restTemplateMock = restTemplateMock;
    }

    @Before("@ThresholdsFeature")
    public void prepareMocks() {
        initMock(cxClientMock);
        initMock(restTemplateMock);
        resultsService = createResultsService();
    }

    @Given("threshold for findings of {string} severity is {string}")
    public void thresholdForFindingsOfSeverityIs(String severity, String threshold) {

    }

    @When("GitHub notifies CxFlow that a pull request was created")
    public void githubNotifiesCxFlow() {
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

    @And("{int} of {string} severity are found")
    public void highFindingsOfSeverityAreFound(int expectedFindingCount, String severity) {
    }

    @Then("CxFlow {string} the pull request")
    public void cxflowApprovesOrFailsThePullRequest(String approveOrFail) {

    }

    private ArrayList<Filter> createFilters() {
        return new ArrayList<>();
    }

    private ScanRequest createScanRequest() {
        ScanRequest scanRequest = new ScanRequest();
        scanRequest.setBugTracker(BugTracker.builder().type(BugTracker.Type.GITHUBPULL).build());
        scanRequest.setMergeNoteUri("MergeNoteUri");
        scanRequest.setAdditionalMetadata(new HashMap<String, String>() {{
            put("statuses_url", "statuses url");
        }});
        return scanRequest;
    }

    private void initMock(RestTemplate restTemplateMock) {
        Answer<ResponseEntity<String>> answer = new RestTemplateAnswer();

        Class<Object> any = ArgumentMatchers.any();
        when(restTemplateMock.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any))
                .thenAnswer(answer);
    }

    private void initMock(CxClient cxClientMock) {
        try {
            ScanResults scanResults = new ScanResults();
            scanResults.setXIssues(new ArrayList<>());
            when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenReturn(scanResults);
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

    private class RestTemplateAnswer implements Answer<ResponseEntity<String>> {
        @Override
        public ResponseEntity<String> answer(InvocationOnMock invocation) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }
}
