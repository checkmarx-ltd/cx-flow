package com.checkmarx.flow.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "servicenow")
public class ServiceNowProperties extends RepoProperties {
    private String serviceNowWorkspaceId;
    private String serviceNowProjectId;
    private String appLabelPrefix = "app";
    private String ownerLabelPrefix = "owner";
    private String repoLabelPrefix = "repo";
    private String branchLabelPrefix = "branch";
    private String username;
    private String password;
    private String apiUrl;

    public String getServiceNowWorkspaceId() {
        return serviceNowWorkspaceId;
    }

    public void setServiceNowWorkspaceId(String serviceNowWorkspaceId) {
        this.serviceNowWorkspaceId = serviceNowWorkspaceId;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServiceNowProjectId() {
        return serviceNowProjectId;
    }

    public void setServiceNowProjectId(String serviceNowProjectId) {
        this.serviceNowProjectId = serviceNowProjectId;
    }

    public String getAppLabelPrefix() {
        return appLabelPrefix;
    }

    public void setAppLabelPrefix(String appLabelPrefix) {
        this.appLabelPrefix = appLabelPrefix;
    }

    public String getOwnerLabelPrefix() {
        return ownerLabelPrefix;
    }

    public void setOwnerLabelPrefix(String ownerLabelPrefix) {
        this.ownerLabelPrefix = ownerLabelPrefix;
    }

    public String getRepoLabelPrefix() {
        return repoLabelPrefix;
    }

    public void setRepoLabelPrefix(String repoLabelPrefix) {
        this.repoLabelPrefix = repoLabelPrefix;
    }

    public String getBranchLabelPrefix() {
        return branchLabelPrefix;
    }

    public void setBranchLabelPrefix(String branchLabelPrefix) {
        this.branchLabelPrefix = branchLabelPrefix;
    }
}
