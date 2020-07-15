package com.checkmarx.flow.cucumber.integration.azure.publishing.github2ado;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.cucumber.integration.azure.publishing.AzureDevopsClient;
import com.checkmarx.flow.cucumber.integration.azure.publishing.githubflow.ScanResultsBuilder;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@SpringBootTest(classes = {CxFlowApplication.class, AzureDevopsClient.class})
@Slf4j
public class Github2AdoSteps {
    public static final String GITHUB_USER = "cxflowtestuser";
    public static final String AZURE = "Azure";

    private final CxClient cxClientMock;
    private final GitHubService gitHubService;
    private final ADOProperties adoProperties;
    private IssueService issueService;
    private GitHubController gitHubControllerSpy;
    private final ObjectMapper mapper = new ObjectMapper();

    private FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final GitHubProperties gitHubProperties;
    private final HelperService helperService;
    private final EmailService emailService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;

    private ScanResults scanResultsToInject;
    
    private ResultsService resultsService;
    private AzureDevopsClient azureDevopsClient;

    private final FlowService flowService;
    private String branch;
    private ScanRequest request;

    private String repoName;

    @Autowired
    private ApplicationContext applicationContext;
    
    public Github2AdoSteps(FlowProperties flowProperties, GitHubService gitHubService,
                           CxProperties cxProperties, GitHubProperties gitHubProperties,
                           ConfigurationOverrider configOverrider, FlowService flowService, ADOProperties adoProperties, FilterFactory filterFactory, AzureDevopsClient azureDevopsClient, EmailService emailService) {
        this.filterFactory = filterFactory;

        this.cxClientMock = mock(CxClient.class);

        this.flowProperties = flowProperties;

        this.cxProperties = cxProperties;

        this.helperService = mock(HelperService.class);
        this.flowService = flowService;
        this.gitHubService = gitHubService;
        this.azureDevopsClient = azureDevopsClient;
        this.gitHubProperties = gitHubProperties;
        this.adoProperties = adoProperties;
        this.configOverrider = configOverrider;
        this.emailService = emailService;
        initGitHubProperties();
    }

    private void initGitHubProperties() {
        this.gitHubProperties.setCxSummary(false);
        this.gitHubProperties.setFlowSummary(false);
        this.gitHubProperties.setUrl("https://github.com/" + GITHUB_USER + "/");
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setApiUrl("https://api.github.com/repos");
                                         
    }

    @Before("@Github2AdoFeature")
    public void prepareServices() {
        this.flowProperties.setBugTracker(AZURE);
        this.flowProperties.setBugTrackerImpl(Collections.singletonList(AZURE));
        this.adoProperties.setUrl("https://dev.azure.com/");
        issueService = new IssueService(flowProperties);
        issueService.setApplicationContext(applicationContext);
        scanResultsToInject = createFakeResults();
        initCxClientMock();
        initServices();
        initHelperServiceMock();
        initMockGitHubController();
    }
    

    @Given("application.yml contains the Azure project {string} and Asure namespace {string}")
    public void setProjectAndNamespace(String inputProject, String inputNamespace){
        if(!StringUtils.isBlank(inputProject)) {
            adoProperties.setProjectName(inputProject);
        }else{
            adoProperties.setProjectName("");
        }
        if(!StringUtils.isBlank(inputNamespace)) {
            adoProperties.setNamespace(inputNamespace);
        }else{
            adoProperties.setNamespace("");
        }
    }

    @And("commit or merge pull request is performed in github repo {string} and branch {string}")
    public void setBranchAppSet(String repoName, String branch){
        this.branch = branch;
        this.repoName = repoName;
        gitHubProperties.setUrl(gitHubProperties.getUrl().concat(repoName));
        
    }

    @And("SAST scan produces high and medium results")
    public void createScanResults() throws InterruptedException {
        buildPushRequest();
        processScanResultsInCxFlow();
    }
  
    
    public void buildPushRequest() {
        PushEvent pushEvent = new PushEvent();
        Repository repo = new Repository();
        repo.setName(repoName);
                      
        repo.setCloneUrl(gitHubProperties.getUrl());
        Owner owner = new Owner();
        owner.setName(GITHUB_USER);
        owner.setLogin(GITHUB_USER);
        repo.setOwner(owner);
        pushEvent.setRepository(repo);
        pushEvent.setCommits(new LinkedList<>());
        
        Pusher pusher = new Pusher();
        pusher.setEmail("some@email");
        pushEvent.setPusher(pusher);


        try {
            String pullEventStr = mapper.writeValueAsString(pushEvent);
            ControllerRequest request = ControllerRequest.builder()
                    .application(repoName)
                    .branch(Collections.singletonList(branch))
                    .project(repoName)
                    .team("\\CxServer\\SP")
                    .assignee("")
                    .preset("default")
                    .build();

            gitHubControllerSpy.pushRequest(pullEventStr, "SIGNATURE", "CX", request);

        } catch (JsonProcessingException e) {
            fail("Unable to parse " + pushEvent.toString());
        }
    }
    
    @Then("CxFlow will create appropriate tickets in project {string} in namespace {string} in Azure")
    public void validateIssues(String project, String namespace){
        try {
            azureDevopsClient.init(namespace,project);
            assertTrue(azureDevopsClient.projectExists());
            assertEquals(2, azureDevopsClient.getIssues().size());
            
            azureDevopsClient.deleteProjectIssues();
            
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
    
    @And("project {string} exists in Azure under namespace {string}")
    public void validateProjectName(String project, String namespace) {

        try {
            azureDevopsClient.init(namespace, project);
            azureDevopsClient.ensureProjectExists();
            azureDevopsClient.deleteProjectIssues();
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }


    private void processScanResultsInCxFlow() throws InterruptedException{
        try {

            CompletableFuture<ScanResults> task = resultsService.processScanResultsAsync(
                    request, 0, 0, null, null);
            
             task.get(1, TimeUnit.MINUTES);

        } catch (MachinaException | ExecutionException | TimeoutException e) {
            String message = "Error processing scan results.";
            log.error(message, e);
            Assert.fail(message);
        }
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
        //It will connect to a real github repository 
        //And thus it will work with real gitHubService
        this.gitHubControllerSpy = spy(new GitHubController(gitHubProperties,
                flowProperties,
                cxProperties,
                null,
                flowService,
                helperService,
                gitHubService,
                null,
                filterFactory,
                configOverrider));
        
        //results service will be a Mock and will work with gitHubService Mock
        //and will not not connect to any external 
        initResultsServiceMock();
    }

    private void initResultsServiceMock() {
        

        this.resultsService = spy(new ResultsService(
                cxClientMock,
                null,
                null,
                issueService,
                gitHubService,
                null,
                null,
                null,
                emailService,
                cxProperties,
                flowProperties));
    }

    private ScanResults createFakeResults() {
        ScanResults result = new ScanResults();
        
        result.setScanSummary(new CxScanSummary());
        
        Map<String, Object> details = new HashMap<>();
        details.put(Constants.SUMMARY_KEY, new HashMap<>());

        result.setAdditionalDetails(details);
        
        result.setXIssues(ScanResultsBuilder.get2XIssues());

        return result;
    }

    
    /**
     * Returns scan results as if they were produced by SAST.
     */
    private class HelperServiceAnswerer implements Answer<Boolean> {
        @Override
        public Boolean answer(InvocationOnMock invocation) {
            request = invocation.getArgument(0);
            request.setBranch(branch);
            return false;
        }
    }

}
