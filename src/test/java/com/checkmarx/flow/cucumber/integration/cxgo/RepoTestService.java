package com.checkmarx.flow.cucumber.integration.cxgo;

import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.controller.WebhookController;
import com.checkmarx.flow.dto.ScanRequest;

public interface RepoTestService {
    void init(ScanRequest scanRequest, RepoProperties properties, String projectName);
    void setController(WebhookController controller);
    void openPullRequest();
    String getPullRequestCommentsUrl();
}
