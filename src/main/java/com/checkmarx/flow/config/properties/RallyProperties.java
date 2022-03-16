package com.checkmarx.flow.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "rally")
@Validated
public class RallyProperties extends RepoProperties {
    private String rallyWorkspaceId;
    private String rallyProjectId;
    private String appLabelPrefix = "app";
    private String ownerLabelPrefix = "owner";
    private String repoLabelPrefix = "repo";
    private String branchLabelPrefix = "branch";

    public String getRallyWorkspaceId() {
        return rallyWorkspaceId;
    }

    public String getRallyProjectId() { return rallyProjectId; }

    public String getOwnerLabelPrefix() {
        return this.ownerLabelPrefix;
    }

    public String getRepoLabelPrefix() {
        return this.repoLabelPrefix;
    }

    public String getBranchLabelPrefix() {
        return this.branchLabelPrefix;
    }

    public String getAppLabelPrefix() {
        return this.appLabelPrefix;
    }

    public void setRallyWorkspaceId(String rallyWorkspaceId) {
        this.rallyWorkspaceId = rallyWorkspaceId;
    }

    public void setRallyProjectId(String rallyProjectId) {
        this.rallyProjectId = rallyProjectId;
    }

    public void setOwnerLabelPrefix(String ownerLabelPrefix) {
        this.ownerLabelPrefix = ownerLabelPrefix;
    }

    public void setRepoLabelPrefix(String repoLabelPrefix) {
        this.repoLabelPrefix = repoLabelPrefix;
    }

    public void setBranchLabelPrefix(String branchLabelPrefix) {
        this.branchLabelPrefix = branchLabelPrefix;
    }

    public void setAppLabelPrefix(String appLabelPrefix) {
        this.appLabelPrefix = appLabelPrefix;
    }
}
