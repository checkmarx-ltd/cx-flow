package com.checkmarx.flow.cucumber.integration.cxconfig;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.util.StringUtils;
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class CxConfigSteps {
    private static final String PULL_REQUEST_STATUSES_URL = "statuses url stub";
    private static final String MERGE_NOTE_URL = "merge note url stub";
    public static final String XSS_REFLECTED = "XSS_REFLECTED";
    public static final String SQL_INJECTION = "SQL_INJECTION";
    public static final String CWE_79 = "79";
    public static final String CWE_89 = "89";

    private final CxClient cxClientMock;
    private final GitHubService gitHubService;
    private GitHubController gitHubControllerSpy;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MergeResultEvaluator mergeResultEvaluator;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final GitHubProperties gitHubProperties;
    private final HelperService helperService;
    private ScanResults scanResultsToInject;

    private ResultsService resultsService;
    private Boolean isPullRequestApproved;

    private FlowService flowService;
    private String branch;
    private ScanRequest request;

    public CxConfigSteps( FlowProperties flowProperties, GitHubService gitHubService,
                         CxProperties cxProperties, GitHubProperties gitHubProperties, MergeResultEvaluator mergeResultEvaluator, FlowService flowService) {

        this.cxClientMock = mock(CxClient.class);

        flowProperties.setThresholds(new HashMap<>());
        this.flowProperties = flowProperties;
        
        this.cxProperties = cxProperties;
        this.mergeResultEvaluator = mergeResultEvaluator;
        this.helperService = mock(HelperService.class);
        this.flowService = flowService;
        this.gitHubService = gitHubService;
        
        this.gitHubProperties = gitHubProperties;
        initGitHubProperties();
    }

    private void initGitHubProperties() {
        this.gitHubProperties.setCxSummary(false);
        this.gitHubProperties.setFlowSummary(false);
        this.gitHubProperties.setUrl("https://github.com/cxflowtestuser/CxConfigTests");
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setConfigAsCode("cx.config");
        this.gitHubProperties.setApiUrl("https://api.github.com/repos");
                                         
    }

    @Before("@CxConfigFeature")
    public void prepareServices() {
        initCxClientMock();
        scanResultsToInject = createFakeScanResults();
        initServices();
        initHelperServiceMock();
        initMockGitHubController();
    }

    @Given("github branch is {string} and threshods section is not set application.yml")
    public void setBranch(String branch){
        this.branch = branch;
        buildPullRequest();
    }

    @And("github branch is {string} with cx.config")
    public void setBranchAppSet(String branch){
        setBranch(branch);
    }
    public void buildPullRequest() {
        PullEvent pullEvent = new PullEvent();
        Repository repo = new Repository();
        repo.setName("CxConfigTests");
                      
        repo.setCloneUrl(gitHubProperties.getUrl());
        Owner owner = new Owner();
        owner.setName("");
        owner.setLogin("cxflowtestuser");
        repo.setOwner(owner);
        pullEvent.setRepository(repo);
        pullEvent.setAction("opened");
        PullRequest pullRequest = new PullRequest();
        pullRequest.setIssueUrl("");
        Head headBranch = new Head();
        headBranch.setRef(branch);
                
        pullRequest.setHead(headBranch);
        pullRequest.setBase(new Base());
        pullRequest.setStatusesUrl("");
        
        pullEvent.setPullRequest(pullRequest);
       
        try {
            String pullEventStr = mapper.writeValueAsString(pullEvent);

            gitHubControllerSpy.pullRequest(
                    pullEventStr,
                    "SIGNATURE",
                    "CX", "VB",
                    Arrays.asList(branch), null,
                    null,
                    null,
                    "VB",
                    "\\CxServer\\SP",
                    null,
                    "",
                    "default",
                    false,
                    null,
                    null,
                    null,
                    null,
                    null);

        } catch (JsonProcessingException e) {
            fail("Unable to parse " + pullEvent.toString());
        }
    }

    @Given("application.xml contains high thresholds {string} medium thresholds {string} and low thresholds {string}")
    public void thresholdForFindingsOfSeverityIs(String highCount, String mediumCount, String lowCount) {
        
        flowProperties.setThresholds(new HashMap<>());
        if(StringUtils.isEmptyOrNull(highCount) && StringUtils.isEmptyOrNull(mediumCount) && StringUtils.isEmptyOrNull(lowCount)){
            flowProperties.setThresholds(null);
        }else {
            if (!StringUtils.isEmptyOrNull(highCount)) {
                flowProperties.getThresholds().put(FindingSeverity.HIGH, Integer.parseInt(highCount));
            }
            if (!StringUtils.isEmptyOrNull(mediumCount)) {
                flowProperties.getThresholds().put(FindingSeverity.MEDIUM, Integer.parseInt(mediumCount));
            } 
            if (!StringUtils.isEmptyOrNull(lowCount)) {
                flowProperties.getThresholds().put(FindingSeverity.LOW, Integer.parseInt(lowCount));
            }
        }
    }
    
    
    @Given("application.xml contains filters section with filter type {string}")
    public void severityFilterIsSetTo(String filterType) {
        if(!StringUtils.isEmptyOrNull(filterType)) {
            String[] filterTypeArr ;
            if(filterType.contains(",")) {
                filterTypeArr = filterType.trim().split(",");
            }else{
                filterTypeArr = new String[1];
                filterTypeArr[0] = filterType;
            }
            for (String currfilterType: filterTypeArr) {
                setCurrentFilter(currfilterType);
            }
        }
    }

    private void setCurrentFilter(String currSeverity) {
        
        switch(currSeverity){
            case("severity"):
                List<String> severity = Arrays.asList(new String[]{FindingSeverity.HIGH.toString(), FindingSeverity.LOW.toString()});
                flowProperties.setFilterSeverity(severity);
                break;
            case("cwe"):
                List<String> cwe = Arrays.asList(new String[]{"anyOther1","anyOther2","anyOther3"});
                flowProperties.setFilterCwe(cwe);
                break;
            case("category"):
                List<String> category = Arrays.asList(new String[]{"anyOther1","anyOther2","anyOther3"});
                flowProperties.setFilterCategory(category);
                break;
            default:
                fail("Invalid Filter");
                break;
        }
        
    }
    

    @And("^(?:SAST detects )?(.*) findings of \"(.+)\" severity$")
    public void sastDetectsFindings(int expectedFindingCount, String severity) {
        addFindingsTo(scanResultsToInject, expectedFindingCount, severity);
    }

    private void addFindingsTo(ScanResults target, int count, String severityName) {
             Object summary = target.getAdditionalDetails().get(Constants.SUMMARY_KEY);
            assertTrue(summary instanceof Map);
            ((Map) summary).put(severityName, count);

    }

    @Then("CxFlow {string} the pull request")
    public void cxflowApprovesOrFailsThePullRequest(String approvesOrFails) throws InterruptedException {
        
        validateRequestByConfig(Boolean.FALSE);
        
        processScanResultsInCxFlow();

        boolean expectingApproval = approvesOrFails.equals("approves");
        verifyPullRequestState(expectingApproval);
    }


    @Then("CxFlow will return results as per the filter in cx.config")
    public void validateFilter() {
        validateRequestFilterByConfig();
    }

    private List<String> getFilter(List<Filter> filters, Filter.Type type) {

        List<String> filterByType = new ArrayList<>();

        if (filters == null || filters.isEmpty()) {
            return filterByType;
        }
        
        for(Filter filter: filters){
            if(filter.getType().equals(type)) {
                String value = filter.getValue();
                if (type.equals(type)) {
                    filterByType.add(value.toUpperCase(Locale.ROOT));
                }
            }
        }
        
        return filterByType;
    }
    private void validateRequestFilterByConfig() {
        
        List<String> filterSeverity = getFilter(request.getFilters(), Filter.Type.SEVERITY);
        List<String> filterCwe = getFilter(request.getFilters(), Filter.Type.CWE);
        List<String> filterCatergory = getFilter(request.getFilters(), Filter.Type.TYPE);
        
        boolean asExpected = false;
        switch(branch){
            case "test7":
                //Filter Severity High and Medium: 

                asExpected = filterSeverity.size() ==2 &&
                        filterSeverity.contains(FindingSeverity.HIGH.toString()) &&
                        filterSeverity.contains(FindingSeverity.MEDIUM.toString()) &&
                        filterCwe.isEmpty() && filterCatergory.isEmpty();
                break;
            case "test8":
                //Filter cwe: "79", "89"
                asExpected = filterCwe.size() ==2 &&
                        filterCwe.contains(CWE_79) &&
                        filterCwe.contains(CWE_89) &&
                        filterCatergory.isEmpty() && filterSeverity.isEmpty();
                break;
            case "test9":
                // filter category: "XSS_Reflected", "SQL_Injection"
                asExpected = filterCatergory.size() ==2 &&
                        filterCatergory.contains(XSS_REFLECTED) &&
                        filterCatergory.contains(SQL_INJECTION) &&
                        filterCwe.isEmpty() && filterSeverity.isEmpty();
                break;
            case "test10":
                // filter cwe:    "79", "89"
                // filter category:   "XSS_Reflected", "SQL_Injection"
                    
                asExpected = filterCwe.size() ==2 &&
                            filterCwe.contains(CWE_79) &&
                            filterCwe.contains(CWE_89) &&
                            filterCatergory.size() ==2 &&
                            filterCatergory.contains("XSS_REFLECTED") &&
                            filterCatergory.contains("SQL_INJECTION") &&
                            filterSeverity.isEmpty();
                break;
            case "test11":
                // filter filter severity: High, Medium
                // filter category:   "XSS_Reflected", "SQL_Injection"       
                asExpected =
                        filterSeverity.size() ==2 &&
                                filterSeverity.contains(FindingSeverity.HIGH.toString()) &&
                                filterSeverity.contains(FindingSeverity.MEDIUM.toString()) &&
                                filterCatergory.size() == 2 &&
                                filterCatergory.contains("XSS_REFLECTED") &&
                                filterCatergory.contains("SQL_INJECTION") &&
                                filterCwe.isEmpty();        
                break;
            default:
                fail("Invalid Branch");
                break;
        }

        if(!asExpected){
            fail("Invalid Filter for branch " + branch + ": " + flowProperties.getThresholds().toString());
        }
        
    }

   

    @Then("CxFlow {string} the pull request and cx.config truncates the data in application.yml")
    public void cxflowApprovesOrFailsThePullRequestOverride(String approvesOrFails) throws InterruptedException {

        validateRequestByConfig(Boolean.TRUE);

        processScanResultsInCxFlow();

        boolean expectingApproval = approvesOrFails.equals("approves");
        verifyPullRequestState(expectingApproval);
    }
    
    private void validateRequestByConfig(Boolean thresholdsOverrideTest) {
        boolean asExpected = false;
        switch(branch){
            case "test1":
                //High: 3  Medium: 8  Low: 15
                asExpected = flowProperties.getThresholds().get(FindingSeverity.HIGH) == 3 &&
                            flowProperties.getThresholds().get(FindingSeverity.MEDIUM) == 8 &&
                            flowProperties.getThresholds().get(FindingSeverity.LOW) == 15;
                break;
            case "test2":
                //High: not set  Medium: 8  Low: 15
                asExpected = flowProperties.getThresholds().get(FindingSeverity.HIGH) == null &&
                             flowProperties.getThresholds().get(FindingSeverity.MEDIUM) == 8 &&
                             flowProperties.getThresholds().get(FindingSeverity.LOW) == 15;
                break;
            case "test3":
                //High: 3  Medium: 8  Low: 15
                asExpected = flowProperties.getThresholds().get(FindingSeverity.HIGH) == 3 &&
                        flowProperties.getThresholds().get(FindingSeverity.MEDIUM) == null &&
                        flowProperties.getThresholds().get(FindingSeverity.LOW) == 15;
                break;
            case "test4":
            case "test5":
                if(Boolean.TRUE.equals(thresholdsOverrideTest)){
                    asExpected = flowProperties.getThresholds().get(FindingSeverity.HIGH) == 2 &&
                            flowProperties.getThresholds().get(FindingSeverity.MEDIUM) == 5 &&
                            flowProperties.getThresholds().get(FindingSeverity.LOW) == 10;
                }
                else {
                    asExpected = flowProperties.getThresholds() == null || flowProperties.getThresholds().isEmpty();
                }
                break;
            default:
                fail("Invalid Branch");
                break;
        }
        
        if(!asExpected){
            fail("Invalid Threshods for branch " + branch + ": " + flowProperties.getThresholds().toString());
        }
  
    }

    private void processScanResultsInCxFlow() throws InterruptedException{
        try {
            ScanRequest scanRequest = createScanRequest();

            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    scanRequest, 0, 0, null, null);

            task.get(1, TimeUnit.MINUTES);
        } catch (MachinaException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
        }
    }

    private void verifyPullRequestState(boolean expectingApproval) {
        Assert.assertNotNull("pullRequestWasApproved is not initialized", isPullRequestApproved);

        if (expectingApproval) {
            Assert.assertTrue("Expecting pull request to be approved, but it was failed", isPullRequestApproved);
        } else {
            Assert.assertFalse("Expecting pull request to be failed, but it was approved", isPullRequestApproved);
        }
    }

    private ScanRequest createScanRequest() {
        ScanRequest scanRequest = new ScanRequest();

        scanRequest.setProduct(ScanRequest.Product.CX);

        scanRequest.setBugTracker(BugTracker.builder().type(BugTracker.Type.GITHUBPULL).build());
        scanRequest.setMergeNoteUri(MERGE_NOTE_URL);
        
        HashMap<String, String> additionalMetdata = new HashMap<>();
        additionalMetdata.put("statuses_url", PULL_REQUEST_STATUSES_URL);
        
        scanRequest.setAdditionalMetadata(additionalMetdata);
        
        return scanRequest;
    }

    private void initMock(RestTemplate restTemplateMock) {
        Answer<ResponseEntity<String>> interceptor = new HttpRequestInterceptor();

        when(restTemplateMock.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), ArgumentMatchers.<Class<String>>any()))
                .thenAnswer(interceptor);
        
     }

    
    private void initCxClientMock() {
        try {
            ScanResultsAnswerer answerer = new ScanResultsAnswerer();
            when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenAnswer(answerer);
        } catch (CheckmarxException e) {
            Assert.fail("Error initializing mock." + e);
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


    private void initHelperServiceMock() {
        HelperServiceAnswerer answerer = new HelperServiceAnswerer();
        when(helperService.isBranch2Scan(any(), anyList())).thenAnswer(answerer);
        when(helperService.getShortUid()).thenReturn("123456");
    }

    private void initMockGitHubController() {
        doNothing().when(gitHubControllerSpy).verifyHmacSignature(any(), any());
    }
    
    private void initServices() {

        //gitHubControllerSpy is a spy which will run real methods.
        //It will connect to a real github repository toread a real cx.config file
        //And thus it will work with real gitHubService
        this.gitHubControllerSpy = spy(new GitHubController(gitHubProperties,
                flowProperties,
                cxProperties,
                null,
                flowService,
                helperService,
                gitHubService));
        
        //results service will be a Mock and will work with gitHubService Mock
        //and will not not connect to any external 
        initResultsServiceMock();
    }

    private void initResultsServiceMock() {
        
        RestTemplate restTemplateMock = mock(RestTemplate.class);

        initMock(restTemplateMock);
        GitHubService gitHubServiceMock = new GitHubService(restTemplateMock,
                gitHubProperties,
                flowProperties,
                mergeResultEvaluator);

        this.resultsService = new ResultsService(
                cxClientMock,
                null,
                null,
                null,
                gitHubServiceMock,
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
                isPullRequestApproved = wasApproved(interceptedRequest);
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
    
    /**
     * Returns scan results as if they were produced by SAST.
     */
    private class HelperServiceAnswerer implements Answer<Boolean> {
        @Override
        public Boolean answer(InvocationOnMock invocation) {
            request = invocation.getArgument(0);
            return false;
        }
    }
}
