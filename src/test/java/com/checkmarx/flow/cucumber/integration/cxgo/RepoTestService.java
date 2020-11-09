package com.checkmarx.flow.cucumber.integration.cxgo;

import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.controller.WebhookController;
import com.checkmarx.flow.dto.ScanRequest;
import org.springframework.web.client.RestTemplate;

public interface RepoTestService {
    void init(ScanRequest scanRequest, RepoProperties properties, String branch, String projectName, String teamName, RestTemplate restTemplate);
    void initPullRequestDetails(int pullRequestId, String lastCommiyHash);
    void setController(WebhookController controller);
    void openPullRequest();
    void sendPushEvent();
    String getPullRequestCommentsUrl();
    String getPullRequestStatus();
}
