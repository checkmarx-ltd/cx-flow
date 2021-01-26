package com.checkmarx.flow.cucumber.integration.cxgo;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.*;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.controller.GitLabController;
import com.checkmarx.flow.cucumber.common.repoServiceMockers.GithubServiceMocker;
import com.checkmarx.flow.cucumber.common.repoServiceMockers.GitlabServiceMocker;
import com.checkmarx.flow.cucumber.common.repoServiceMockers.RepoServiceMocker;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.RepoService;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.CxGoProperties;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.scanner.GoScanner;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;


@CucumberContextConfiguration
@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@Slf4j
public class CxGoRemoteRepoScanSteps {

    private static final String JIRA_PROJECT = "CT";
    private static final String PR_COMMIT_HASH = "1a137e46d2b83e580aa20eedf2fdf8d7ed2073a5";
    private static final int PULL_REQUEST_ID = 1;
    private static final String GITHUB_PROJECT_NAME = "CxGo-Integration-Tests";
    private static final String GITHUB_BRANCH = "develop";
    private static final String GITLAB_PROJECT_NAME = "cxflow-cxgo-integration-tests";
    private static final String GITLAB_BRANCH = "feature-branch";
    private static final String CXGO_PROJECT_NAME = "CxGo-Integration-Tests-develop";
    private static final String CXGO_TEAM_NAME = "\\Demo\\CxFlow";
    private static final String PENDING_STATUS = "pending";

    private static final int MAX_TIME_FOR_SCAN_COMPLETED_IN_SEC = 600;
    private static final int MAX_TIME_FOR_PULL_REQUEST_UPDATE_IN_SEC = 60;
    private static final int MAX_TIME_FOR_PULL_REQUEST_NOT_PENDING_IN_SEC = 15;
    private static final int MAX_TIME_FOR_BUG_TRACKER_UPDATE_IN_SEC = 150;
    private final GitHubProperties gitHubProperties;
    private final GitLabProperties gitLabProperties;
    private final FlowProperties flowProperties;
    private Integer oldScanId;
    private Integer cxgoProjectId;
    private GoScanner cxGoClient;
    private CxGoProperties cxGoProperties;
    private RepoServiceMocker repoServiceMocker;
    private RepoService repoService;
    private GitHubController gitHubController;
    private GitHubService gitHubService;

    private GitLabController gitLabController;
    private JiraProperties jiraProperties;

    @Autowired
    FlowProperties properties;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private IJiraTestUtils jiraUtils;


    public CxGoRemoteRepoScanSteps(CxGoProperties goProperties, GitHubController gitHubController, GitHubProperties gitHubProperties, GitHubService gitHubService, GoScanner client,
                                   JiraProperties jiraProperties, GitLabController gitLabController, GitLabProperties gitLabProperties, FlowProperties flowProperties){
        this.gitHubProperties = gitHubProperties;
        this.gitHubController = gitHubController;
        this.cxGoProperties = goProperties;
        this.cxGoClient = client;
        this.gitHubService = gitHubService;
        this.jiraProperties = jiraProperties;
        this.gitLabController = gitLabController;
        this.gitLabProperties = gitLabProperties;
        this.flowProperties = flowProperties;
    }

    @Before("@CxGoIntegrationTests")
    public void init() throws IOException {
        initCxFlowConfiguration();
        initJiraBugTracker();
    }

