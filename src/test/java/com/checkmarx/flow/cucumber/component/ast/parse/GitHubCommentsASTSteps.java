package com.checkmarx.flow.cucumber.component.ast.parse;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.cucumber.common.JsonLoggerTestUtils;
import com.checkmarx.flow.dto.BugTracker;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.ScanResultsReport;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.*;

import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResults;
import com.checkmarx.sdk.dto.ast.SCAResults;
import com.checkmarx.sdk.dto.ast.Summary;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.sdk.service.CxService;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
import com.cx.restclient.ast.dto.sast.AstSastResults;

import com.cx.restclient.ast.dto.sast.report.AstSastSummaryResults;
import com.cx.restclient.ast.dto.sast.report.FindingNode;
import com.cx.restclient.ast.dto.sast.report.StatusCounter;
import com.cx.restclient.dto.scansummary.Severity;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import com.cx.restclient.ast.dto.sast.report.Finding;


import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import static com.checkmarx.flow.utils.HTMLHelper.NO_POLICY_VIOLATION_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = {CxFlowMocksConfig.class, CxFlowApplication.class})
public class GitHubCommentsASTSteps {

    private final static int PROJECT_ID = 101;
    private final static int SCAN_ID = 100001;
    private static final String PULL_REQUEST_STATUSES_URL = "statuses url stub";
    private static final String MERGE_NOTE_URL = "merge note url stub";
    private static final String AST = "AST";
    private static final String GITHUB = "GITHUB";
    private static final String AST_SCA = "AST,SCA";
    private static final String GIT_URL =  "https://github.com/cxflowtestuser/testsAST.git";
    private static final String REGEX = "### Checkmarx";
    private static final String AST_WEB_REPORT_LINK = "https://astWebReportUrl";

    private final CxService cxClientMock;
    private final CxProperties cxProperties;
    private final EmailService emailService;
    private final IssueService issueService;
    private final GitHubService gitHubService;
    private final GitHubProperties gitHubProperties;
    private final FlowProperties flowProperties;
    private ScanResults scanResultsToInject;
    private ResultsService resultsService;
    private String scannerType;
    private ScanRequest.Repository repo;
    private String comment;

    public GitHubCommentsASTSteps(CxService cxClientMock, FlowProperties flowProperties,
                                  EmailService emailService,
                                  GitHubService gitHubService, IssueService issueService,
                                  GitHubProperties gitHubProperties,
                                  CxProperties cxProperties) {
        this.cxClientMock = cxClientMock;
        flowProperties.setThresholds(new HashMap<>());
        this.emailService = emailService;
        this.gitHubService = Mockito.spy(gitHubService);
        this.issueService = issueService;
        this.gitHubProperties = gitHubProperties;
        this.cxProperties = cxProperties;
        this.flowProperties = flowProperties;
    }

    @Before()
    public void prepareServices() {
        initMock(cxClientMock);
        initGitHubServiceMock();
    }

    private void initGitHubServiceMock() {

        GitHubServiceAnswerer answerer = new GitHubServiceAnswerer();
        doAnswer(answerer).when(gitHubService).sendMergeComment(any(), any());
            
    }

   
    private class GitHubServiceAnswerer implements Answer<Void> {
        
        @Override
        public Void answer(InvocationOnMock invocation) {
            comment = invocation.getArgument(1);
            return null;
        }
    }

    private ResultsService initResultsService(String repo) {
        
        if(repo.equalsIgnoreCase(GITHUB)) {

            this.repo = ScanRequest.Repository.GITHUB;
            
            initGitHubProperties();
            
            return new ResultsService(
                    cxClientMock,
                    null,
                    cxProperties,
                    null,
                    null,
                    null,
                    issueService,
                    gitHubService,
                    null, 
                    null,
                    null,
                    emailService,
                    flowProperties
                    );
        }
        
        throw new UnsupportedOperationException();
    }


