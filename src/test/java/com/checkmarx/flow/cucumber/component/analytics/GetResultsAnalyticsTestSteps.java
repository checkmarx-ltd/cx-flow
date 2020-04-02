package com.checkmarx.flow.cucumber.component.analytics;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.common.JsonLoggerTestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.GetResultsReport;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.ResultsService;
import com.checkmarx.jira.PublishUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
@Slf4j
@SpringBootTest(classes = {CxFlowMocksConfig.class, CxFlowApplication.class, PublishUtils.class})
public class GetResultsAnalyticsTestSteps {

    private final static int PROJECT_ID = 101;
    private final static int SCAN_ID = 100001;
    private static final String PULL_REQUEST_STATUSES_URL = "statuses url stub";
    private static final String MERGE_NOTE_URL = "merge note url stub";
    private final CxClient cxClientMock;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private ScanResults scanResultsToInject;
    private ResultsService resultsService;


    public GetResultsAnalyticsTestSteps(CxClient cxClientMock, FlowProperties flowProperties,
                                        CxProperties cxProperties) {
        this.cxClientMock = cxClientMock;
        flowProperties.setThresholds(new HashMap<>());
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
    }

    @Before()
    public void prepareServices() {
        initMock(cxClientMock);
        scanResultsToInject = createFakeScanResults();
        resultsService = createResultsService();
    }

    private ResultsService createResultsService() {
        return new ResultsService(
                cxClientMock,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                cxProperties,
                flowProperties);
    }


    private static ScanResults createFakeScanResults() {
        ScanResults result = new ScanResults();
        CxScanSummary summary =  new CxScanSummary();
        result.setScanSummary(summary);
        Map<String, Object> details = new HashMap<>();
        details.put(Constants.SUMMARY_KEY, new HashMap<>());
        result.setAdditionalDetails(details);
        result.setXIssues(new ArrayList<>());
        return result;
    }

    private void initMock(CxClient cxClientMock) {
        try {
            ScanResultsAnswerer answerer = new ScanResultsAnswerer();
            when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenAnswer(answerer);
        } catch (CheckmarxException e) {
            Assert.fail("Error initializing mock." + e);
        }
    }




    private Integer getFindingsNum(GetResultsReport report, FindingSeverity severity) {
        return report.getScanSummary().get(severity);
    }

    private Integer getScanResultsBySeverity(GetResultsReport report, FindingSeverity severity) {
        return report.getCxFlowResults().get(severity);
    }

    private GetResultsReport getLatestReport() throws CheckmarxException {
        JsonLoggerTestUtils  testUtils = new JsonLoggerTestUtils();
        GetResultsReport report = (GetResultsReport)testUtils.getReportNode(GetResultsReport.OPERATION, GetResultsReport.class);
        return report;
    }

    private void setFindingsSummary(int high, int medium, int low, int info, String filter) {
        FindingSeverity severity = FindingSeverity.valueOf(filter);
        switch(severity) {
            case HIGH:
                setFindingsSummary(high, 0,0,0);
                break;
            case MEDIUM:
                setFindingsSummary(high, medium,0,0);
                break;
            case LOW:
                setFindingsSummary(high, medium,low,0);
                break;
            case INFO:
                setFindingsSummary(high, medium,low,info);
                break;
        }
    }

    private void setFindingsSummary(int high, int medium, int low, int info) {
        scanResultsToInject.getScanSummary().setHighSeverity(high);
        scanResultsToInject.getScanSummary().setMediumSeverity(medium);
        scanResultsToInject.getScanSummary().setLowSeverity(low);
        scanResultsToInject.getScanSummary().setInfoSeverity(info);
    }

    private void addFlowSummaryToResults(int high, int medium, int low, int info) {
        Map<String, Object> flowSummary = new HashMap<>();
        flowSummary.put("High", high);
        flowSummary.put("Medium", medium);
        flowSummary.put("Low", low);
        flowSummary.put("Info", info);
        scanResultsToInject.getAdditionalDetails().put("flow-summary", flowSummary);
    }

