package com.checkmarx.flow.cucumber.component.deletebranch;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.sastscanning.ScanRequestConverter;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import lombok.extern.slf4j.Slf4j;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class DeleteBranchSteps {

    private static final int EXISTING_PROJECT_ID = 20;
    private static final String TEAM = "SOME_TEAM";
    private static final String PROJECT_NAME = "VB_3845-test1";
    private static final String PRESET = "Default Preset";
    private static final String BRANCH_REF_TYPE = "branch";
    private static final String GITHUB_USER = "cxflowtestuser";
    private final CxClient cxClientMock;
    private final GitHubService gitHubService;
    private GitHubController gitHubControllerSpy;
    private static final ObjectMapper mapper = new ObjectMapper();

    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final GitHubProperties gitHubProperties;
    private final HelperService helperService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;

    private String branch;
    
    private Boolean deleteCalled;
    private String repoName;

    private String trigger;
    private String calculatedProjectName;

    public DeleteBranchSteps(FlowProperties flowProperties, GitHubService gitHubService,
                             CxProperties cxProperties, GitHubProperties gitHubProperties, FilterFactory filterFactory,
                             ConfigurationOverrider configOverrider) {
        this.filterFactory = filterFactory;

        this.configOverrider = configOverrider;

        this.cxClientMock = mock(CxClient.class);

        this.flowProperties = flowProperties;

        this.cxProperties = cxProperties;

        this.helperService = mock(HelperService.class);

        this.gitHubService = gitHubService;

        this.gitHubProperties = gitHubProperties;
    }

    private void initGitHubProperties() {
        this.gitHubProperties.setCxSummary(false);
        this.gitHubProperties.setFlowSummary(false);
        this.gitHubProperties.setUrl("https://github.com/" + GITHUB_USER + "/" + repoName);
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setApiUrl("https://api.github.com/repos");
    }

    @Before("@DeleteBranchFeature")
    public void prepareServices() {
        deleteCalled = Boolean.FALSE;
        trigger = BRANCH_REF_TYPE;
        initCxClientMock();
        initHelperServiceMock();
        initServices();
        initMockGitHubController();
    }

    private void initCxClientMock() {
        ScanResultsAnswerer answerer = new ScanResultsAnswerer();
        doAnswer(answerer).when(cxClientMock).deleteProject(any());
        try {
            when(cxClientMock.getTeamId(anyString(),anyString())).thenReturn(TEAM);
            when(cxClientMock.getTeamId(anyString())).thenReturn(TEAM);
        } catch (CheckmarxException e) {
            fail(e.getMessage());
        }
    }

    private class ScanResultsAnswerer implements Answer<Object> {
        @Override
        public Object answer(InvocationOnMock invocation) {
            deleteCalled = true;
            return null;
        }
    }

    @Given("GitHub repo name is {string}")
    public void setRepoName(String repoName){
        this.repoName = repoName;
        initGitHubProperties();
    }

    @And("no exception will be thrown")
    public void validateNoException() {
        // If we have arrived here, no exception was thrown.
    }
    
    @And("GitHub trigger is {string}")
    public void setTrigger(String trigger){
        this.trigger = trigger;
    }
    
    @And("GitHub branch is {string}")
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    @And("a project {string} {string} in SAST")
    public void setProjectId(String projectName, String exists){
        int projectId;
        if (exists.equals("exists") || Boolean.parseBoolean(exists)) {
            projectId = EXISTING_PROJECT_ID;
        }else{
            projectId = Constants.UNKNOWN_INT;
        }
        when(cxClientMock.getProjectId(anyString(), any())).thenReturn(projectId);
    }
    
    @And("the {string} branch is {string} as determined by application.yml")
    public void theBranchIsSpecifiedAsProtected(String branch, String protectedOrNot) {
        List<String> protectedBranches = flowProperties.getBranches();
        if (Boolean.parseBoolean(protectedOrNot)) {
            protectedBranches.add(branch);
        } else {
            protectedBranches.remove(branch);
        }
    }
        
    @When("GitHub notifies cxFlow that a {string} branch/ref was deleted")
    public void githubNotifiesCxFlowThatABranchWasDeleted(String deletedBranch) {
        branch = deletedBranch;
        sendDeleteEvent();
    }

    @Then("CxFlow will {string} the SAST delete API for the {string} project")
    public void cxflowWillNotCallTheSASTDeleteAPI(String willCall, String projectName) {
        boolean expectingCall = Boolean.parseBoolean(willCall);
        verifyDeleteApiCall(expectingCall);

        if (expectingCall) {
            assertEquals("Wrong project name in SAST delete API call.", projectName, calculatedProjectName);
        }
    }

    private void verifyDeleteApiCall(boolean expectingCall) {
        if (expectingCall) {
            assertEquals(Boolean.TRUE, deleteCalled);
        }else{
            assertEquals(Boolean.FALSE, deleteCalled);
        }
    }
    
    private void sendDeleteEvent() {
        DeleteEvent deleteEvent = new DeleteEvent();
        Repository repo = new Repository();
        repo.setName(repoName);

        repo.setCloneUrl(gitHubProperties.getUrl());
        Owner owner = new Owner();
        owner.setName(GITHUB_USER);
        owner.setLogin(GITHUB_USER);
        repo.setOwner(owner);
        deleteEvent.setRepository(repo);
        deleteEvent.setRefType(trigger);
        deleteEvent.setRef(branch);
                
        try {
            String deleteEventStr = mapper.writeValueAsString(deleteEvent);

            gitHubControllerSpy.deleteBranchRequest(
                    deleteEventStr,"SIGNATURE", "CX", null, null, TEAM );

        } catch (JsonProcessingException e) {
            fail("Unable to parse " + deleteEvent.toString());
        }
    }

     private void initHelperServiceMock() {
         when(helperService.getShortUid()).thenReturn("123456");
         when(helperService.getCxTeam(any())).thenReturn(TEAM);
         when(helperService.getCxProject(any())).thenReturn(PROJECT_NAME);
         when(helperService.getPresetFromSources(any())).thenReturn(PRESET);
        when(helperService.isBranchProtected(anyString(), anyList(), any())).thenCallRealMethod();
    }

    private void initMockGitHubController() {
        doNothing().when(gitHubControllerSpy).verifyHmacSignature(any(), any(), any());
    }
    
    private void initServices() {
        ProjectNameGenerator projectNameGeneratorSpy = spy(new ProjectNameGenerator(helperService, cxProperties, null));
            initProjectNameGeneratorSpy(projectNameGeneratorSpy);
        
        ScanRequestConverter scanRequestConverter = new ScanRequestConverter(helperService, cxProperties, cxClientMock, flowProperties, gitHubService, null,null);
        SastScanner sastScanner = new SastScanner(null, cxClientMock, helperService, cxProperties, flowProperties, null, null, scanRequestConverter, null, projectNameGeneratorSpy);
        List<VulnerabilityScanner> scanners= new LinkedList<>();
        scanners.add(sastScanner);
        
        FlowService flowServiceSpy = spy(new FlowService(scanners, projectNameGeneratorSpy, null));
        
        //gitHubControllerSpy is a spy which will run real methods.
        //It will connect to a real github repository to read a real cx.config file
        //And thus it will work with real gitHubService
        this.gitHubControllerSpy = spy(new GitHubController(gitHubProperties,
                flowProperties,
                cxProperties,
                null,
                flowServiceSpy,
                helperService,
                gitHubService,
                null,
                sastScanner,
                filterFactory,
                configOverrider,
                null));
        
    }

    private void initProjectNameGeneratorSpy(ProjectNameGenerator projectNameGenerator) {
        doAnswer(this::interceptProjectName).when(projectNameGenerator).determineProjectName(any());
    }

    public Object interceptProjectName(InvocationOnMock invocation) {
            try {
                calculatedProjectName = (String)invocation.callRealMethod();
            } catch (Throwable throwable) {
                fail(throwable.getMessage());
            }
        return null;
        }
    }