    private ScanResults createFakeASTScanResults(int highCount, int mediumCount, int lowCount) {
        ScanResults result = new ScanResults();
        ASTResults astResults = new ASTResults();
        AstSastResults astSastResults = new AstSastResults();

        List<Finding> findings = new LinkedList<>();

        astSastResults.setScanId("" + SCAN_ID);

        boolean addNodes = false;
        if(highCount + mediumCount + lowCount > 0){
            addNodes = true;
        }
        List<StatusCounter> findingCounts = new LinkedList<> ();
        addFinding(highCount, findingCounts, findings, Severity.HIGH.name(),addNodes, "SQL_INJECTION");
        addFinding(mediumCount, findingCounts, findings, Severity.MEDIUM.name(), addNodes, "Hardcoded_password_in_Connection_String");
        addFinding(lowCount, findingCounts, findings, Severity.LOW.name(),addNodes, "Open_Redirect");
        
        astSastResults.setFindings(findings);
        astResults.setResults(astSastResults);
        result.setAstResults(astResults);
        
        AstSastSummaryResults summary = new AstSastSummaryResults();
        summary.setStatusCounters(findingCounts);
        summary.setHighVulnerabilityCount(highCount);
        summary.setMediumVulnerabilityCount(mediumCount);
        summary.setLowVulnerabilityCount(lowCount);

        astSastResults.setWebReportLink(AST_WEB_REPORT_LINK);
        astSastResults.setSummary(summary);
        Map<String, Object> details = new HashMap<>();
        details.put(Constants.SUMMARY_KEY, new HashMap<>());
        result.setAdditionalDetails(details);
        return result;
    }

