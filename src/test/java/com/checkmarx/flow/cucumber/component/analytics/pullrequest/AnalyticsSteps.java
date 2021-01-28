package com.checkmarx.flow.cucumber.component.analytics.pullrequest;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.cucumber.common.JsonLoggerTestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.CxScannerService;
import com.checkmarx.flow.service.GitHubAppAuthService;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.ThresholdValidator;
import com.checkmarx.flow.service.ResultsService;
import com.checkmarx.flow.utils.AesEncryptionUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.checkmarx.sdk.dto.ast.SCAResults;
import com.checkmarx.sdk.dto.ast.Summary;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.scanner.CxClient;
import com.checkmarx.sdk.service.CxService;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
import com.checkmarx.sdk.dto.scansummary.Severity;
import com.checkmarx.sdk.dto.sca.report.Finding;
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

import java.io.IOException;
import java.util.*;
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
    private static final String PULL_REQUEST_STATUSES_URL = "http://statuses.url.stub";
    private static final String MERGE_NOTE_URL = "http://merge.note.url.stub";

    private final GitHubProperties gitHubProperties;
    private final FlowProperties flowProperties;
    private final ThresholdValidator thresholdValidator;
    private final ScmConfigOverrider scmConfigOverrider;

    private final CxService cxClientMock;
    private final CxProperties cxProperties;
    private final RestTemplate restTemplateMock;
    private final GitHubAppAuthService gitHubAppAuthService;

    private static class State {
        ScanResults scanResultsToInject;
        int fakeScanId;
        ScanRequest scanRequest;
        PullRequestReport lastAnalyticsReport;
        FilterConfiguration filter;
        Map<FindingSeverity, Integer> findingsPerSeverity;
    }

    private final State state = new State();
    private final JsonLoggerTestUtils loggerUtils = new JsonLoggerTestUtils();

    private ResultsService resultsService;

    @Before("@PullRequestAnalyticsFeature")
    public void prepareServices() throws IOException {
        initMock(cxClientMock);
        initMock(restTemplateMock);
        resultsService = createResultsService();
        loggerUtils.clearLogContents();
    }

    @Given("thresholds are configured as HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void thresholdsAreConfiguredAs(int high, int medium, int low) {
        Map<FindingSeverity, Integer> thresholds = toSeverityMap(high, medium, low);
        flowProperties.setThresholds(thresholds);
    }

    @Given("SCA Scan in configured in application.yml")
    public void scanScanIsconfigured(){}

    @And("filters are disabled")
    public void filtersAreDisabled() {
        state.filter = FilterConfiguration.fromSimpleFilters(Collections.emptyList());
    }

    @When("pull request is created for a repo with URL: {string} in GitHub")
    public void pullRequestIsCreated(String repoUrl) {
        state.scanRequest = createScanRequest(repoUrl);
    }

    @And("SAST returns scan ID: {int} and finding count per severity: HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void sastReturnsScanID(int scanId, int high, int medium, int low) throws InterruptedException {
        state.fakeScanId = scanId;
        state.findingsPerSeverity = toSeverityMap(high, medium, low);
        state.scanResultsToInject = createFakeSASTScanResults(state.findingsPerSeverity);
        processScanResultsInCxFlow();
    }

    @And("SCA returns scan ID: {int} and finding count per severity: HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void scaReturnsScanID(int scanId, int high, int medium, int low) throws InterruptedException {
        state.fakeScanId = scanId;
        state.findingsPerSeverity = toSeverityMap(high, medium, low);
        state.scanResultsToInject = createFakeSCAScanResults(state.findingsPerSeverity, scanId);
        processScanResultsInCxFlow();
    }

    @Then("in analytics report, the operation is {string}")
    public void inAnalyticsReportTheOperationIs(String operation) throws CheckmarxException {
        state.lastAnalyticsReport = (PullRequestReport) loggerUtils.getReportNode(operation, PullRequestReport.class);

        Assert.assertNotEquals(String.format("JSON node not found for the '%s' operation", operation),
                null,
                state.lastAnalyticsReport);
    }

    @And("pullRequestStatus is {string}")
    public void pullrequeststatusIs(String expectedStatus) {
        OperationResult result = state.lastAnalyticsReport.getPullRequestResult();
        Assert.assertEquals("Unexpected pull request status.", expectedStatus, result.getStatus().toString());
    }

    @And("repoUrl contains encrypted {string}")
    public void repoUrlIsEncryptedRepoUrl(String expectedRepoUrl) throws IOException {
        String encryptedRepoUrl = state.lastAnalyticsReport.getRepoUrl();
        String decryptedRepoUrl = AesEncryptionUtils.decrypt(encryptedRepoUrl);
        Assert.assertEquals("Incorrect encrypted repo URL.", expectedRepoUrl, decryptedRepoUrl);
    }

    @And("scanInitiator is {string}, scanId is {string}")
    public void scanInitiatorIs(String initiator, String scanId) {
        Assert.assertEquals("Unexpected initiator.", initiator, state.lastAnalyticsReport.getScanInitiator());
        Assert.assertEquals("Unexpected scan ID.", scanId, state.lastAnalyticsReport.getScanId());
    }

    @And("findingsMap is HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void findingsPerSeverity(int high, int medium, int low) {
        Map<FindingSeverity, Integer> expectedMap = toSeverityMap(high, medium, low);
        Map<FindingSeverity, Integer> actualMap = state.lastAnalyticsReport.getFindingsPerSeverity();
        actualMap.remove(FindingSeverity.INFO);
        Assert.assertEquals("Incorrect finding map", expectedMap, actualMap);
    }

    @And("thresholds are HIGH: {int}, MEDIUM: {int}, LOW: {int}")
    public void thresholdsAre(int high, int medium, int low) {
        Map<FindingSeverity, Integer> actualThresholds = state.lastAnalyticsReport.getThresholds();
        Map<FindingSeverity, Integer> expectedThresholds = toSeverityMap(high, medium, low);
        Assert.assertEquals("Incorrect thresholds", expectedThresholds, actualThresholds);
    }

    private static Map<FindingSeverity, Integer> toSeverityMap(int high, int medium, int low) {
        HashMap<FindingSeverity, Integer> result = new HashMap<>();
        result.put(FindingSeverity.HIGH, high);
        result.put(FindingSeverity.MEDIUM, medium);
        result.put(FindingSeverity.LOW, low);
        return result;
    }

    private void processScanResultsInCxFlow() throws InterruptedException {
        try {
            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    state.scanRequest, 0, state.fakeScanId, null, state.filter);

            task.get(1, TimeUnit.MINUTES);
        } catch (MachinaException | ExecutionException | TimeoutException e) {
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

        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("statuses_url", PULL_REQUEST_STATUSES_URL);
        scanRequest.setAdditionalMetadata(metadata);

        return scanRequest;
    }


    private void initMock(RestTemplate restTemplateMock) {
        ResponseEntity<String> sendingPostRequest = restTemplateMock.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), ArgumentMatchers.<Class<String>>any());

        when(sendingPostRequest).thenAnswer(invocation -> new ResponseEntity<>(HttpStatus.OK));
        when(restTemplateMock.exchange(anyString(),eq(HttpMethod.GET),any(), any(Class.class) )).thenReturn(createResponseForGetComments());
    }

    private ResponseEntity<String> createResponseForGetComments() {
        return ResponseEntity.ok("{}");
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
                thresholdValidator,
                scmConfigOverrider,
                gitHubAppAuthService);
                
        
        CxScannerService cxScannerService = new CxScannerService(cxProperties,null, null, cxClientMock, null );

        return new ResultsService(
                cxScannerService,
                null,
                null,
                null,
                gitService,
                null,
                null,
                null,
                null);
    }

    private static ScanResults createFakeSASTScanResults(Map<FindingSeverity, Integer> findingsPerSeverity) {
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

    private static ScanResults createFakeSCAScanResults(Map<FindingSeverity, Integer> findingsPerSeverity, int scanId) {

        Map<Filter.Severity, Integer> findingCounts= new HashMap<>() ;

        SCAResults scaResults = new SCAResults();

        scaResults.setScanId("" + scanId);

        List<Finding> findings = new LinkedList<>();
        addFinding(findingsPerSeverity.get(FindingSeverity.HIGH), findingCounts, findings, Severity.HIGH, Filter.Severity.HIGH);
        addFinding(findingsPerSeverity.get(FindingSeverity.MEDIUM), findingCounts, findings, Severity.MEDIUM, Filter.Severity.MEDIUM);
        addFinding(findingsPerSeverity.get(FindingSeverity.LOW), findingCounts, findings, Severity.LOW, Filter.Severity.LOW);

        Summary summary = new Summary();
        summary.setFindingCounts(findingCounts);

        scaResults.setFindings(findings);

        scaResults.setSummary(summary);
        scaResults.setPackages(new LinkedList<>());

        return ScanResults.builder()
                .scaResults(scaResults)
                .xIssues(new ArrayList<>())
                .build();
    }

    private static void addFinding(Integer countFindingsPerSeverity, Map<Filter.Severity, Integer> findingCounts, List<Finding> findings, Severity severity, Filter.Severity filterSeverity) {
        for ( int i=0; i <countFindingsPerSeverity; i++) {
            Finding fnd = new Finding();
            fnd.setSeverity(severity);
            fnd.setPackageId("");
            findings.add(fnd);
        }

        findingCounts.put(filterSeverity, countFindingsPerSeverity);
    }
}
