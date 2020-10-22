package com.checkmarx.flow.cucumber.component.analytics.get;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.common.JsonLoggerTestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.ScanResultsReport;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.CxScannerService;
import com.checkmarx.flow.service.EmailService;
import com.checkmarx.flow.service.ResultsService;
import com.checkmarx.jira.PublishUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.checkmarx.sdk.dto.ast.SCAResults;
import com.checkmarx.sdk.dto.ast.Summary;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.sdk.service.CxService;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
import com.cx.restclient.dto.scansummary.Severity;
import com.cx.restclient.ast.dto.sca.report.Finding;

import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
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
    private final CxService cxClientMock;
    private final EmailService emailService;
    private final CxProperties cxProperties;
    private ScanResults scanResultsToInject;
    private ResultsService resultsService;


    public GetResultsAnalyticsTestSteps(CxService cxClientMock, FlowProperties flowProperties, EmailService emailService, CxProperties cxProperties) {
        this.cxClientMock = cxClientMock;
        flowProperties.setThresholds(new HashMap<>());
        this.emailService = emailService;
        this.cxProperties = cxProperties;
    }

    @Before()
    public void prepareServices() {
        initMock(cxClientMock);

        resultsService = createResultsService();
    }

    private ResultsService createResultsService() {
        
        CxScannerService cxScannerService = new CxScannerService(cxProperties,null, null, cxClientMock, null );
        
        return new ResultsService(
                cxScannerService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                emailService);
    }


    private static ScanResults createFakeSASTScanResults() {
        ScanResults result = new ScanResults();
        CxScanSummary summary =  new CxScanSummary();
        result.setScanSummary(summary);
        Map<String, Object> details = new HashMap<>();
        details.put(Constants.SUMMARY_KEY, new HashMap<>());
        result.setAdditionalDetails(details);
        result.setXIssues(new ArrayList<>());
        return result;
    }

    private static ScanResults createFakeSCAScanResults(int high, int medium, int low) {

        Map<Filter.Severity, Integer> findingCounts= new HashMap<Filter.Severity, Integer>() ;

        SCAResults scaResults = new SCAResults();

        scaResults.setScanId("" + SCAN_ID);

        List<Finding> findings = new LinkedList<>();
        addFinding(high, findingCounts, findings, Severity.HIGH, Filter.Severity.HIGH);
        addFinding(medium, findingCounts, findings, Severity.MEDIUM, Filter.Severity.MEDIUM);
        addFinding(low, findingCounts, findings, Severity.LOW, Filter.Severity.LOW);

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

    private void initMock(CxClient cxClientMock) {
        try {
            ScanResultsAnswerer answerer = new ScanResultsAnswerer();
            when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenAnswer(answerer);
        } catch (CheckmarxException e) {
            Assert.fail("Error initializing mock." + e);
        }
    }




    private Integer getFindingsNum(ScanResultsReport report, FindingSeverity severity) {
        return report.getScanSummary().get(severity);
    }

    private Integer getScanResultsBySeverity(ScanResultsReport report, FindingSeverity severity) {
        return report.getCxFlowResults().get(severity);
    }

    private ScanResultsReport getLatestReport() throws CheckmarxException {
        JsonLoggerTestUtils  testUtils = new JsonLoggerTestUtils();
        return (ScanResultsReport)testUtils.getReportNode(ScanResultsReport.OPERATION, ScanResultsReport.class);
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
        Map<String,String> additionalMetaData = new HashMap<>();
        additionalMetaData.put("statuses_url", PULL_REQUEST_STATUSES_URL);
        scanRequest.setAdditionalMetadata(additionalMetaData);
        return scanRequest;
    }

    @When("doing get results operation on SAST scan with {int} {int} {int} {int} results")
    public void getSASTResults(int high, int medium, int low, int info) throws InterruptedException {
        try {
            scanResultsToInject = createFakeSASTScanResults();
            ScanRequest scanRequest = createScanRequest();
            setFindingsSummary(high, medium, low, info);
            addAdditionalInfoToResults();
            addFlowSummaryToResults(high, medium, low, info);
            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    scanRequest, PROJECT_ID, SCAN_ID, null, null);
            task.get(1, TimeUnit.MINUTES);
        } catch (MachinaException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
        }
    }

    @When("doing get results operation on SCA scan with {int} {int} {int} results")
    public void getSCAResults(int high, int medium, int low) throws InterruptedException {
        try {
            scanResultsToInject = createFakeSCAScanResults(high, medium, low);
            ScanRequest scanRequest = createScanRequest();

            //addAdditionalInfoToResults();
            //addFlowSummaryToResults(high, medium, low, info);
            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    scanRequest, PROJECT_ID, SCAN_ID, null, null);
            task.get(1, TimeUnit.MINUTES);
        } catch (MachinaException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
        }
    }

    @When("doing get results operation on SAST scan with {int} {int} {int} {int} results and filter is {string}")
    public void getResultsWithFilter(int high, int medium, int low, int info, String filter) throws InterruptedException  {
        try {
            scanResultsToInject = createFakeSASTScanResults();
            ScanRequest scanRequest = createScanRequest();
            setFindingsSummary(high, medium, low, info, filter);
            addAdditionalInfoToResults();
            addFlowSummaryToResults(high, medium, low, info);
            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    scanRequest, PROJECT_ID, SCAN_ID, null, null);
            task.get(1, TimeUnit.MINUTES);
        } catch (MachinaException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
        }
    }
    @Then("we should see the expected number of tickets in analytics for SAST")
    public void verifyReportSAST() throws CheckmarxException {
        ScanResultsReport report = getLatestReport();
        assertScanSASTSummary(report);
        assertScanSASTResults(report);
        Assert.assertEquals("Scan ID does not match !", Integer.valueOf(SCAN_ID), Integer.valueOf(report.getScanId()));
    }

    @Then("we should see the expected number of tickets in analytics for SCA")
    public void verifyReportSCA() throws CheckmarxException {
        ScanResultsReport report = getLatestReport();
        assertScanSCASummary(report);
        Assert.assertEquals("Scan ID does not match !", Integer.valueOf(SCAN_ID), Integer.valueOf(report.getScanId()));
    }

    private void assertScanSASTResults(ScanResultsReport report) {
        Assert.assertEquals("Error getting high severity flow results", getFlowSummaryField("High") , getScanResultsBySeverity(report, FindingSeverity.HIGH));
        Assert.assertEquals("Error getting medium severity flow results", getFlowSummaryField("Medium") , getScanResultsBySeverity(report, FindingSeverity.MEDIUM));
        Assert.assertEquals("Error getting low severity flow results", getFlowSummaryField("Low") , getScanResultsBySeverity(report, FindingSeverity.LOW));
        Assert.assertEquals("Error getting info severity flow results", getFlowSummaryField("Info") , getScanResultsBySeverity(report, FindingSeverity.INFO));
    }

    private Integer getFlowSummaryField(String field) {
        return (Integer) ((Map) scanResultsToInject.getAdditionalDetails().get("flow-summary")).get(field);
    }

    private void assertScanSASTSummary(ScanResultsReport report) {
        Assert.assertEquals("Error getting high severity results summary", scanResultsToInject.getScanSummary().getHighSeverity(), getFindingsNum(report, FindingSeverity.HIGH));
        Assert.assertEquals("Error getting low severity results summary", scanResultsToInject.getScanSummary().getLowSeverity(), getFindingsNum(report, FindingSeverity.LOW));
        Assert.assertEquals("Error getting info severity results summary", scanResultsToInject.getScanSummary().getInfoSeverity(), getFindingsNum(report, FindingSeverity.INFO));
    }

    private void assertScanSCASummary(ScanResultsReport report) {
        Assert.assertEquals("Error getting high severity results summary", scanResultsToInject.getScaResults().getSummary().getFindingCounts().get(Filter.Severity.HIGH), getFindingsNum(report, FindingSeverity.HIGH));
        Assert.assertEquals("Error getting medium severity results summary", scanResultsToInject.getScaResults().getSummary().getFindingCounts().get(Filter.Severity.MEDIUM), getFindingsNum(report, FindingSeverity.MEDIUM));
        Assert.assertEquals("Error getting low severity results summary", scanResultsToInject.getScaResults().getSummary().getFindingCounts().get(Filter.Severity.LOW), getFindingsNum(report, FindingSeverity.LOW));
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
