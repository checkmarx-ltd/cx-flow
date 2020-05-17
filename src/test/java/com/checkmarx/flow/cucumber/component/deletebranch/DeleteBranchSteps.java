package com.checkmarx.flow.cucumber.component.deletebranch;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;

import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.sdk.exception.CheckmarxException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
        
import com.checkmarx.flow.controller.GitHubController;

import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;

import com.checkmarx.sdk.service.CxClient;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.test.context.SpringBootTest;


import java.util.LinkedList;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class DeleteBranchSteps {

    private static final int EXISTING_PROJECT_ID = 20;
    private static final String TEAM = "SOME_TEAM";
    private static final String PROJECT_NAME = "VB_3845-test1";
    private static final String PRESET = "Default Preset";
    private static final String BRANCH = "branch";
    public static String GITHUB_USER = "cxflowtestuser";
    private final CxClient cxClientMock;
    private final GitHubService gitHubService;
    private GitHubController gitHubControllerSpy;
    private final ObjectMapper mapper = new ObjectMapper();

    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final GitHubProperties gitHubProperties;
    private final HelperService helperService;
    private final SastScanner sastScanner;

    private ProjectNameGenerator projectNameGeneratorSpy;
    private FlowService flowServiceSpy;
    private String branch;
    
    private Boolean deleteCalled = null;
    private String repoName;

    private int actualProjectId;
    private int projectId;
    private String trigger;
    private String calculatedProjectName;
    
    public DeleteBranchSteps(FlowProperties flowProperties, GitHubService gitHubService,
                             CxProperties cxProperties, GitHubProperties gitHubProperties, SastScanner sastScanner) {

        this.cxClientMock = mock(CxClient.class);
        
        this.flowProperties = flowProperties;
        
        this.cxProperties = cxProperties;

        this.helperService = mock(HelperService.class);

        this.gitHubService = gitHubService;
        
        this.gitHubProperties = gitHubProperties;

        this.sastScanner = sastScanner;

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
        actualProjectId = Constants.UNKNOWN_INT;
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

    private class ScanResultsAnswerer implements Answer {

        @Override
        public Object answer(InvocationOnMock invocation) {
            actualProjectId = invocation.getArgument(0);
            deleteCalled = true;
            return null;
        }
    }

    @Given("GitHub repoName is {string}")
    public void setRepoName(String repoName){
        this.repoName = repoName;
        initGitHubProperties();
    }

    @And("SAST delete API will be called for project {string}")
    public void validateProjectName(String projectName){
        assertEquals(calculatedProjectName, projectName);
    }
    
    @And("GitHub webhook is configured for delete branch or tag")
    public void nothingToImpl(){}
    
    @And("no exception will be thrown")
    public void validateNoException(){}
    
    @And("github trigger can be branch or tag {string}")
    public void setTrigger(String trigger){
        this.trigger = trigger;
    }
    
    @And("CxFlow will call the SAST delete API only if trigger is branch")
    public void callDelete(){
        
        buildDeleteRequest(trigger);

        if(trigger.equals(BRANCH)){
            assertEquals(deleteCalled, Boolean.TRUE);
            assertEquals(actualProjectId,EXISTING_PROJECT_ID );
        }else{
            assertEquals(deleteCalled, Boolean.FALSE);
            assertEquals(actualProjectId,Constants.UNKNOWN_INT);
        }
    }
    
    @And("github branch is {string} and it is set {string} application.yml")
    public void setBranch(String branch, String set_in_app){
        LinkedList<String> branches = new LinkedList<>();
        if(Boolean.parseBoolean(set_in_app)){
            branches.add(branch);
            flowProperties.setBranches(branches);
        }else{
            flowProperties.setBranches(branches);
        }
        this.branch = branch;
    }
    
    @And("a project {string} {string} in SAST")
    public void setProjectId(String projectName, String exists){
        if(Boolean.parseBoolean(exists)){
            this.projectId = EXISTING_PROJECT_ID;
        }else{
            this.projectId = Constants.UNKNOWN_INT;
        }
        when(cxClientMock.getProjectId(anyString(),anyString())).thenReturn(projectId);
    }
    
    @And("CxFlow will call or not call the SAST delete API based on the fact whether the project {string} or not in SAST")
    public void checkIfDeleteMethodIsCalled(String methodCalled)        
    {
        buildDeleteRequest(BRANCH);
        
        if(Boolean.parseBoolean(methodCalled)){
            assertEquals(deleteCalled, Boolean.TRUE);
            assertEquals(actualProjectId,EXISTING_PROJECT_ID );
        }else{
            assertEquals(deleteCalled, Boolean.FALSE);
            assertEquals(actualProjectId,Constants.UNKNOWN_INT);
        }
    }
    
    public void buildDeleteRequest(String refType) {
        DeleteEvent deleteEvent = new DeleteEvent();
        Repository repo = new Repository();
        repo.setName(repoName);

        repo.setCloneUrl(gitHubProperties.getUrl());
        Owner owner = new Owner();
        owner.setName(GITHUB_USER);
        owner.setLogin(GITHUB_USER);
        repo.setOwner(owner);
        deleteEvent.setRepository(repo);
        deleteEvent.setRefType(refType);
        
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
                 
    }

    private void initMockGitHubController() {
        doNothing().when(gitHubControllerSpy).verifyHmacSignature(any(), any());
        
    }
    
    private void initServices() {

        projectNameGeneratorSpy = spy(new ProjectNameGenerator(helperService, cxProperties, null));

        try {
            initProjectNameGeneratorSpy(projectNameGeneratorSpy);
        } catch (MachinaException e) {
            fail(e.getMessage());
        }

        //gitHubControllerSpy is a spy which will run real methods.
        //It will connect to a real github repository to read a real cx.config file
        //And thus it will work with real gitHubService
        this.gitHubControllerSpy = spy(new GitHubController(gitHubProperties,
                flowProperties,
                cxProperties,
                null,
                flowServiceSpy,
                helperService,
                gitHubService, sastScanner));
        
    }

    private void initProjectNameGeneratorSpy(ProjectNameGenerator projectNameGenerator) throws MachinaException {
        ProjectNameGeneratorAnswered answered = new ProjectNameGeneratorAnswered();
        doAnswer(answered).when(projectNameGenerator).determineProjectName(any());
    }

    private class ProjectNameGeneratorAnswered implements Answer {

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            try {
                calculatedProjectName = (String)invocation.callRealMethod();
            } catch (Throwable throwable) {
                fail(throwable.getMessage());
            }

            return calculatedProjectName;
        }
    }
}
