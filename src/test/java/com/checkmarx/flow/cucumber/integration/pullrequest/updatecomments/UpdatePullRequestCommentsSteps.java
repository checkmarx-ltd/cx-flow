package com.checkmarx.flow.cucumber.integration.pullrequest.updatecomments;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.controller.ADOController;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.controller.GitLabController;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.azure.*;
import com.checkmarx.flow.dto.github.PullEvent;
import com.checkmarx.flow.dto.github.Repository;
import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.dto.gitlab.LastCommit;
import com.checkmarx.flow.dto.gitlab.MergeEvent;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.scanner.CxClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpClientErrorException;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;


@SpringBootTest(classes = {CxFlowApplication.class, UpdatePullRequestConfiguration.class})
@Slf4j
public class UpdatePullRequestCommentsSteps {

    @Autowired
    private CxClient cxClientMock;
    public static final int COMMENTS_POLL_INTERVAL = 5;
    private static final String GITHUB_PROJECT_NAME = "vb_test_pr_comments";
    private static final String GITHUB_PR_BASE_URL = "https://api.github.com/repos/cxflowtestuser/" + GITHUB_PROJECT_NAME;
    private static final String GITHUB_PR_ID = "6";
    private static final String ADO_PR_ID = "90";
    public static final String PULL_REQUEST_COMMENTS_URL = GITHUB_PR_BASE_URL + "/issues/"+ GITHUB_PR_ID + "/comments";
    private static final String GITHUB_URL = "https://github.com/cxflowtestuser/" + GITHUB_PROJECT_NAME;

    private static final String GITLAB_BASE_URL = "https://gitlab.com/api/v4";
    private static final String GITLAB_PROJECT_NAME = "cxflow-integration-gitlab-tests";
    private static final String GITLAB_URL = "https://gitlab.com/cxflowtestuser/" + GITLAB_PROJECT_NAME;
    private static final String GITLAB_PROJECT_ID = "23910442";
    private static final String GITLAB_MERGE_REQUEST_ID = "4";
    public static final String MERGE_REQUEST_NOTES_URL =
            GITLAB_BASE_URL + "/projects/" + GITLAB_PROJECT_ID +
                    "/merge_requests/" + GITLAB_MERGE_REQUEST_ID +"/notes";

    private static final String ADO_PR_COMMENTS_URL = "https://dev.azure.com/CxNamespace/d50fc6e5-a5ab-4123-9bc9-ccb756c0bf16/_apis/git/repositories/a89a9d2f-ab67-4bda-9c56-a571224c2c66/pullRequests/" + ADO_PR_ID + "/threads";
    private static final String filePath = "sample-sast-results/3-findings-filter-script-test.xml";
    private final GitHubService gitHubService;
    private final GitLabService gitLabService;
    private final ADOService adoService;
    private final GitHubController gitHubControllerSpy;
    private final ADOController adoControllerSpy;
    private final GitLabController gitLabControllerSpy;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GitHubProperties gitHubProperties;
    private final GitLabProperties gitLabProperties;
    private final HelperService helperService;
    private final ScaProperties scaProperties;
    private SourceControlType sourceControl;
    private FlowProperties flowProperties;
    private CxProperties cxProperties;
    private String branchGitHub;
    private ScannerType scannerType;

    private File sastFile = TestUtils.getFileFromResource(filePath);
    private FilterConfiguration filterMedium = getSeverityFilter("Medium");
    private FilterConfiguration filterLow = getSeverityFilter("Low");

    public UpdatePullRequestCommentsSteps(GitHubService gitHubService,
                                          GitLabService gitLabService,
                                          GitHubProperties gitHubProperties,
                                          GitHubController gitHubController, ADOService adoService,
                                          ADOController adoController,
                                          GitLabController gitLabControllerSpy,
                                          GitLabProperties gitLabProperties,
                                          FlowProperties flowProperties, CxProperties cxProperties,
                                          ScaProperties scaProperties) throws IOException {
        this.gitLabService = gitLabService;
        this.gitLabControllerSpy =  Mockito.spy(gitLabControllerSpy);
        this.gitLabProperties = gitLabProperties;
        this.helperService = mock(HelperService.class);
        this.gitHubService = gitHubService;
        this.gitHubProperties = gitHubProperties;
        this.gitHubControllerSpy = Mockito.spy(gitHubController);
        this.adoService = adoService;
        this.adoControllerSpy = Mockito.spy(adoController);
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
        this.scaProperties = scaProperties;
    }

