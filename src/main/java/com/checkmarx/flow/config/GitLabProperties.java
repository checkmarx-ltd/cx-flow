package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "gitlab")
@Validated
public class GitLabProperties {
    private String webhookToken;
    private String token;
    private String url;
    private String apiUrl;
    private String falsePositiveLabel = "false-positive";
    private String openTransition = "reopen";
    private String closeTransition = "close";
    private boolean blockMerge = false;
    private boolean blockMergeComplete = false;

    public String getWebhookToken() {
        return this.webhookToken;
    }

    public String getToken() {
        return this.token;
    }

    public String getUrl() {
        return this.url;
    }

    public String getApiUrl() {
        return this.apiUrl;
    }

    public String getFalsePositiveLabel() {
        return this.falsePositiveLabel;
    }

    public String getOpenTransition() {
        return this.openTransition;
    }

    public String getCloseTransition() {
        return this.closeTransition;
    }

    public void setWebhookToken(String webhookToken) {
        this.webhookToken = webhookToken;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setFalsePositiveLabel(String falsePositiveLabel) {
        this.falsePositiveLabel = falsePositiveLabel;
    }

    public void setOpenTransition(String openTransition) {
        this.openTransition = openTransition;
    }

    public void setCloseTransition(String closeTransition) {
        this.closeTransition = closeTransition;
    }

    public boolean isBlockMerge() {
        return blockMerge;
    }

    public void setBlockMerge(boolean blockMerge) {
        this.blockMerge = blockMerge;
    }

    public boolean isBlockMergeComplete() {
        return blockMergeComplete;
    }

    public void setBlockMergeComplete(boolean blockMergeComplete) {
        this.blockMergeComplete = blockMergeComplete;
    }

    public String getGitUri(String namespace, String repo){
        String format = "%s/%s/%s.git";
        return String.format(format, getUrl(), namespace, repo);
        //sample: https://github.com/namespace/repo.git
    }

}