    private void addAdditionalInfoToResults() {
        scanResultsToInject.getAdditionalDetails().put("numFailedLoc", 0);
        scanResultsToInject.getAdditionalDetails().put("scanRiskSeverity", 0);
        scanResultsToInject.getAdditionalDetails().put("scanId", 100001);
        scanResultsToInject.getAdditionalDetails().put("scanStartDate", new Date());
        scanResultsToInject.getAdditionalDetails().put("customFields", new HashMap<>());
        scanResultsToInject.getAdditionalDetails().put("scanRisk", 0);


    }

    private ScanRequest createScanRequest() {
        ScanRequest scanRequest = new ScanRequest();
        BugTracker issueTracker = BugTracker.builder().type(BugTracker.Type.NONE).build();
        scanRequest.setBugTracker(issueTracker);
        scanRequest.setMergeNoteUri(MERGE_NOTE_URL);
        scanRequest.setProduct(ScanRequest.Product.CX);
        scanRequest.setAdditionalMetadata(new HashMap<String, String>() {{
            put("statuses_url", PULL_REQUEST_STATUSES_URL);
        }});
        return scanRequest;
    }

    @When("doing get results operation on a scan with {int} {int} {int} {int} results")
    public void getResults(int high, int medium, int low, int info)  {
        try {
            ScanRequest scanRequest = createScanRequest();
            setFindingsSummary(high, medium, low, info);
            addAdditionalInfoToResults();
            addFlowSummaryToResults(high, medium, low, info);
            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    scanRequest, PROJECT_ID, SCAN_ID, null, null);
            task.get(1, TimeUnit.MINUTES);
        } catch (MachinaException | InterruptedException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
        }
    }

    @When("doing get results operation on a scan with {int} {int} {int} {int} results and filter is {string}")
    public void getResultsWithFilter(int high, int medium, int low, int info, String filter)  {
        try {
            ScanRequest scanRequest = createScanRequest();
            setFindingsSummary(high, medium, low, info, filter);
            addAdditionalInfoToResults();
            addFlowSummaryToResults(high, medium, low, info);
            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    scanRequest, PROJECT_ID, SCAN_ID, null, null);
            task.get(1, TimeUnit.MINUTES);
        } catch (MachinaException | InterruptedException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
        }
    }
    @Then("we should see the expected number of tickets in analytics")
    public void verifyReport() throws CheckmarxException {
        GetResultsReport report = getLatestReport();
        assertScanSummary(report);
        assertScanResults(report);
        Assert.assertEquals("Scan ID does not match !", Integer.valueOf(SCAN_ID), Integer.valueOf(report.getScanId()));
    }

    private void assertScanResults(GetResultsReport report) {
        Assert.assertEquals("Error getting high severity flow results", getFlowSummaryField("High") , getScanResultsBySeverity(report, FindingSeverity.HIGH));
        Assert.assertEquals("Error getting medium severity flow results", getFlowSummaryField("Medium") , getScanResultsBySeverity(report, FindingSeverity.MEDIUM));
        Assert.assertEquals("Error getting low severity flow results", getFlowSummaryField("Low") , getScanResultsBySeverity(report, FindingSeverity.LOW));
        Assert.assertEquals("Error getting info severity flow results", getFlowSummaryField("Info") , getScanResultsBySeverity(report, FindingSeverity.INFO));
    }

    private Integer getFlowSummaryField(String field) {
        return (Integer) ((Map) scanResultsToInject.getAdditionalDetails().get("flow-summary")).get(field);
    }

    private void assertScanSummary(GetResultsReport report) {
        Assert.assertEquals("Error getting high severity results summary", scanResultsToInject.getScanSummary().getHighSeverity(), getFindingsNum(report, FindingSeverity.HIGH));
        Assert.assertEquals("Error getting low severity results summary", scanResultsToInject.getScanSummary().getLowSeverity(), getFindingsNum(report, FindingSeverity.LOW));
        Assert.assertEquals("Error getting info severity results summary", scanResultsToInject.getScanSummary().getInfoSeverity(), getFindingsNum(report, FindingSeverity.INFO));
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
