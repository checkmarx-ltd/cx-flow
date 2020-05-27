package com.checkmarx.flow.cucumber.integration.pullrequest.updatecomments;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class UpdatePullRequestCommentsSteps {


    public static final int COMMENTS_POLL_INTERVAL = 5;
    public static final String PULL_REQUEST_COMMENTS_URL = "https://api.github.com/repos/cxflowtestuser/vb_test_udi/issues/2/comments";
    private final GitHubService gitHubService;
    private GitHubController gitHubControllerSpy;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GitHubProperties gitHubProperties;
    private final HelperService helperService;
    private ScanResults scanResultsToInject;

    private String branch;

    public UpdatePullRequestCommentsSteps(GitHubService gitHubService, GitHubProperties gitHubProperties, GitHubController gitHubController) {

        this.helperService = mock(HelperService.class);
        this.gitHubService = gitHubService;

        this.gitHubProperties = gitHubProperties;
        this.gitHubControllerSpy = Mockito.spy(gitHubController);
        initGitHubProperties();
    }

    @Before
    public void initMocks() {
        initGitHubControllerSpy();
        initHelperServiceMock();
    }

    @Given("branch is udi-tests-2")
    public void setBranchName() {
        branch = "udi-tests-2";
    }

    @Given("different filters configuration is set")
    public void setConfigAsCodeFilters() {
        gitHubProperties.setConfigAsCode("cx.config.high.json");
    }

    @Given("no comments on pull request")
    public void deletePRComments() throws IOException, InterruptedException {
        List<RepoComment> comments = getRepoComments();
        for (RepoComment comment: comments) {
            gitHubService.deleteComment(comment.getCommentUrl());
        }
        TimeUnit.SECONDS.sleep(20);
    }

    private List<RepoComment> getRepoComments() throws IOException {
        return gitHubService.getComments(PULL_REQUEST_COMMENTS_URL);
    }

    @When("pull request arrives to CxFlow")
    public void sendPullRequest() {
        buildPullRequest();
    }

    private void validateCommentsUpdated(List<RepoComment> comments) throws IOException {
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
        comments.stream().forEach(c -> Assert.assertTrue("Comment is not new (probably updated", isCommentNew(c)));

    }

    @Then("Wait for updated comment")
    public void waitForUpdatedComment() {
        Awaitility.await().pollInterval(Duration.ofSeconds(COMMENTS_POLL_INTERVAL)).atMost(Duration.ofSeconds(125)).until(this::isThereUpdatedComment);
    }

/*    @Then("verify 2 updated comments")
    public void verify2UpdatedComments() throws IOException {
        List<RepoComment> comments = getRepoComments();
        Assert.assertEquals("Wrong number of comments", 2, comments.size());
        comments.stream().forEach(c -> Assert.assertTrue("Comment is not new (probably updated", isCommentUpdated(c)));
    }*/


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
        this.gitHubProperties.setUrl("https://github.com/cxflowtestuser/vb_test_udi");
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setApiUrl("https://api.github.com/repos");
    }


    public void buildPullRequest() {
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
        pullRequest.setIssueUrl("https://api.github.com/repos/cxflowtestuser/vb_test_udi/issues/2");

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

    private class ScanResultsAnswerer implements Answer<ScanResults> {
        @Override
        public ScanResults answer(InvocationOnMock invocation) {
            return scanResultsToInject;
        }
    }

}
