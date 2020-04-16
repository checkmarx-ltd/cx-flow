package com.checkmarx.flow.cucumber.component.cxconfig;

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

    private static final ObjectMapper mapper = new ObjectMapper();

    private final CxClient cxClientMock;
    private final GitHubService gitHubService;
    private GitHubController gitHubControllerSpy;
    
    private final MergeResultEvaluator mergeResultEvaluator;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final GitHubProperties gitHubProperties;
    private final HelperService helperService;
    private ScanResults scanResultsToInject;

    private ResultsService resultsService;
    private Boolean pullRequestWasApproved;
    private Filter filter;
    private FlowService flowService;
    private String branch;


    public CxConfigSteps( FlowProperties flowProperties, GitHubService gitHubService,
                         CxProperties cxProperties, GitHubProperties gitHubProperties, MergeResultEvaluator mergeResultEvaluator, FlowService flowService) {

        this.cxClientMock = mock(CxClient.class);;

        flowProperties.setThresholds(new HashMap<>());
        this.flowProperties = flowProperties;
        
        this.cxProperties = cxProperties;
        this.mergeResultEvaluator = mergeResultEvaluator;
        this.helperService = mock(HelperService.class);;
        this.flowService = flowService;
        this.gitHubService = gitHubService;
        
        this.gitHubProperties = gitHubProperties;
        initGitHubProperties(gitHubProperties);
    }

    private void initGitHubProperties(GitHubProperties gitHubProperties) {
        this.gitHubProperties.setCxSummary(false);
        this.gitHubProperties.setFlowSummary(false);
        this.gitHubProperties.setUrl("https://github.com/cxflowtestuser/CxConfigTests");
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setToken("1086823f09df45273157a8531bad59ca03e46322");
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
        this.branch = branch;
        buildPullRequest();
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

       ObjectMapper mapper = new ObjectMapper();
       
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
    
    
//    @And("severity filter is set to {string}")
//    public void severityFilterIsSetTo(String severity) {
//        filter = new Filter(Filter.Type.SEVERITY, severity);
//    }
//
//    @And("no severity filter is specified")
//    public void noSeverityFilterIsSpecified() {
//        filter = null;
//    }

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
        
        validateRequestByConfig(Boolean.FALSE);
        
        processScanResultsInCxFlow();

        boolean expectingApproval = approvesOrFails.equals("approves");
        verifyPullRequestState(expectingApproval);
    }

    
    @Then("CxFlow {string} the pull request and cx.config truncates the data in application.yml")
    public void cxflowApprovesOrFailsThePullRequestOverride(String approvesOrFails) {

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
                if(thresholdsOverrideTest){
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

        scanRequest.setAdditionalMetadata(new HashMap<String, String>() {{
            put("statuses_url", PULL_REQUEST_STATUSES_URL);
        }});
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
    private class HelperServiceAnswerer implements Answer<Boolean> {
        @Override
        public Boolean answer(InvocationOnMock invocation) {
            return false;
        }
    }
}
