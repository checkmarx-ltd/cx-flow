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
import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedList;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@Slf4j
public class GithubTestService implements RepoTestService{

    private static final String GITHUB_USER = "cxflowtestuser";
    private final ObjectMapper mapper = new ObjectMapper();
    private String githubProjectName;
    private String cxProjectName;
    private String branchName;
    private GitHubProperties gitHubProperties;
    private String githubPullRequestUrl;
    private String getGithubPullRequestUrlWithComments;
    private String gitUrl;
    private String cxTeam;
    private String statusApiUrl;
    private GitHubController gitHubControllerSpy;
    private RestTemplate restTemplate;


    @Override
    public void init(ScanRequest scanRequest, RepoProperties properties, String branch, String projectName, String teamName, RestTemplate rest){
        githubProjectName = scanRequest.getProject();
        gitHubProperties = (GitHubProperties) properties;

        gitUrl = String.format("%s/%s/%s.git", gitHubProperties.getUrl(), GITHUB_USER, githubProjectName);
        cxProjectName = projectName;
        branchName = branch;
        cxTeam = teamName;
        restTemplate = rest;
        initGitHubProperties();
    }

    @Override
    public void initPullRequestDetails(int pullRequestId, String lastCommitHash) {
        statusApiUrl = String.format("https://api.github.com/repos/%s/%s/statuses/%s", GITHUB_USER, githubProjectName, lastCommitHash);
        githubPullRequestUrl = String.format("%s/%s/%s/issues/%d", gitHubProperties.getApiUrl(), GITHUB_USER, githubProjectName, pullRequestId);
        getGithubPullRequestUrlWithComments = String.format("%s/comments", githubPullRequestUrl);
        putDefaultStatus();
    }

    @Override
    public void setController(WebhookController controller) {
        gitHubControllerSpy = Mockito.spy((GitHubController) controller);
        initGitHubControllerSpy();
    }

    private void putDefaultStatus() {
        HttpEntity<?> httpEntity = new HttpEntity<>(getJSONStatus("pending"), getHeaders());
        ResponseEntity<String> responseEntity = restTemplate.exchange(statusApiUrl, HttpMethod.POST, httpEntity, String.class);

        JSONObject updateStatusResponse = new JSONObject(responseEntity.getBody());
        log.info("pull request status changed to: {}", updateStatusResponse.getString("state"));
    }

    private static String getJSONStatus(String state){
        JSONObject requestBody = new JSONObject();
        requestBody.put("state", state);
        requestBody.put("context", "checkmarx");
        requestBody.put("target_url", "https://api.checkmarx.net");

        requestBody.put("description", "Checkmarx Scan Initiated");

        return requestBody.toString();
    }

    private void initGitHubProperties() {
        this.gitHubProperties.setCxSummary(false);
        this.gitHubProperties.setFlowSummary(false);
        this.gitHubProperties.setWebhookToken("1234");
        this.gitHubProperties.setApiUrl("https://api.github.com/repos");
    }

    @Override
    public void openPullRequest() {
        PullEvent pullEvent = new PullEvent();
        Repository repo = new Repository();
        repo.setName(githubProjectName);

        repo.setCloneUrl(gitUrl);
        Owner owner = new Owner();
        owner.setName("");
        owner.setLogin(GITHUB_USER);
        repo.setOwner(owner);
        pullEvent.setRepository(repo);
        pullEvent.setAction("opened");
        PullRequest pullRequest = new PullRequest();
        pullRequest.setIssueUrl("");
        Head headBranch = new Head();
        headBranch.setRef(branchName);

        pullRequest.setHead(headBranch);
        pullRequest.setBase(new Base());
        pullRequest.setStatusesUrl(statusApiUrl);
        pullRequest.setIssueUrl(githubPullRequestUrl);

        pullEvent.setPullRequest(pullRequest);

        try {
            String pullEventStr = mapper.writeValueAsString(pullEvent);
            ControllerRequest controllerRequest = new ControllerRequest();
            controllerRequest.setApplication(githubProjectName);
            controllerRequest.setBranch(Collections.singletonList(branchName));
            controllerRequest.setProject(cxProjectName);
            controllerRequest.setTeam(cxTeam);
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

    @Override
    public String getPullRequestStatus() {
        HttpEntity<String> httpEntity = new HttpEntity<>(getHeaders());
        ResponseEntity<String> response = restTemplate.exchange(statusApiUrl, HttpMethod.GET, httpEntity, String.class);

        JSONArray statuses = new JSONArray(response.getBody());

        return statuses.getJSONObject(0).getString("state");
    }

    private void initGitHubControllerSpy() {
        doNothing().when(gitHubControllerSpy).verifyHmacSignature(any(), any(), any());
    }

    HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "token ".concat(gitHubProperties.getToken()));
        return httpHeaders;
    }

    @Override
    public void sendPushEvent() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setCommits(new LinkedList<>());

        Repository repo = new Repository();
        repo.setName(githubProjectName);
        repo.setCloneUrl(gitUrl);
        Owner owner = new Owner();
        owner.setName(GITHUB_USER);
        owner.setLogin(GITHUB_USER);
        repo.setOwner(owner);
        pushEvent.setRepository(repo);

        Pusher pusher = new Pusher();
        pusher.setEmail("some@email");
        pushEvent.setPusher(pusher);

        pushEvent.setRef("refs/head/" + branchName);
        try {
            String pushEventStr = mapper.writeValueAsString(pushEvent);
            ControllerRequest request = ControllerRequest.builder()
                    .application(githubProjectName)
                    .branch(Collections.singletonList(branchName))
                    .project(cxProjectName)
                    .team(cxTeam)
                    .assignee("")
                    .preset("default")
                    .build();

            gitHubControllerSpy.pushRequest(pushEventStr, "SIGNATURE", "CX", request);

        } catch (JsonProcessingException e) {
            fail("Unable to parse " + pushEvent.toString());
        }
    }
}
