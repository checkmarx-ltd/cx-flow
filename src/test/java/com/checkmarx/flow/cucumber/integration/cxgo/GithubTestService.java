package com.checkmarx.flow.cucumber.integration.cxgo;

import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.controller.WebhookController;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.github.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@Slf4j
public class GithubTestService implements RepoTestService{

    private static final String GITHUB_USER = "cxflowtestuser";
    private String githubProjectName;
    private String cxgoProjectName;
    private GitHubProperties gitHubProperties;
    private String githubPullRequestUrl;
    private String getGithubPullRequestUrlWithComments;
    private String gitUrl;
    private GitHubController gitHubControllerSpy;

    @Override
    public void init(ScanRequest scanRequest, RepoProperties properties, String projectName){
        githubProjectName = scanRequest.getProject();
        gitHubProperties = (GitHubProperties) properties;

        githubPullRequestUrl = String.format("%s/%s/%s/issues/%d", gitHubProperties.getApiUrl(), GITHUB_USER, githubProjectName, 2);
        getGithubPullRequestUrlWithComments = String.format("%s/comments", githubPullRequestUrl);
        gitUrl = String.format("%s/%s/%s.git", gitHubProperties.getUrl(), GITHUB_USER, githubProjectName);
        cxgoProjectName = projectName;
        initGitHubProperties();
    }

    @Override
    public void setController(WebhookController controller) {
        gitHubControllerSpy = Mockito.spy((GitHubController) controller);
        initGitHubControllerSpy();
    }

    private void initGitHubProperties() {
        this.gitHubProperties.setCxSummary(false);
        this.gitHubProperties.setFlowSummary(false);
        this.gitHubProperties.setUrl(gitUrl);
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setApiUrl("https://api.github.com/repos");
    }

    @Override
    public void openPullRequest() {
        PullEvent pullEvent = new PullEvent();
        Repository repo = new Repository();
        repo.setName(githubProjectName);

        log.info("gitHubProperties.getUrl() - {}", gitHubProperties.getUrl());
        log.info("gitCloneUrl() - {}", gitUrl);
        repo.setCloneUrl(gitUrl);
        Owner owner = new Owner();
        owner.setName("");
        owner.setLogin("cxflowtestuser");
        repo.setOwner(owner);
        pullEvent.setRepository(repo);
        pullEvent.setAction("opened");
        PullRequest pullRequest = new PullRequest();
        pullRequest.setIssueUrl("");
        Head headBranch = new Head();
        headBranch.setRef("featureBranch");

        pullRequest.setHead(headBranch);
        pullRequest.setBase(new Base());
        pullRequest.setStatusesUrl("");
        pullRequest.setIssueUrl(githubPullRequestUrl);

        pullEvent.setPullRequest(pullRequest);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String pullEventStr = mapper.writeValueAsString(pullEvent);
            ControllerRequest controllerRequest = new ControllerRequest();
            controllerRequest.setApplication("CxGo-Test");
            controllerRequest.setBranch(Collections.singletonList("featureBranch"));
            controllerRequest.setProject(cxgoProjectName);
            controllerRequest.setTeam("\\Demo\\CxFlow");
            controllerRequest.setPreset("default");
            controllerRequest.setIncremental(false);
            gitHubControllerSpy.pullRequest(
                    pullEventStr,
                    "SIGNATURE",
                    "CX",
                    controllerRequest
            );

        } catch (JsonProcessingException e) {
            fail("Unable to parse " + pullEvent.toString());
        }
    }

    @Override
    public String getPullRequestCommentsUrl() {
        return getGithubPullRequestUrlWithComments;
    }

    private void initGitHubControllerSpy() {
        doNothing().when(gitHubControllerSpy).verifyHmacSignature(any(), any(), any());
    }
}
