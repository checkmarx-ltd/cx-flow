package com.checkmarx.flow.cucumber.integration.cxgo;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.RepoService;
import com.checkmarx.sdk.config.CxGoProperties;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.cx.restclient.CxGoClientImpl;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;


@CucumberContextConfiguration
@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
public class CxGoRemoteRepoScanSteps {

    private final GitHubProperties gitHubProperties;
    private static final String GIT_PROJECT_NAME = "CxGo-Test";
    private static final String CXGO_PROJECT_NAME = "CxGo-Test-featureBranch";
    private Integer oldScanId;
    private Integer cxgoProjectId;
    private CxGoClientImpl cxGoClient;
    private CxGoProperties cxGoProperties;
    private RepoTestService repoTestService;
    private RepoService repoService;
    private GitHubController gitHubController;
    private GitHubService gitHubService;
    @Autowired
    FlowProperties properties;

    public CxGoRemoteRepoScanSteps(CxGoProperties goProperties, GitHubController gitHubController, GitHubProperties gitHubProperties, GitHubService gitHubService, CxGoClientImpl client){
        this.gitHubProperties = gitHubProperties;
        this.gitHubController = gitHubController;
        this.cxGoProperties = goProperties;
        this.cxGoClient = client;
        this.gitHubService = gitHubService;
    }

    @Before("@CxGoIntegrationTests")
    public void initCxFlow(){
        properties.setEnabledVulnerabilityScanners(Collections.singletonList("cxgo"));
        properties.setBugTracker(BugTracker.Type.JIRA.toString());
    }

    @Given("SCM type is {}")
    public void setScmType(String scmType) throws IOException {
        if(scmType.equals("Github")) {
            repoTestService = new GithubTestService();
            ScanRequest request = ScanRequest.builder().project(GIT_PROJECT_NAME).build();
            repoTestService.init(request, gitHubProperties, CXGO_PROJECT_NAME);
            repoTestService.setController(gitHubController);
            repoService = gitHubService;
        }
        deleteRepoPullRequestComments();
    }

    @And("configured scanner in cxflow configuration are {}")
    public void setCxGoScanners(String scanners) {
        // currently do nothing. Todo: add scanner configuration to cxgo yml
    }

    @And("Pull Request is opened in repo")
    public void openPullRequest() {
        repoTestService.openPullRequest();
    }

    @Then("CxFlow initiate scan in CxGo")
    public void initCxGoScan() throws CheckmarxException {
        String teamId = cxGoProperties.getTeam();
        String ownerId = cxGoClient.getTeamId(teamId);
        cxgoProjectId = cxGoClient.getProjectId(ownerId, CXGO_PROJECT_NAME);
        oldScanId = cxGoClient.getLastScanId(cxgoProjectId);
        log.info("checking for active scans in projectId '{}'", cxgoProjectId);
    }

    @And("{} bugTracker will be updated with tickets for {} findings")
    public void validateBugTrackerTickets(String bugTracker, String scanners){
        log.info("write something");
    }

    @And("Pull request is updated in {} repo")
    public void validatePullRequestComment(String scmType){
        waitForOperationToComplete(this::scanFinished, 600);
        waitForOperationToComplete(this::pullRequestHas2CxFlowComments, 30);
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
        log.info("waiting for scanId {} to finish", oldScanId);
        Integer latestScanId = cxGoClient.getLastScanId(cxgoProjectId);
        log.info("latest scanId found for project: '{}' is scanId: {}", cxgoProjectId, oldScanId);
        return !latestScanId.equals(oldScanId);
    }

    private boolean pullRequestHas2CxFlowComments() throws IOException {
        List<RepoComment> comments = repoService.getComments(getBasicRequest());
        log.info("found {} comments in pull request", comments.size());
        return comments.size() > 1;
    }

    private ScanRequest getBasicRequest() {
        return ScanRequest.builder()
                .mergeNoteUri(repoTestService.getPullRequestCommentsUrl())
                .build();
    }

    private void deleteRepoPullRequestComments() throws IOException {
        List<RepoComment> comments = repoService.getComments(getBasicRequest());
        for (RepoComment comment: comments) {
            repoService.deleteComment(comment.getCommentUrl(), getBasicRequest());
        }
    }
}