    private class ScanResultsAnswerer implements Answer<ScanResults> {

        private FilterConfiguration filterConfiguration;

        private FilterConfiguration switchFilterConfiguration(){
            return filterConfiguration == filterMedium ? filterLow : filterMedium ;
        }

        @Override
        public ScanResults answer(InvocationOnMock invocation) throws CheckmarxException {

            filterConfiguration = switchFilterConfiguration();
            ScanResults results = cxClientMock.getReportContent(sastFile, filterConfiguration);
            results.setScanSummary(new CxScanSummary());

            results.setAdditionalDetails(Collections.singletonMap(Constants.SUMMARY_KEY, new HashMap<>()));

            return results;
        }
    }


    @Before
    public void initConfiguration(){
        initGitHubProperties();
        initGitlabProperties();
        ScaCommonSteps.initSCAConfig(scaProperties);
        flowProperties.getBranches().add("udi-tests-2");
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList("sast"));
        cxProperties.setOffline(true);
        initGitHubControllerSpy();
        initHelperServiceMock();
        setBranches();
        initCxClientMock();
    }

    private void initCxClientMock() {
        try {
            ScanResultsAnswerer answerWithExistingScanResult = new ScanResultsAnswerer();
            when(cxClientMock.getReportContentByScanId(anyInt(), any())).thenAnswer(answerWithExistingScanResult);
            when(cxClientMock.getScanIdOfExistingScanIfExists(anyInt())).thenReturn(-1);
            when(cxClientMock.getTeamId(anyString())).thenReturn("teamId");
            when(cxClientMock.getReportContent(sastFile, filterLow)).thenCallRealMethod();
            when(cxClientMock.getReportContent(sastFile, filterMedium)).thenCallRealMethod();
        } catch (CheckmarxException e) {
            Assert.fail("Error initializing mock." + e);
        }
    }

    @Given("different filters configuration is set")
    public void setConfigAsCodeFilters() {
        /*
        This is required to test 'comment updated' flow
        However mockito mockers are initialized in the beginning of the test, including the answerer and all the fields and properties inside for example (sastFile, filterConfiguration)
        Using ScanResultsAnswerer::switchFilterConfiguration to change filter configuration in the between steps in same test scenario
         */
    }

    @And("no comments on pull request")
    public void deletePRComments() throws IOException {
        if (sourceControl.equals(SourceControlType.GITHUB)) {
            deleteGitHubComments();
        } else if (sourceControl.equals(SourceControlType.ADO)) {
            deleteADOComments();
        }  else if (sourceControl.equals(SourceControlType.GITLAB)) {
            deleteGitLabComments();
        }
    }

    private void setBranches() {
        branchGitHub = "pr-comments-tests";
        // ADO repo and branch defined by the ADO_PR_COMMENTS_URL
    }

    @After
    public void cleanUp() throws IOException {
        try {
            if (sourceControl.equals(SourceControlType.GITHUB)) {
                deleteGitHubComments();
            } else if (sourceControl.equals(SourceControlType.ADO)) {
                deleteADOComments();
            }  else if (sourceControl.equals(SourceControlType.GITLAB)) {
                deleteGitLabComments();
            }
        }catch(HttpClientErrorException e){
            //the comments have already been deleted
        }
    }

    @Given("scanner is set to {string}")
    public void setScanner(String scanner) {
        if ("sast".equals(scanner)) {
            scannerType = ScannerType.SAST;
            flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList("sast"));
        } else if ("sca".equals(scanner)) {
            scannerType = ScannerType.SCA;
            flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList("sca"));
        } else if ("both".equals(scanner)) {
            scannerType = ScannerType.BOTH;
            flowProperties.setEnabledVulnerabilityScanners(Arrays.asList("sca", "sast"));
        }
        else {
            throw new IllegalArgumentException("Wrong scanner type: " + scanner);
        }
    }

    @Given("source control is GitHub")
    public void scGitHub() {
        sourceControl = SourceControlType.GITHUB;
    }

    @Given("source control is ADO")
    public void scAdo() {
        sourceControl = SourceControlType.ADO;
    }

    @Given("source control is Gitlab")
    public void sourceControlIsGitlab() {
        sourceControl = SourceControlType.GITLAB;
    }

    private void deleteADOComments() throws IOException {
        List<RepoComment> adoComments = getRepoComments();
        for (RepoComment rc: adoComments) {
            adoService.deleteComment(rc.getCommentUrl(), getBasicRequest());
        }
    }

    private void deleteGitHubComments() throws IOException {
        List<RepoComment> comments = getRepoComments();
        for (RepoComment comment: comments) {
            gitHubService.deleteComment(comment.getCommentUrl(), getBasicRequest());
        }
    }
    private void deleteGitLabComments() throws IOException {
        List<RepoComment> comments = getRepoComments();
        for (RepoComment comment: comments) {
            gitLabService.deleteComment(comment.getCommentUrl(), getBasicRequest());
        }
    }

    private List<RepoComment> getRepoComments() throws IOException {
        if (sourceControl.equals(SourceControlType.GITHUB)) {
            return gitHubService.getComments(getBasicRequest());
        }
        else if (sourceControl.equals(SourceControlType.ADO)){
            return adoService.getComments(ADO_PR_COMMENTS_URL, getBasicRequest());
        }
        else if (sourceControl.equals(SourceControlType.GITLAB)) {
            return gitLabService.getComments(getBasicRequest());
        }
        throw new IllegalArgumentException("Unknown source control: " + sourceControl);
    }

    @When("pull request arrives to CxFlow")
    public void sendPullRequest() {
        if (sourceControl.equals(SourceControlType.GITHUB)) {
            buildGitHubPullRequest();
        } else if (sourceControl.equals(SourceControlType.ADO)) {
            buildADOPullRequestEvent();
        } else if (sourceControl.equals(SourceControlType.GITLAB)) {
            buildGitlabPullRequestEvent();
        }
    }

    @Then("Wait for comments")
    public void waitForNewComments() {
        log.info("waiting for new comments. scanner type {}", scannerType);
        int minutesToWait = scannerType == ScannerType.BOTH ? 3 : 2;
        Awaitility.await()
                .atMost(Duration.ofMinutes(minutesToWait))
                .pollInterval(Duration.ofSeconds(COMMENTS_POLL_INTERVAL))
                .until(this::areThereCommentsAtAll);
    }

    @Then("verify new comments")
    public void verifyNewComments() throws IOException {
        log.info("verifying comments. scanner type: {} source control type: {}", scannerType, sourceControl);

        int expectedNumOfComments = getExpectedNumOfNewComments();
        List<RepoComment> comments = getRepoComments();
        Assert.assertEquals("Wrong number of comments", expectedNumOfComments, comments.size());
        comments.forEach(c -> Assert.assertTrue("Comment is not new (probably updated)", isCommentNew(c)));

        log.info("Found the correct comments in pull request !!");
    }

    private int getExpectedNumOfNewComments() {
        switch (scannerType) {
            case SCA:
            case SAST:
            case BOTH:
                return 2;
        }
        throw  new RuntimeException("Wrong scanner type");
    }

    @Then("Wait for updated comment")
    public void waitForUpdatedComment() {
        Awaitility.await()
                .pollInterval(Duration.ofSeconds(COMMENTS_POLL_INTERVAL))
                .atMost(Duration.ofSeconds(125))
                .until(this::isThereUpdatedComment);

        log.info("Found the correct comments in pull request !!");
    }

    private boolean areThereCommentsAtAll() throws IOException {
        List<RepoComment> comments = getRepoComments();

        log.info("found {} comments in {}", comments.size(), sourceControl);

        if (scannerType == ScannerType.SCA) {
            if (comments.size() < 1) {
                return false;
            }
        } else if (scannerType == ScannerType.SAST || scannerType == ScannerType.BOTH) {
            if(comments.size() <= 1) {
                return false;
            }
        } else {
            throw new IllegalArgumentException("Wrong Scanner Type: " + scannerType.name());
        }

        return areThereCorrectComments(comments, scannerType);
    }

    private boolean areThereCorrectComments(List<RepoComment> comments, ScannerType sct){

        if (sct.equals(ScannerType.BOTH)) {
            boolean foundScaAndSast = false;
            boolean foundScanStarted = false;
            for (RepoComment comment : comments) {
                if (PullRequestCommentsHelper.isScanStartedComment(comment.getComment())) {
                    log.info("BOTH: found pull request 'scan started' comment");
                    foundScanStarted = true;
                } else if (PullRequestCommentsHelper.isSastAndScaComment(comment.getComment())) {
                    log.info("BOTH: found pull request Sca&Sast comment");
                    foundScaAndSast = true;
                }
            }
            return foundScaAndSast && foundScanStarted;
        }
        else if (sct.equals(ScannerType.SAST)) {
            boolean foundSast = false;
            boolean foundScanStarted = false;
            for (RepoComment comment : comments) {
                if (PullRequestCommentsHelper.isSastFindingsComment(comment.getComment())) {
                    log.info("SAST: found pull request sast comment");
                    foundSast = true;
                } else if (PullRequestCommentsHelper.isScanStartedComment(comment.getComment())) {
                    log.info("SAST: found pull request 'scan started' comment");
                    foundScanStarted = true;
                }
            }
            return foundSast && foundScanStarted;
        }
        else if (sct.equals(ScannerType.SCA)) {
            boolean foundSCA = false;
            boolean foundScanStarted = false;
            for (RepoComment comment : comments) {
                if (PullRequestCommentsHelper.isScaComment(comment.getComment())) {
                    log.info("SCA: found pull request comment");
                    foundSCA = true;
                }
                else if (PullRequestCommentsHelper.isScanStartedComment(comment.getComment())) {
                    log.info("SAST: found pull request 'scan started' comment");
                    foundScanStarted = true;
                }
            }
            return foundSCA && foundScanStarted;
        }
        throw new IllegalArgumentException("Wrong scanner type: " + sct.name());
    }

    private boolean isThereUpdatedComment() throws IOException {
        List<RepoComment> comments = getRepoComments();
        log.info("isThereUpdatedComment: found {} comments", comments.size());
        for (RepoComment comment: comments) {
            if (isCommentUpdated(comment)) {
                log.info("isThereUpdatedComment: True");
                return true;
            }
        }
        log.info("isThereUpdatedComment: False");
        return false;
    }

    private boolean isCommentNew(RepoComment comment) {
        return comment.getUpdateTime().equals(comment.getCreatedAt());
    }

    private boolean isCommentUpdated(RepoComment comment) {
        return comment.getUpdateTime().after(comment.getCreatedAt());
    }

    private void initGitHubControllerSpy() {
        doNothing().when(gitHubControllerSpy).verifyHmacSignature(any(), any(), any());
    }

    private void initHelperServiceMock() {
        when(helperService.isBranch2Scan(any(), anyList())).thenReturn(true);
        when(helperService.getShortUid()).thenReturn("123456");
    }

    private ScanRequest getBasicRequest() {
        if (sourceControl.equals(SourceControlType.GITLAB)) {
            return ScanRequest.builder()
                    .mergeNoteUri(MERGE_REQUEST_NOTES_URL)
                    .repoProjectId(Integer.parseInt(GITLAB_PROJECT_ID))
                    .additionalMetadata(new HashMap<String, String>() {{
                        put("merge_id", GITLAB_MERGE_REQUEST_ID);
                    }})
                    .build();
        } else
            return ScanRequest.builder()
                    .mergeNoteUri(PULL_REQUEST_COMMENTS_URL)
                    .build();
    }

    private void initGitHubProperties() {
        this.gitHubProperties.setCxSummary(false);
        this.gitHubProperties.setFlowSummary(false);
        this.gitHubProperties.setUrl(GITHUB_URL);
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setApiUrl("https://api.github.com/repos");
    }

    private void initGitlabProperties() {
        this.gitLabProperties.setWebhookToken("1234");
        this.gitLabProperties.setUrl(GITLAB_URL);
        this.gitLabProperties.setApiUrl("https://gitlab.com/api/v4");
        this.gitLabProperties.setFalsePositiveLabel("false-positive");
        this.gitLabProperties.setBlockMerge(true);
    }


    public void buildGitHubPullRequest() {
        PullEvent pullEvent = new PullEvent();
        Repository repo = new Repository();
        repo.setName("vb_test_udi");

        repo.setCloneUrl(gitHubProperties.getUrl());
        Owner owner = new Owner();
        owner.setName("");
        owner.setLogin("cxflowtestuser");

        Repo r = new Repo();
        r.setOwner(owner);

        repo.setOwner(owner);
        pullEvent.setRepository(repo);
        pullEvent.setAction("opened");
        PullRequest pullRequest = new PullRequest();
        pullRequest.setIssueUrl("");
        Head headBranch = new Head();
        headBranch.setRef(branchGitHub);
        headBranch.setRepo(r);

        pullRequest.setHead(headBranch);
        pullRequest.setBase(new Base());
        pullRequest.setStatusesUrl("");
        pullRequest.setIssueUrl(GITHUB_PR_BASE_URL + "/issues/" + GITHUB_PR_ID);

        pullEvent.setPullRequest(pullRequest);

        try {
            String pullEventStr = mapper.writeValueAsString(pullEvent);
            ControllerRequest controllerRequest = new ControllerRequest();
            controllerRequest.setApplication("VB");
            controllerRequest.setBranch(Collections.singletonList(branchGitHub));
            controllerRequest.setProject("VB");
            controllerRequest.setTeam("\\CxServer\\SP");
            controllerRequest.setPreset("default");
            controllerRequest.setIncremental(false);
            gitHubControllerSpy.pullRequest(
                    pullEventStr,
                    "SIGNATURE",
                    "CX",
                    controllerRequest
            );

        } catch (JsonProcessingException e) {
            fail("Unable to parse " + pullEvent);
        }
    }


    public void buildADOPullRequestEvent() {
        com.checkmarx.flow.dto.azure.PullEvent pullEvent = new com.checkmarx.flow.dto.azure.PullEvent();
        pullEvent.setEventType("git.pullrequest.updated");
        pullEvent.setId("4519989c-c157-4bf8-9651-e94b8d0fca27");
        pullEvent.setSubscriptionId("25aa3b80-54ed-4b26-976a-b74f94940852");
        pullEvent.setPublisherId("tfs");

        Project_ project = new Project_();
        project.setId("3172109f-8bcb-4f21-a8f7-4f94d4a825b0");
        project.setBaseUrl("https://dev.azure.com/OrgName/");

        ResourceContainers resourceContainers = new ResourceContainers();
        resourceContainers.setProject(project);

        pullEvent.setResourceContainers(resourceContainers);
        Resource resource = new Resource();
        resource.setStatus("active");
        resource.setSourceRefName("refs/heads/master");
        resource.setTargetRefName("refs/heads/udi-tests-2");
        resource.setUrl("https://dev.azure.com/CxNamespace/d50fc6e5-a5ab-4123-9bc9-ccb756c0bf16/_apis/git/repositories/a89a9d2f-ab67-4bda-9c56-a571224c2c66/pullRequests/" + ADO_PR_ID);
        com.checkmarx.flow.dto.azure.Repository repo = new com.checkmarx.flow.dto.azure.Repository();
        repo.setId("a89a9d2f-ab67-4bda-9c56-a571224c2c66");
        repo.setName("AdoPullRequestTests");
        repo.setUrl("https://dev.azure.com/CxNamespace/d50fc6e5-a5ab-4123-9bc9-ccb756c0bf16/_apis/git/repositories/a89a9d2f-ab67-4bda-9c56-a571224c2c66");
        repo.setRemoteUrl("https://CxNamespace@dev.azure.com/CxNamespace/AdoPullRequestTests/_git/AdoPullRequestTests");
        repo.setSshUrl("git@ssh.dev.azure.com:v3/CxNamespace/AdoPullRequestTests/AdoPullRequestTests");
        repo.setWebUrl("https://dev.azure.com/CxNamespace/AdoPullRequestTests/_git/AdoPullRequestTests");
        Project pr = new Project();
        pr.setId("d50fc6e5-a5ab-4123-9bc9-ccb756c0bf16");
        pr.setName("AdoPullRequestTests");
        repo.setProject(pr);
        resource.setRepository(repo);

        pullEvent.setResource(resource);
        ControllerRequest controllerRequest = new ControllerRequest();
        controllerRequest.setProject("AdoPullRequestTests-master");
        controllerRequest.setTeam("\\CxServer\\SP");
        AdoDetailsRequest adoRequest = new AdoDetailsRequest();
        adoControllerSpy.pullRequest(pullEvent,"Basic Y3hmbG93OjEyMzQ=", null, controllerRequest, adoRequest);
    }

    private void buildGitlabPullRequestEvent() {
        MergeEvent mergeEvent = new MergeEvent();
        mergeEvent.setObjectKind("merge_request");
        mergeEvent.setUser(com.checkmarx.flow.dto.gitlab.User.builder()
                                   .name("cxflowtestuser")
                                   .username("cxflowtestuser")
                                   .avatarUrl("https://secure.gravatar.com/avatar/5b0716952104a8b0b496af18a335f1d2?s=80&d=identicon")
                                   .build());
        mergeEvent.setProject(com.checkmarx.flow.dto.gitlab.Project.builder()
                                      .id(23910442).name("CxFlow Integration GitLab Tests")
                                      .webUrl("https://gitlab.com/cxflowtestuser/cxflow-integration-gitlab-tests")
                                      .gitSshUrl("git@gitlab.com:cxflowtestuser/cxflow-integration-gitlab-tests.git")
                                      .gitHttpUrl("https://gitlab.com/cxflowtestuser/cxflow-integration-gitlab-tests.git")
                                      .namespace("cxflowtestuser")
                                      .visibilityLevel(0)
                                      .pathWithNamespace("cxflowtestuser/cxflow-integration-gitlab-tests")
                                      .defaultBranch("master")
                                      .homepage("homepage")
                                      .url("git@gitlab.com:cxflowtestuser/cxflow-integration-gitlab-tests.git")
                                      .sshUrl("git@gitlab.com:cxflowtestuser/cxflow-integration-gitlab-tests.git")
                                      .httpUrl("https://gitlab.com/cxflowtestuser/cxflow-integration-gitlab-tests.git")
                                      .build());
        mergeEvent.setRepository(com.checkmarx.flow.dto.gitlab.Repository.builder()
                                         .name("CxFlow Integration GitLab Tests")
                                         .url("git@gitlab.com:cxflowtestuser/cxflow-integration-gitlab-tests.git")
                                         .description("")
                                         .homepage("https://gitlab.com/cxflowtestuser/cxflow-integration-gitlab-tests")
                                         .build());
        com.checkmarx.flow.dto.gitlab.Target target = new com.checkmarx.flow.dto.gitlab.Target();
        target.setDefaultBranch("master");
        mergeEvent.setObjectAttributes(com.checkmarx.flow.dto.gitlab.ObjectAttributes.builder()
                                               .id(86014571).targetBranch("master").sourceBranch("cxflow-test").sourceProjectId(23910442)
                                               .authorId(7362071).title("Update README.md").createdAt("2021-01-25 14:32:47 UTC")
                                               .updatedAt("2021-01-25 14:32:47 UTC").state("opened").mergeStatus("unchecked")
                                               .targetProjectId(Integer.parseInt(GITLAB_PROJECT_ID))
                                               .iid(Integer.parseInt(GITLAB_MERGE_REQUEST_ID))
                                               .description("")
                                               .workInProgress(false)
                                               .target(target)
                                               .lastCommit(new LastCommit().withId("fa907029c049b781f961e452a375d606402102a6"))
                                               .action("open")
                                               .build());
        ControllerRequest controllerRequest = new ControllerRequest();
        controllerRequest.setProject("cxflow-integration-gitlab-tests-Cxflow-test");
        controllerRequest.setTeam("\\CxServer\\SP");
        gitLabControllerSpy.mergeRequest(mergeEvent, "1234", null, controllerRequest);
    }

    private FilterConfiguration getSeverityFilter(String filter){
        return FilterConfiguration.fromSimpleFilters(Collections.singletonList(new Filter(Filter.Type.SEVERITY, filter)));
    }

    enum SourceControlType {
        GITHUB,
        ADO,
        GITLAB
    }

    enum ScannerType {
        SAST,
        SCA,
        BOTH
    }
}