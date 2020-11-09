package com.checkmarx.flow.cucumber.integration.cxgo;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.RepoService;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.CxGoProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.cx.restclient.CxGoClientImpl;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;


@CucumberContextConfiguration
@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@Slf4j
public class CxGoRemoteRepoScanSteps {

    private final GitHubProperties gitHubProperties;
    private static final String JIRA_PROJECT = "CT";
    private static final String PR_COMMIT_HASH = "75380372c24c0caeeb97c104214dd448ac9e066d";
    private static final int PULL_REQUEST_ID = 1;
    private static final String GIT_PROJECT_NAME = "CxGo-Integration-Tests";
    private static final String GIT_BRANCH = "develop";
    private static final String CXGO_PROJECT_NAME = "CxGo-Integration-Tests-develop";

    private static final String CXGO_TEAM_NAME = "\\Demo\\CxFlow";

    private static final int MAX_TIME_FOR_SCAN_COMPLETED_IN_SEC = 600;
    private static final int MAX_TIME_FOR_PULL_REQUEST_UPDATE_IN_SEC = 60;
    private static final int MAX_TIME_FOR_BUG_TRACKER_UPDATE_IN_SEC = 150;
    private Integer oldScanId;
    private Integer cxgoProjectId;
    private CxGoClientImpl cxGoClient;
    private CxGoProperties cxGoProperties;
    private RepoTestService repoTestService;
    private RepoService repoService;
    private GitHubController gitHubController;
    private GitHubService gitHubService;
    private JiraProperties jiraProperties;

    @Autowired
    FlowProperties properties;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private IJiraTestUtils jiraUtils;


    public CxGoRemoteRepoScanSteps(CxGoProperties goProperties, GitHubController gitHubController, GitHubProperties gitHubProperties, GitHubService gitHubService, CxGoClientImpl client,
                                    JiraProperties jiraProperties){
        this.gitHubProperties = gitHubProperties;
        this.gitHubController = gitHubController;
        this.cxGoProperties = goProperties;
        this.cxGoClient = client;
        this.gitHubService = gitHubService;
        this.jiraProperties = jiraProperties;
    }

    @Before("@CxGoIntegrationTests")
    public void initCxFlow() throws IOException {
        properties.setEnabledVulnerabilityScanners(Collections.singletonList("cxgo"));
        properties.setBugTracker(BugTracker.Type.JIRA.toString());

        log.info("Jira project key: {}", JIRA_PROJECT);
        jiraProperties.setProject(JIRA_PROJECT);
        initJiraBugTracker();
    }

    private void initJiraBugTracker() throws IOException {
        log.info("Cleaning jira project before test: {}", jiraProperties.getProject());
        jiraUtils.ensureProjectExists(jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }

    @After("@CxGoIntegrationTests")
    public void cleanRepo() {
        deleteRepoPullRequestComments();
        log.info("Cleaning jira project after test: {}", jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }


    @Given("SCM type is {}")
    public void setScmType(String scmType){
        if(scmType.equals("Github")) {
            repoTestService = new GithubTestService();
            ScanRequest request = ScanRequest.builder().project(GIT_PROJECT_NAME).build();
            repoTestService.init(request, gitHubProperties, GIT_BRANCH, CXGO_PROJECT_NAME, CXGO_TEAM_NAME, restTemplate);
            repoTestService.initPullRequestDetails(PULL_REQUEST_ID, PR_COMMIT_HASH);
            repoTestService.setController(gitHubController);
            repoService = gitHubService;
        }
        deleteRepoPullRequestComments();
    }

    @And("Thresholds set to {}")
    public void setThresholds(String scanners) {
        // currently do nothing. Todo: add thresholds validation
    }

    @And("Filters set to {}")
    public void setFilters(String scanners) {
        // currently do nothing. Todo: add filters validation
    }

    @And("Pull Request is opened in repo")
    public void openPullRequest() {
        repoTestService.openPullRequest();
    }

    @And("Push event is sent to cxflow")
    public void sendPushEvent() {
        repoTestService.sendPushEvent();
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
    public void validatePullRequestComment(String pullRequestStatus){
        waitForOperationToComplete(this::scanFinished, MAX_TIME_FOR_SCAN_COMPLETED_IN_SEC);
        waitForOperationToComplete(this::pullRequestHas2CxFlowComments, MAX_TIME_FOR_PULL_REQUEST_UPDATE_IN_SEC);

        String status = repoTestService.getPullRequestStatus();

        Assert.assertEquals(pullRequestStatus, status);
    }

    private void waitForOperationToComplete(Callable<Boolean> conditionEvaluator, int secondsToWait){
        try {
            Awaitility.await()
                    .atMost(Duration.ofSeconds(secondsToWait))
                    .pollInterval(Duration.ofSeconds(10))
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

    private boolean bugTrackerUpdateWithTickets(){
        int numOfTickets = jiraUtils.getNumberOfIssuesInProject(JIRA_PROJECT);
        log.debug("found {} tickets in Jira project: {}", numOfTickets, JIRA_PROJECT);
        return numOfTickets > 20;
    }

    private ScanRequest getBasicRequest() {
        return ScanRequest.builder()
                .mergeNoteUri(repoTestService.getPullRequestCommentsUrl())
                .build();
    }

    private void deleteRepoPullRequestComments() {
        try {
            List<RepoComment> comments = repoService.getComments(getBasicRequest());
            log.info("deleting {} comments from pull request", comments.size());
            for (RepoComment comment : comments) {
                log.info("deleting comment: '{}'", comment.getComment());
                repoService.deleteComment(comment.getCommentUrl(), getBasicRequest());
            }
        }
        catch (IOException ex){
            log.error("fail to delete Pull Request comments: {}", ex.getMessage());
        }
    }
}