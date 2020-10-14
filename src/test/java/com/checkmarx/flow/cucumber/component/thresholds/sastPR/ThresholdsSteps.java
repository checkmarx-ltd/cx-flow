package com.checkmarx.flow.cucumber.component.thresholds.sastPR;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.*;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.cli.IntegrationTestContext;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxProject;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.sdk.service.CxService;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final String CLI_COMMAND = "--project  --cx-project=test --app=MyApp --branch=master --repo-name=CLI-Tests --namespace=CxFlow --blocksysexit";
    private static final String MERGE_NOTE_URL = "merge note url stub";
    private static  final String DEFAULT_SEVERITY_HIGH = "HIGH";
    private static final int DEFAULT_FINDINGS_COUNT = 10;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String STATUSES_URL_KEY = "statuses_url";
    private final IntegrationTestContext testContext;

    private final CxService cxClientMock;
    private final RestTemplate restTemplateMock;
    private final ThresholdValidator thresholdValidator;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final GitHubProperties gitHubProperties;
    private final ADOProperties adoProperties;
    private final EmailService emailService;
    private final GitHubAppAuthService gitHubAppAuthService;
    private final ScmConfigOverrider scmConfigOverrider;
    private final ScaProperties scaProperties;

    private ScanResults scanResultsToInject;
    private ResultsService resultsService;
    private Boolean pullRequestWasApproved;
    private Filter filter;
    private boolean thresholdsSectionExist;

    public ThresholdsSteps(IntegrationTestContext testContext, CxService cxClientMock, RestTemplate restTemplateMock, FlowProperties flowProperties, ADOProperties adoProperties,
                           CxProperties cxProperties, GitHubProperties gitHubProperties, ThresholdValidator thresholdValidator,
                           EmailService emailService, GitHubAppAuthService gitHubAppAuthService, ScmConfigOverrider scmConfigOverrider, ScaProperties scaProperties) {

        this.cxClientMock = cxClientMock;
        this.restTemplateMock = restTemplateMock;
        this.testContext = testContext;

        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
        this.gitHubAppAuthService = gitHubAppAuthService;
        this.scaProperties = scaProperties;
        flowProperties.setThresholds(new HashMap<>());
        gitHubProperties.setCxSummary(false);
        this.gitHubProperties = gitHubProperties;
        this.adoProperties = adoProperties;

        this.thresholdValidator = thresholdValidator;
        this.emailService = emailService;
        this.scmConfigOverrider = scmConfigOverrider;
    }

    @Before("@ThresholdsFeature")
    public void prepareServices() {
        log.info("setting scan engine to CxSAST");
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(CxProperties.CONFIG_PREFIX));

        initMock(cxClientMock);
        initMock(restTemplateMock);
        scanResultsToInject = createFakeScanResults();
        resultsService = createResultsService();
        this.adoProperties.setBlockMerge(true);
        this.adoProperties.setErrorMerge(true);
        this.gitHubProperties.setBlockMerge(true);
        this.gitHubProperties.setErrorMerge(true);
        pullRequestWasApproved = null;
    }

    @Given("thresholds section {} in cxflow configuration")
    public void setThresholdsSectionExist(boolean thresholdsExist) {
        thresholdsSectionExist = thresholdsExist;
    }

    @And("thresholds {} by scan findings")
    public void setThresholds(boolean thresholdsExceeded) {
        if (thresholdsSectionExist){
            if (thresholdsExceeded) {
                thresholdForFindingsOfSeverityIs(DEFAULT_SEVERITY_HIGH, String.valueOf(DEFAULT_FINDINGS_COUNT - 1));
            } else {
                thresholdForFindingsOfSeverityIs(DEFAULT_SEVERITY_HIGH, String.valueOf(DEFAULT_FINDINGS_COUNT + 1));
            }
        }
        else{ resetThresholds(); }
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
    public void resetThresholds() {
        flowProperties.setThresholds(null);
        scaProperties.setThresholdsSeverity(null);
        scaProperties.setThresholdsScore(null);
    }

    @When("cxflow called with scan cli command")
    public void runCxFlowFromCommandLine() {

        flowProperties.setBugTracker(BugTracker.Type.NONE.toString());

        Throwable exception = null;
        try {
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), CLI_COMMAND);
        } catch (Throwable e) {
            exception = e;
        }
        testContext.setCxFlowExecutionException(exception);
    }

    @Then("cxflow should exit with the correct {}")
    public void validateExitCode(int expectedExitCode) {
        Throwable exception = testContext.getCxFlowExecutionException();

        Assert.assertNotNull("Expected an exception to be thrown.", exception);
        Assert.assertEquals(InvocationTargetException.class, exception.getClass());

        Throwable targetException = ((InvocationTargetException) exception).getTargetException();
        Assert.assertTrue(targetException instanceof ExitThrowable);

        int actualExitCode = ((ExitThrowable) targetException).getExitCode();

        Assert.assertEquals("The expected exist code did not match",
                expectedExitCode, actualExitCode);
    }

    @And("severity filter is set to {string}")
    public void severityFilterIsSetTo(String severity) {
        filter = new Filter(Filter.Type.SEVERITY, severity);
    }

    @And("scan findings are {} after filter")
    public void severityFilterIsSetTo(boolean findingsPresentedAfterFilter) {
        if (findingsPresentedAfterFilter){
            addFindingsTo(scanResultsToInject, DEFAULT_FINDINGS_COUNT, DEFAULT_SEVERITY_HIGH);
        }else{
            scanResultsToInject = createFakeScanResults();
        }
    }

    @And("break-build property is set to {}")
    public void setBreakBuildProperty(boolean breakBuild){
        flowProperties.setBreakBuild(breakBuild);
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

    @Then("CxFlow {string} the pull request on GitHub")
    public void cxflowApprovesOrFailsGithubPullRequest(String approvesOrFails) {
        processScanResultsInCxFlow(true);

        boolean expectingApproval = approvesOrFails.equals("approves");
        verifyPullRequestState(expectingApproval);
    }

    @And ("blockMerge is {string} and errorMerge is {string}")
    public void setFlags(String blockMergeStr, String errorMergeStr) {
        boolean blockMerge  = Boolean.parseBoolean(blockMergeStr);
        boolean errorMerge  = Boolean.parseBoolean(errorMergeStr);
        
        if(!(blockMerge && errorMerge)){
            pullRequestWasApproved = true;             
        }
        this.adoProperties.setBlockMerge(blockMerge);
        this.adoProperties.setErrorMerge(errorMerge);
        this.gitHubProperties.setBlockMerge(blockMerge);
        this.gitHubProperties.setErrorMerge(errorMerge);
    }

    @Then("CxFlow {string} the pull request on Azure")
    public void cxflowApprovesOrFailsAzurePullRequest(String approvesOrFails) {
        processScanResultsInCxFlow(false);

        boolean expectingApproval = approvesOrFails.equals("approves");
        verifyPullRequestState(expectingApproval);
    }
    private void processScanResultsInCxFlow(boolean isGitHub) {
        try {

            ScanRequest scanRequest = createScanRequest(isGitHub);
     
            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    scanRequest, 0, 0, null, null);

            task.get(1, TimeUnit.MINUTES);
        } catch (MachinaException | InterruptedException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
            Thread.currentThread().interrupt();
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

    private ScanRequest createScanRequest(boolean isGitHub) {
        ScanRequest scanRequest = new ScanRequest();
        BugTracker.Type issueTruckerType;

        Map<String, String> additionalMetadata = new HashMap<>();
        additionalMetadata.put(STATUSES_URL_KEY, PULL_REQUEST_STATUSES_URL);

        if (isGitHub) {
            issueTruckerType = BugTracker.Type.GITHUBPULL;
            scanRequest.setMergeNoteUri(MERGE_NOTE_URL);
            scanRequest.setRepoType(ScanRequest.Repository.GITHUB);
        } else {
            issueTruckerType = BugTracker.Type.ADOPULL;
            additionalMetadata.put("status_id", Integer.toString(1));
            scanRequest.setRepoType(ScanRequest.Repository.ADO);
    }
        BugTracker issueTracker = BugTracker.builder().type(issueTruckerType).build();
        scanRequest.setBugTracker(issueTracker);

        scanRequest.setProduct(ScanRequest.Product.CX);


        scanRequest.setAdditionalMetadata(additionalMetadata);
        return scanRequest;
    }


    private void initMock(RestTemplate restTemplateMock) {
        Answer<ResponseEntity<String>> interceptor = new HttpRequestInterceptor();

        ResponseEntity<String> sendingPostRequest = restTemplateMock.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), ArgumentMatchers.<Class<String>>any());
  
        when(sendingPostRequest).thenAnswer(interceptor);
        when(restTemplateMock.exchange(anyString(),eq(HttpMethod.GET),any(), any(Class.class) ))
                .thenReturn(ResponseEntity.ok("{}"));
    }

    private void initMock(CxClient cxClientMock) {
        try {
            CxProject cxProject = CxProject.builder().id(1).name("testproject").isPublic(false).customFields(Collections.EMPTY_LIST).build();

            ScanResultsAnswerer answerer = new ScanResultsAnswerer();
            when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenAnswer(answerer);

            when(cxClientMock.getProject(anyInt())).thenReturn(cxProject);

            when(cxClientMock.getTeamId(anyString())).thenReturn("1");
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

        ADOService adoService = new ADOService(restTemplateMock,
                adoProperties,
                flowProperties,
                cxScannerService,
                scmConfigOverrider,
                thresholdValidator
                );
        
        return new ResultsService(
                cxScannerService,
                null,
                null,
                null,
                gitService,
                null,
                null,
                adoService,
                emailService);
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
            log.info("HttpRequestInterceptor url: " + url);
            if (url.contains(PULL_REQUEST_STATUSES_URL)) {
                HttpEntity<String> interceptedRequest = invocation.getArgument(2);
                log.info("interceptedRequest: " + interceptedRequest);
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
                log.info("state:" + state);
                if (state.equals("success") || state.equals("succeeded")) {
                    result = true;
                } else if (state.equals("failure") || state.equals("failed")) {
                    result = false;
                }
            } catch (JsonProcessingException e) {
                Assert.fail("Error parsing request. " + e);
            }catch(Exception e){
                Assert.fail("Unexpected error: " + e);
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