    private void initJiraBugTracker() throws IOException {
        log.info("Jira project key: {}", JIRA_PROJECT);
        jiraProperties.setProject(JIRA_PROJECT);
        log.info("Cleaning jira project before test: {}", jiraProperties.getProject());
        jiraUtils.ensureProjectExists(jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }

    private void initCxFlowConfiguration(){
        properties.setEnabledVulnerabilityScanners(Collections.singletonList("cxgo"));
        properties.setBugTracker(BugTracker.Type.JIRA.toString());

    }

    @After("@CxGoIntegrationTests")
    public void cleanTestOutputs() {
        deleteRepoPullRequestComments();
        log.info("Cleaning jira project after test: {}", jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }

    @Given("SCM type is {}")
    public void setScmType(String scmType){
        if(scmType.equals("Github")) {
            repoServiceMocker = new GithubServiceMocker();
            repoServiceMocker.init(GITHUB_PROJECT_NAME, gitHubProperties, GITHUB_BRANCH, CXGO_PROJECT_NAME, CXGO_TEAM_NAME, restTemplate);
            repoServiceMocker.initPullRequestDetails(PULL_REQUEST_ID, PR_COMMIT_HASH);
            repoServiceMocker.setController(gitHubController);
            repoService = gitHubService;
        }
        if(scmType.equals("Gitlab")) {
            repoServiceMocker = new GitlabServiceMocker();
            repoServiceMocker.init(GITLAB_PROJECT_NAME, gitLabProperties, GITLAB_BRANCH, CXGO_PROJECT_NAME, CXGO_TEAM_NAME, restTemplate);
            repoServiceMocker.setController(gitLabController);
        }

        deleteRepoPullRequestComments();
    }

    @And("Thresholds set to {}")
    public void setThresholds(boolean thresholdsExceeded) {
        flowProperties.setThresholds(new HashMap<>());
        if(!thresholdsExceeded){
            log.info("resetting thresholds to approve pull request");
            flowProperties.getThresholds().put(FindingSeverity.MEDIUM, 100);
        }
        else {
            log.info("setting thresholds to reject pull request");
            flowProperties.getThresholds().put(FindingSeverity.MEDIUM, 1);
        }
    }

    @And("Filters set to {}")
    public void setFilters(String filters) {
        // currently do nothing. Todo: add filters validation
    }

    @And("Pull Request is opened in repo")
    public void openPullRequest() {
        repoServiceMocker.openPullRequest();
    }

    @And("Push event is sent to cxflow")
    public void sendPushEvent() {
        repoServiceMocker.sendPushEvent();
    }

    @Then("CxFlow initiate scan in CxGo")
    public void initCxGoScan() throws CheckmarxException {
        String teamId = cxGoProperties.getTeam();
        String ownerId = cxGoClient.getTeamId(teamId);
        cxgoProjectId = cxGoClient.getProjectId(ownerId, CXGO_PROJECT_NAME);
        oldScanId = cxGoClient.getLastScanId(cxgoProjectId);
        log.info("checking for active scans in projectId '{}'", cxgoProjectId);
    }

    @And("bugTracker will be updated with tickets for filtered findings by {}")
    public void validateBugTrackerTickets(String filters){
        waitForOperationToComplete(this::scanFinished, MAX_TIME_FOR_SCAN_COMPLETED_IN_SEC);
        waitForOperationToComplete(this::bugTrackerUpdateWithTickets, MAX_TIME_FOR_BUG_TRACKER_UPDATE_IN_SEC);

        log.info("pulling all issues from bug tracker");
        Map<Filter.Severity, Integer> actualJira = jiraUtils.getIssuesPerSeverity(jiraProperties.getProject());

        Assert.assertTrue("HIGH severity vulnerabilities not found in project", actualJira.get(Filter.Severity.HIGH) > 0);
        Assert.assertTrue("MEDIUM severity vulnerabilities not found in project", actualJira.get(Filter.Severity.MEDIUM) > 0);
        Assert.assertTrue("LOW severity vulnerabilities not found in project", actualJira.get(Filter.Severity.LOW) > 0);
    }

    @And("Pull request comments updated in repo and status is {}")
    public void validatePullRequestComment(String pullRequestStatus) throws InterruptedException {
        waitForOperationToComplete(this::scanFinished, MAX_TIME_FOR_SCAN_COMPLETED_IN_SEC);
        waitForOperationToComplete(this::pullRequestHas2CxFlowComments, MAX_TIME_FOR_PULL_REQUEST_UPDATE_IN_SEC);
        waitForOperationToComplete(this::pullRequestNotPending, MAX_TIME_FOR_PULL_REQUEST_NOT_PENDING_IN_SEC);

        String status = repoServiceMocker.getPullRequestStatus();

        Assert.assertEquals(pullRequestStatus, status);
    }

    private void waitForOperationToComplete(Callable<Boolean> conditionEvaluator, int secondsToWait){
        try {
            Awaitility.await()
                    .atMost(Duration.ofSeconds(secondsToWait))
                    .pollInterval(Duration.ofSeconds(5))
                    .until(conditionEvaluator);

            log.info("wait completed");
        }
        catch (Exception e) {
            log.info("wait completed with exception - {}", e.getMessage());
        }
    }

    private boolean scanFinished(){
        log.info("original scan Id is {}. waiting for new scan to complete", oldScanId);
        Integer latestScanId = cxGoClient.getLastScanId(cxgoProjectId);
        log.info("latest scanId found for project: '{}' is scanId: {}", cxgoProjectId, latestScanId);
        return !latestScanId.equals(oldScanId);
    }

    private boolean pullRequestHas2CxFlowComments() throws IOException {
        List<RepoComment> comments = repoService.getComments(getBasicRequest());
        log.info("found {} comments in pull request", comments.size());
        return comments.size() > 1;
    }

    private boolean pullRequestNotPending() {
        String status = repoServiceMocker.getPullRequestStatus();
        log.info("pull request statues is: {}", status);
        return !status.equalsIgnoreCase(PENDING_STATUS);
    }

    private boolean bugTrackerUpdateWithTickets(){
        int numOfTickets = jiraUtils.getNumberOfIssuesInProject(JIRA_PROJECT);
        log.debug("found {} tickets in Jira project: {}", numOfTickets, JIRA_PROJECT);
        return numOfTickets > 18;
    }

    private ScanRequest getBasicRequest() {
        return ScanRequest.builder()
                .mergeNoteUri(repoServiceMocker.getPullRequestCommentsUrl())
                .build();
    }

    private void deleteRepoPullRequestComments() {
        try {
            if(repoService != null) {
                List<RepoComment> comments = repoService.getComments(getBasicRequest());
                log.info("deleting {} comments from pull request", comments.size());
                for (RepoComment comment : comments) {
                    log.info("deleting comment: '{}'", comment.getComment());
                    repoService.deleteComment(comment.getCommentUrl(), getBasicRequest());
                }
            }
        }
        catch (IOException ex){
            log.error("fail to delete Pull Request comments: {}", ex.getMessage());
        }
    }
}