package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "github")
@Validated
public class GitHubProperties {
    @NotNull
    private boolean enabled;
    private String webhookToken;
    private String token;
    private String url;
    private String apiUrl;
    private String falsePositiveLabel = "false-positive";
    private String openTransition = "open";
    private String closeTransition = "closed";
    private boolean blockMerge = false;

    public @NotNull
    boolean isEnabled() {
        return this.enabled;
    }

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

    public void setEnabled(@NotNull boolean enabled) {
        this.enabled = enabled;
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

    public String getMergeNoteUri(String namespace, String repo, String mergeId){
        String format = "%s/%s/%s/issues/%s/comments";
        return String.format(format, getApiUrl(), namespace, repo, mergeId);
        //sample: https://api.github.com/repos/octocat/Hello-World/issues/1347/comments

    }
}
