package com.checkmarx.flow.cucumber.integration.cxgo;

import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.controller.WebhookController;
import com.checkmarx.flow.dto.ScanRequest;
import org.springframework.web.client.RestTemplate;

public interface RepoServiceMocker {
    void init(String gitProjectName, RepoProperties properties, String branch, String projectName, String teamName, RestTemplate restTemplate);
    void initPullRequestDetails(int pullRequestId, String lastCommitHash);
    void setController(WebhookController controller);
    void openPullRequest();
    void sendPushEvent();
    String getPullRequestCommentsUrl();
    String getPullRequestStatus();
}