    private static ScanResults createFakeSCAScanResults(int high, int medium, int low) {

        Map<Filter.Severity, Integer> findingCounts= new HashMap<>() ;

        SCAResults scaResults = new SCAResults();

        scaResults.setScanId("" + SCAN_ID);

        List<com.cx.restclient.ast.dto.sca.report.Finding> findings = new LinkedList<>();
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

    private static void addFinding(Integer countFindingsPerSeverity, Map<Filter.Severity, Integer> findingCounts, List<com.cx.restclient.ast.dto.sca.report.Finding> findings, Severity severity, Filter.Severity filterSeverity) {
        for ( int i=0; i <countFindingsPerSeverity; i++) {
            com.cx.restclient.ast.dto.sca.report.Finding fnd = new com.cx.restclient.ast.dto.sca.report.Finding();
            fnd.setSeverity(severity);
            fnd.setPackageId("");
            findings.add(fnd);
        }

        findingCounts.put(filterSeverity, countFindingsPerSeverity);
    }
    
    private  void addFinding(Integer countFindingsPerSeverity, List<StatusCounter> findingCounts, List<Finding> findings, String severity,boolean addNodes, String  queryName) {
        for ( int i=0; i <countFindingsPerSeverity; i++) {
            Finding fnd = new Finding();
            fnd.setSeverity(severity);
            fnd.setQueryName(queryName + "-" + i);
            if(addNodes){
                addNodes(fnd);
            }
            findings.add(fnd);
        }
        
        StatusCounter statusCounter = new StatusCounter();
        statusCounter.setStatus(severity);
        statusCounter.setCounter(countFindingsPerSeverity);
        findingCounts.add(statusCounter);
    }

    private void addNodes(Finding f) {

        FindingNode node = new FindingNode();
        node.setFileName("file");
        node.setLine(10);
        node.setName("");
        node.setColumn(20);
        LinkedList<FindingNode> nodes = new LinkedList<>();
        nodes.add(node);
        f.setNodes(nodes);

    }
    
    private void initMock(CxClient cxClientMock) {
        try {
            ScanResultsAnswerer answerer = new ScanResultsAnswerer();
            when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenAnswer(answerer);
        } catch (CheckmarxException e) {
            Assert.fail("Error initializing mock." + e);
        }
    }

    private void initGitHubProperties() {
        this.gitHubProperties.setCxSummary(false);
        this.gitHubProperties.setUrl(GIT_URL);
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setApiUrl("https://api.github.com/repos");
        this.gitHubProperties.setBlockMerge(false);
        this.gitHubProperties.setFlowSummary(true);
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
        scanResultsToInject.setAdditionalDetails( new HashMap<>());
        flowSummary.put("High", high);
        flowSummary.put("Medium", medium);
        flowSummary.put("Low", low);
        flowSummary.put("Info", info);
        scanResultsToInject.getAdditionalDetails().put("flow-summary", flowSummary);
    }

    private ScanRequest createScanRequest() {
        ScanRequest scanRequest = new ScanRequest();

        scanRequest.setMergeNoteUri(MERGE_NOTE_URL);
        scanRequest.setProduct(ScanRequest.Product.CX);
        Map<String,String> additionalMetaData = new HashMap<>();
        additionalMetaData.put("statuses_url", PULL_REQUEST_STATUSES_URL);
        scanRequest.setAdditionalMetadata(additionalMetaData);

        if(repo.equals(ScanRequest.Repository.GITHUB)) {
            scanRequest.setRepoType(ScanRequest.Repository.GITHUB);
            BugTracker issueTracker = BugTracker.builder().type(BugTracker.Type.GITHUBPULL).build();
            scanRequest.setBugTracker(issueTracker);
        }else{
            throw new UnsupportedOperationException();
        }
        return scanRequest;
    }

    @When("repository is {string} and scanner is {string}")
    public void setRepoAndScanner(String repo, String scannerType){

        resultsService = initResultsService(repo);
        this.scannerType = scannerType;
    }
    @And("doing get results operation on scan with {int} {int} {int} {int} results")
    public void getASTResults(int high, int medium, int low, int info) throws InterruptedException {
        try {
            if(scannerType.equalsIgnoreCase(AST)) {
                scanResultsToInject = createFakeASTScanResults(high, medium, low);
                addFlowSummaryToResults(high, medium, low, info);
            }
            if(scannerType.equalsIgnoreCase(AST_SCA)) {
                scanResultsToInject = createFakeSCAScanResults(high, medium, low);
                ScanResults astScanResults = createFakeASTScanResults(high, medium, low);
                scanResultsToInject.setAstResults(astScanResults.getAstResults());
                addFlowSummaryToResults(high*2, medium*2, low*2, info*2);
            }
            
            ScanRequest scanRequest = createScanRequest();

            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    scanRequest, PROJECT_ID, SCAN_ID, null, null);
            task.get(1, TimeUnit.MINUTES);
            
        } catch (MachinaException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
        }
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



    @Then("we should see the expected number of results in comments")
    public void verifyComments(){

        int actaulHighCounter = StringUtils.countMatches(comment, "HIGH");
        int actualMediumCounter = StringUtils.countMatches(comment, "MEDIUM");
        int actaulLowCounter = StringUtils.countMatches(comment, "LOW");

        
        if (scannerType.equalsIgnoreCase(AST_SCA)) {
            Assert.assertTrue(PullRequestCommentsHelper.isSastAndScaComment(comment) );

            Assert.assertEquals(scanResultsToInject.getAstResults().getResults().getSummary().getHighVulnerabilityCount()+
                    scanResultsToInject.getScaResults().getSummary().getFindingCounts().get(Filter.Severity.HIGH), actaulHighCounter);
            Assert.assertEquals(scanResultsToInject.getAstResults().getResults().getSummary().getMediumVulnerabilityCount() +
                    scanResultsToInject.getScaResults().getSummary().getFindingCounts().get(Filter.Severity.MEDIUM), actualMediumCounter);
            
            // add 1 to the results 
            Assert.assertEquals(scanResultsToInject.getAstResults().getResults().getSummary().getLowVulnerabilityCount()+ 1 +
                    scanResultsToInject.getScaResults().getSummary().getFindingCounts().get(Filter.Severity.LOW), actaulLowCounter);

        }
        else if (scannerType.equalsIgnoreCase(AST)) {
            
            Assert.assertTrue(PullRequestCommentsHelper.isSastFindingsComment(comment));

            int expectedHigh = getExpectedResults(scanResultsToInject.getAstResults().getResults().getSummary().getHighVulnerabilityCount());
            int expectedMedium = getExpectedResults(scanResultsToInject.getAstResults().getResults().getSummary().getMediumVulnerabilityCount());
            int expectedLow = getExpectedResults(scanResultsToInject.getAstResults().getResults().getSummary().getLowVulnerabilityCount());
            
            Assert.assertEquals(expectedHigh  , actaulHighCounter);
            Assert.assertEquals(expectedMedium  , actualMediumCounter);
            Assert.assertEquals(expectedLow , actaulLowCounter);
    
            if(!comment.contains(NO_POLICY_VIOLATION_MESSAGE)) {
                Assert.assertTrue(comment.contains(AST_WEB_REPORT_LINK));
            }
        }

        
    }

    private int getExpectedResults(int vulnerabilityCount) {
        
        //adding 1 to the expected count since the severity string appears not only in the Summary section but also in in the Flow Summary Section
        return vulnerabilityCount==0 ? 0 :  vulnerabilityCount + 1;
    }
}
