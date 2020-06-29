package com.checkmarx.flow.cucumber.integration.pullrequest.updatecomments;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.ADOController;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.azure.Project;
import com.checkmarx.flow.dto.azure.Resource;
import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.service.ADOService;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class UpdatePullRequestCommentsSteps {


    public static final int COMMENTS_POLL_INTERVAL = 5;
    private static final String GIT_PROJECT_NAME = "vb_test_pr_comments";
    private static final String GITHUB_PR_BASE_URL = "https://api.github.com/repos/cxflowtestuser/" + GIT_PROJECT_NAME;
    public static final String PULL_REQUEST_COMMENTS_URL = GITHUB_PR_BASE_URL + "/issues/5/comments";
    private static final String GIT_URL = "https://github.com/cxflowtestuser/" + GIT_PROJECT_NAME;

    //private static final String ADO_PR_URL = "https://dev.azure.com/CxNamespace/d50fc6e5-a5ab-4123-9bc9-ccb756c0bf16/_apis/git/repositories/a89a9d2f-ab67-4bda-9c56-a571224c2c66/pullRequests/67";
    private static final String ADO_PR_COMMENTS_URL = "https://dev.azure.com/CxNamespace/d50fc6e5-a5ab-4123-9bc9-ccb756c0bf16/_apis/git/repositories/a89a9d2f-ab67-4bda-9c56-a571224c2c66/pullRequests/67/threads";

    private final GitHubService gitHubService;
    private final ADOService adoService;
    private GitHubController gitHubControllerSpy;
    private ADOController adoControllerSpy;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GitHubProperties gitHubProperties;
    private final HelperService helperService;
    private ScanResults scanResultsToInject;
    private SourceControlType sourceControl;
    private FlowProperties flowProperties;
    private CxProperties cxProperties;
    private String branch;

    public UpdatePullRequestCommentsSteps(GitHubService gitHubService, GitHubProperties gitHubProperties, GitHubController gitHubController, ADOService adoService, ADOController adoController, FlowProperties flowProperties, CxProperties cxProperties) {
        this.helperService = mock(HelperService.class);
        this.gitHubService = gitHubService;
        this.gitHubProperties = gitHubProperties;
        this.gitHubControllerSpy = Mockito.spy(gitHubController);
        this.adoService = adoService;
        this.adoControllerSpy = Mockito.spy(adoController);
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
        initGitHubProperties();
    }

    @Before
    public void initMocks() {
        flowProperties.getBranches().add("udi-tests-2");
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList("sast"));
        initGitHubControllerSpy();
        initHelperServiceMock();
    }

    @After
    public void cleanUp() throws IOException, InterruptedException {
        if (sourceControl.equals(SourceControlType.GITHUB)) {
            deleteGitHubComments();
        } else if (sourceControl.equals(SourceControlType.ADO)) {
            deleteADOComments();
        }
    }

    @Given("branch is pr-comments-tests")
    public void setBranchName() {
        branch = "pr-comments-tests";
    }

    @Given("branch is udi-test-2")
    public void setAdoBranch() {
        branch = "udi-tests-2";
    }

    @Given("different filters configuration is set")
    public void setConfigAsCodeFilters() {
        gitHubProperties.setConfigAsCode("cx.config.high.json");
    }

    @Given("source control is GitHub")
    public void scGitHub() {
        sourceControl = SourceControlType.GITHUB;
    }

    @Given("source control is ADO")
    public void scAdo() {
        sourceControl = SourceControlType.ADO;
    }


    @Given("no comments on pull request")
    public void deletePRComments() throws IOException, InterruptedException {
        if (sourceControl.equals(SourceControlType.GITHUB)) {
            deleteGitHubComments();
        } else if (sourceControl.equals(SourceControlType.ADO)) {
            deleteADOComments();
        }
    }

    private void deleteADOComments() throws IOException, InterruptedException {
        List<RepoComment> adoComments = getRepoComments();
        for (RepoComment rc: adoComments) {
            adoService.deleteComment(rc.getCommentUrl());
        }
        TimeUnit.SECONDS.sleep(20);
    }

    private void deleteGitHubComments() throws IOException, InterruptedException{
        List<RepoComment> comments = getRepoComments();
        for (RepoComment comment: comments) {
            gitHubService.deleteComment(comment.getCommentUrl());
        }
        TimeUnit.SECONDS.sleep(20);
    }

    private List<RepoComment> getRepoComments() throws IOException {
        if (sourceControl.equals(SourceControlType.GITHUB)) {
            return gitHubService.getComments(PULL_REQUEST_COMMENTS_URL);
        }
        else if (sourceControl.equals(SourceControlType.ADO)){
            return adoService.getComments(ADO_PR_COMMENTS_URL);
        }
        throw new IllegalArgumentException("Unknown source control: " + sourceControl);
    }

    @When("pull request arrives to CxFlow")
    public void sendPullRequest() {
        if (sourceControl.equals(SourceControlType.GITHUB)) {
            buildGitHubPullRequest();
        } else if (sourceControl.equals(SourceControlType.ADO)) {
            buildADOPullRequestEvent();
        }
    }

    private void validateCommentsUpdated(List<RepoComment> comments) {
        for (RepoComment comment: comments) {
            Assert.assertTrue("Comment was not updated", isCommentUpdated(comment));
        }
    }

    @Then("Wait for comments")
    public void waitForNewComments() {
        Awaitility.await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(COMMENTS_POLL_INTERVAL)).until(this::areThereCommentsAtAll);
    }

    @Then("verify 2 new comments")
    public void verify2NewComments() throws IOException {
        List<RepoComment> comments = getRepoComments();
        Assert.assertEquals("Wrong number of comments", 2, comments.size());
        comments.forEach(c -> Assert.assertTrue("Comment is not new (probably updated", isCommentNew(c)));

    }

    @Then("Wait for updated comment")
    public void waitForUpdatedComment() {
        Awaitility.await().pollInterval(Duration.ofSeconds(COMMENTS_POLL_INTERVAL)).atMost(Duration.ofSeconds(125)).until(this::isThereUpdatedComment);
    }

    private boolean areThereCommentsAtAll() throws IOException {
        List<RepoComment> comments = getRepoComments();
        return comments.size() > 1;
    }

    private boolean isThereUpdatedComment() throws IOException {
        List<RepoComment> comments = getRepoComments();
        for (RepoComment comment: comments) {
            if (isCommentUpdated(comment)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCommentNew(RepoComment comment) {
        return comment.getUpdateTime().equals(comment.getCreatedAt());
    }

    private boolean isCommentUpdated(RepoComment comment) {
        return comment.getUpdateTime().after(comment.getCreatedAt());
    }

    private void initGitHubControllerSpy() {
        doNothing().when(gitHubControllerSpy).verifyHmacSignature(any(), any());
    }

    private void initHelperServiceMock() {
        when(helperService.isBranch2Scan(any(), anyList())).thenReturn(true);
        when(helperService.getShortUid()).thenReturn("123456");
    }


    private void initGitHubProperties() {
        this.gitHubProperties.setCxSummary(false);
        this.gitHubProperties.setFlowSummary(false);
        this.gitHubProperties.setUrl(GIT_URL);
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setApiUrl("https://api.github.com/repos");
    }


    public void buildGitHubPullRequest() {
        PullEvent pullEvent = new PullEvent();
        Repository repo = new Repository();
        repo.setName("vb_test_udi");

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
        pullRequest.setIssueUrl(GITHUB_PR_BASE_URL + "/issues/5");

        pullEvent.setPullRequest(pullRequest);

        try {
            String pullEventStr = mapper.writeValueAsString(pullEvent);

            ControllerRequest request = ControllerRequest.builder()
                    .branch(Collections.singletonList(branch))
                    .application("VB")
                    .team("\\CxServer\\SP")
                    .assignee("")
                    .preset("default")
                    .build();

            gitHubControllerSpy.pullRequest(pullEventStr, "SIGNATURE", "CX", request);

        } catch (JsonProcessingException e) {
            fail("Unable to parse " + pullEvent.toString());
        }
    }


    public void buildADOPullRequestEvent() {
        com.checkmarx.flow.dto.azure.PullEvent pullEvent = new com.checkmarx.flow.dto.azure.PullEvent();
        pullEvent.setEventType("git.pullrequest.updated");
        pullEvent.setId("4519989c-c157-4bf8-9651-e94b8d0fca27");
        pullEvent.setSubscriptionId("25aa3b80-54ed-4b26-976a-b74f94940852");
        pullEvent.setPublisherId("tfs");
        Resource resource = new Resource();
        resource.setStatus("active");
        resource.setSourceRefName("refs/heads/master");
        resource.setTargetRefName("refs/heads/udi-tests-2");
        resource.setUrl("https://dev.azure.com/CxNamespace/d50fc6e5-a5ab-4123-9bc9-ccb756c0bf16/_apis/git/repositories/a89a9d2f-ab67-4bda-9c56-a571224c2c66/pullRequests/67");
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

        ControllerRequest request = ControllerRequest.builder()
                .project("AdoPullRequestTests-master")
                .team("\\CxServer\\SP")
                .build();
        adoControllerSpy.pullRequest(pullEvent,"Basic Y3hmbG93OjEyMzQ=", null, request, null);
    }

    private class ScanResultsAnswerer implements Answer<ScanResults> {
        @Override
        public ScanResults answer(InvocationOnMock invocation) {
            return scanResultsToInject;
        }
    }

    enum SourceControlType {
        GITHUB,
        ADO
    }
}
