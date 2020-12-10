package com.checkmarx.flow.cucumber.component.deletebranch;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.ADOController;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.controller.*;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.BugTrackersDto;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.azure.*;
import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.dto.github.Repository;
import com.checkmarx.flow.sastscanning.ScanRequestConverter;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.sdk.service.CxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

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
    private static final String AZURE_WEBHOOK_TOKEN = "cxflow:1234";
    private static final String AZURE_WEBHOOK_TOKEN_AUTH = "Basic Y3hmbG93OjEyMzQ=";
    private static final String AZURE_PUSH_EVENT_TYPE = "git.push";
    private static final String AZURE_DELETED_BRANCH_OBJ_ID = "0000000000000000000000000000000000000000";
    private static final String GIT_DEFAULT_BRANCH = "refs/heads/master";
    private static final int SCAN_ID_EXISTING_SCAN_NOT_EXIST = -1;
    private final CxService cxClientMock;
    private final GitHubService gitHubService;
    private final GitHubAppAuthService gitHubAppAuthService;
    private GitHubController gitHubControllerSpy;
    private ADOController adoControllerSpy;

    private static final ObjectMapper mapper = new ObjectMapper();
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final GitHubProperties gitHubProperties;
    private HelperService helperService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;
    private final ADOProperties adoProperties;
    private final EmailService emailService;
    private final ScmConfigOverrider scmConfigOverrider;
    private final BugTrackerEventTrigger bugTrackerEventTrigger;
    private final GitAuthUrlGenerator gitAuthUrlGenerator;

    private final ADOService adoServiceMock;
    private final ResultsService resultsServiceMock;
    private CxConfig cxConfig;
    private String adoRepo;
    private String githubBranch;
    
    private Boolean deleteCalled;
    private String repoName;

    private String trigger;
    private String calculatedProjectName;

    public DeleteBranchSteps(FlowProperties flowProperties, GitHubService gitHubService,
                             GitHubAppAuthService gitHubAppAuthService, CxProperties cxProperties, GitHubProperties gitHubProperties, FilterFactory filterFactory,
                             ConfigurationOverrider configOverrider, ADOProperties adoProperties, EmailService emailService,
                             BugTrackerEventTrigger bugTrackerEventTrigger, ScmConfigOverrider scmConfigOverrider,
                             GitAuthUrlGenerator gitAuthUrlGenerator) {
        this.gitHubAppAuthService = gitHubAppAuthService;

        this.filterFactory = filterFactory;
        this.configOverrider = configOverrider;
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
        this.gitHubService = gitHubService;
        this.gitHubProperties = gitHubProperties;
        this.adoProperties = adoProperties;
        this.cxConfig = new CxConfig();
        this.emailService = emailService;
        this.bugTrackerEventTrigger = bugTrackerEventTrigger;

        this.adoServiceMock = mock(ADOService.class);
        this.resultsServiceMock = mock(ResultsService.class);
        this.cxClientMock = mock(CxService.class);
        CxScannerService  cxScannerService = new CxScannerService(cxProperties, null, flowProperties,cxClientMock, null);
        this.helperService = mock(HelperService.class, Mockito.withSettings().useConstructor(flowProperties, cxScannerService, null));
        this.scmConfigOverrider = scmConfigOverrider;
        this.gitAuthUrlGenerator = gitAuthUrlGenerator;
    }

    private void initGitHubProperties() {
        this.gitHubProperties.setCxSummary(false);
        this.gitHubProperties.setFlowSummary(false);
        this.gitHubProperties.setUrl("https://github.com/" + GITHUB_USER + "/" + repoName);
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setApiUrl("https://api.github.com/repos");
    }

    private void initAdoProperties() {
        this.adoProperties.setNamespace("TestNameSpace");
        this.adoProperties.setWebhookToken(AZURE_WEBHOOK_TOKEN);
    }

    @Before("@DeleteBranchFeature")
    public void prepareServices() throws CheckmarxException {
        deleteCalled = Boolean.FALSE;
        trigger = BRANCH_REF_TYPE;
        flowProperties.setBugTracker(BugTracker.Type.CUSTOM.toString());
        initMockers();
        initServices();
        initMockGitHubController();
    }

    private void initMockers() throws CheckmarxException {

        ScanResultsAnswerer answerer = new ScanResultsAnswerer();
        doAnswer(answerer).when(cxClientMock).deleteProject(any());

        when(helperService.getShortUid()).thenReturn("12345");
        when(helperService.getCxTeam(any())).thenReturn(TEAM);
        when(helperService.getCxProject(any())).thenCallRealMethod();
        when(helperService.getPresetFromSources(any())).thenReturn(PRESET);
        when(helperService.isBranchProtected(anyString(), anyList(), any())).thenCallRealMethod();
        when(helperService.isBranch2Scan(any(), anyList())).thenCallRealMethod();

        when(cxClientMock.getTeamId(anyString(),anyString())).thenReturn(TEAM);
        when(cxClientMock.getTeamId(anyString())).thenReturn(TEAM);
        when(cxClientMock.getScanIdOfExistingScanIfExists(anyInt())).thenReturn(SCAN_ID_EXISTING_SCAN_NOT_EXIST);
        when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenReturn(new ScanResults());

        when(adoServiceMock.getCxConfigOverride(any(), anyString())).thenReturn(cxConfig);
        when(resultsServiceMock.publishCombinedResults(any(), any())).thenReturn(null);
    }

    private class ScanResultsAnswerer implements Answer<Object> {
        @Override
        public Object answer(InvocationOnMock invocation) {
            deleteCalled = true;
            return null;
        }
    }

    @Given("Azure repo name is {string}")
    public void setAdoRepoName(String repo){
        adoRepo = repo;
        initAdoProperties();
    }

    @And("Azure branch is {}")
    public void setAdoBranch(String branch){

    }

    @Given("GitHub repo name is {string}")
    public void setGithubRepoName(String repoName){
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
    public void setGithubBranch(String branch) {
        this.githubBranch = branch;
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
    
    @And("the {string} branch is {} as determined by application.yml")
    public void theBranchIsSpecifiedAsProtected(String branch, String protectedOrNot) {
        List<String> protectedBranches = flowProperties.getBranches();
        if (Boolean.parseBoolean(protectedOrNot)) {
            protectedBranches.add(branch);
        } else {
            protectedBranches.remove(branch);
        }
    }

    @And("the Azure properties is {} by application.yml")
    public void theBranchIsSpecifiedAsProtected(boolean deleteBranch) {
        adoProperties.setDeleteCxProject(deleteBranch);
    }

    @And("config-as-code in azure default branch include Cx-project {}")
    public void setAzureConfigAsCodeProject(String cxProject) {
        if (!cxProject.equals("none")){
            cxConfig.setProject(cxProject);
        }
    }

    @When("GitHub notifies cxFlow that a {string} branch/ref was deleted")
    public void githubNotifiesCxFlowThatABranchWasDeleted(String deletedBranch) {
        githubBranch = deletedBranch;
        sendGithubDeleteEvent();
    }

    @When("Azure notifies cxFlow that a {string} branch was deleted")
    public void azureNotifiesCxFlowThatABranchWasDeleted(String deletedBranch) {
        sendAzureDeleteEvent(deletedBranch);
    }


    @Then("CxFlow will {} the SAST delete API for the {string} project")
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
    
    private void sendGithubDeleteEvent() {
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
        deleteEvent.setRef(githubBranch);
                
        try {
            String deleteEventStr = mapper.writeValueAsString(deleteEvent);

            gitHubControllerSpy.deleteBranchRequest(
                    deleteEventStr,"SIGNATURE", "CX", null, null, TEAM );

        } catch (JsonProcessingException e) {
            fail("Unable to parse " + deleteEvent.toString());
        }
    }

    public void sendAzureDeleteEvent(String deletedBranch) {

        String meaningless_string = "some_string";
        com.checkmarx.flow.dto.azure.PushEvent pushEvent = new com.checkmarx.flow.dto.azure.PushEvent();
        pushEvent.setEventType(AZURE_PUSH_EVENT_TYPE);

        Project_ project = new Project_();
        project.setId(meaningless_string);
        project.setBaseUrl("https://dev.azure.com/OrgName/");

        ResourceContainers resourceContainers = new ResourceContainers();
        resourceContainers.setProject(project);

        pushEvent.setResourceContainers(resourceContainers);
        RefUpdate refUpdate = new RefUpdate();
        refUpdate.setName("refs/heads/" + deletedBranch);
        refUpdate.setOldObjectId(meaningless_string);
        refUpdate.setNewObjectId(AZURE_DELETED_BRANCH_OBJ_ID);

        Resource resource = new Resource();
        resource.setRefUpdates(Collections.singletonList(refUpdate));
        resource.setStatus(meaningless_string);
        resource.setUrl(meaningless_string);
        com.checkmarx.flow.dto.azure.Repository repo = new com.checkmarx.flow.dto.azure.Repository();
        repo.setId(meaningless_string);
        repo.setName(adoRepo);
        repo.setUrl(meaningless_string);
        repo.setRemoteUrl(meaningless_string);

        repo.setDefaultBranch(GIT_DEFAULT_BRANCH);
        Project pr = new Project();
        pr.setId(meaningless_string);
        pr.setName(meaningless_string);
        repo.setProject(pr);
        resource.setRepository(repo);

        pushEvent.setResource(resource);
        ControllerRequest controllerRequest = new ControllerRequest();
        AdoDetailsRequest adoRequest = new AdoDetailsRequest();
        adoControllerSpy.pushRequest(pushEvent, AZURE_WEBHOOK_TOKEN_AUTH, null, controllerRequest, adoRequest);
    }


    private void initMockGitHubController() {
        doNothing().when(gitHubControllerSpy).verifyHmacSignature(any(), any(), any());
    }

    private void initServices() {
        CxScannerService cxScannerService = new CxScannerService(cxProperties,null, flowProperties, cxClientMock, null );

        ProjectNameGenerator projectNameGeneratorSpy = spy(new ProjectNameGenerator(helperService, cxScannerService));
            initProjectNameGeneratorSpy(projectNameGeneratorSpy);
 
        ScanRequestConverter scanRequestConverter = new ScanRequestConverter(helperService, flowProperties, gitHubService, null, null, null, null,cxClientMock,cxProperties);
        SastScanner sastScanner = new SastScanner(null,  cxProperties, flowProperties, null,  projectNameGeneratorSpy, cxClientMock, new BugTrackersDto(emailService, bugTrackerEventTrigger,gitHubService, null, null, null, null));
        List<VulnerabilityScanner> scanners= new LinkedList<>();
        scanners.add(sastScanner);
        
        FlowService flowServiceSpy = spy(new FlowService(scanners, projectNameGeneratorSpy, resultsServiceMock));
        
        //gitHubControllerSpy is a spy which will run real methods.
        //It will connect to a real github repository to read a real cx.config file
        //And thus it will work with real gitHubService
        this.gitHubControllerSpy = spy(new GitHubController(gitHubProperties,
                flowProperties,
                null,
                flowServiceSpy,
                helperService,
                gitHubService,
                gitHubAppAuthService,
                filterFactory,
                configOverrider,
                null,
                gitAuthUrlGenerator));

        this.adoControllerSpy = spy(new ADOController(adoProperties,
                flowProperties,
                null,
                flowServiceSpy,
                helperService,
                filterFactory,
                configOverrider,
                adoServiceMock,
                scmConfigOverrider,
                gitAuthUrlGenerator));
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
        return calculatedProjectName;
        }
    }


