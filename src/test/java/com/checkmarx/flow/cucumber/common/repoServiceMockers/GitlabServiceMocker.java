package com.checkmarx.flow.cucumber.common.repoServiceMockers;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.controller.GitLabController;
import com.checkmarx.flow.controller.WebhookController;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.gitlab.Project;
import com.checkmarx.flow.dto.gitlab.PushEvent;
import com.checkmarx.flow.dto.gitlab.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedList;

public class GitlabServiceMocker implements RepoServiceMocker {

    private static final String GITLAB_USER = "cxflowtestuser";
    private final ObjectMapper mapper = new ObjectMapper();
    private String gitlabProjectName;
    private String cxProjectName;
    private String branchName;
    private String gitUrl;
    private String cxTeam;
    private GitLabController gitlabControllerSpy;


    @Override
    public void init(String gitProjectName, RepoProperties properties, String branch, String projectName, String teamName, RestTemplate rest) {
        gitlabProjectName = gitProjectName;
        GitLabProperties gitlabProperties = (GitLabProperties) properties;
        branchName = branch;
        cxProjectName = projectName;
        cxTeam = teamName;

        gitUrl = String.format("%s/%s/%s.git", gitlabProperties.getUrl(), GITLAB_USER, gitlabProjectName);
        gitlabProperties.setWebhookToken("SIGNATURE");
    }

    @Override
    public void initPullRequestDetails(int pullRequestId, String lastCommitHash) {
        // not implemented
    }

    @Override
    public void setController(WebhookController controller) {
        gitlabControllerSpy = Mockito.spy((GitLabController) controller);
    }

    @Override
    public void openPullRequest() {
        // not implemented
    }

    @Override
    public void sendPushEvent() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setCommits(new LinkedList<>());

        Repository repo = new Repository();
        repo.setName(gitlabProjectName);
        pushEvent.setRepository(repo);
        pushEvent.setRef("refs/head/" + branchName);
        Project project = new Project();
        project.setGitHttpUrl(gitUrl);
        project.setNamespace(GITLAB_USER);
        project.setName(gitlabProjectName);
        pushEvent.setProject(project);
        ControllerRequest request = ControllerRequest.builder()
                .application(gitlabProjectName)
                .branch(Collections.singletonList(branchName))
                .project(cxProjectName)
                .team(cxTeam)
                .assignee("")
                .preset("default")
                .build();

        gitlabControllerSpy.pushRequest(pushEvent, "SIGNATURE", "CX", request);
    }

    @Override
    public String getPullRequestCommentsUrl() {
        return null;
    }

    @Override
    public String getPullRequestStatus() {
        return null;
    }
}
