package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "azure")
@Validated
public class ADOProperties extends RepoProperties{
    private String issueType = "issue";
    private String issueBody = "Description";
    private String appTagPrefix = "app";
    private String ownerTagPrefix = "owner";
    private String repoTagPrefix = "repo";
    private String branchLabelPrefix = "branch";
    private String apiVersion = "5.0";
    private String openStatus;
    private String closedStatus;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getIssueBody() {
        return issueBody;
    }

    public void setIssueBody(String issueBody) {
        this.issueBody = issueBody;
    }

    public String getAppTagPrefix() {
        return appTagPrefix;
    }

    public void setAppTagPrefix(String appTagPrefix) {
        this.appTagPrefix = appTagPrefix;
    }

    public String getOwnerTagPrefix() {
        return ownerTagPrefix;
    }

    public void setOwnerTagPrefix(String ownerTagPrefix) {
        this.ownerTagPrefix = ownerTagPrefix;
    }

    public String getRepoTagPrefix() {
        return repoTagPrefix;
    }

    public void setRepoTagPrefix(String repoTagPrefix) {
        this.repoTagPrefix = repoTagPrefix;
    }

    public String getBranchLabelPrefix() {
        return branchLabelPrefix;
    }

    public void setBranchLabelPrefix(String branchLabelPrefix) {
        this.branchLabelPrefix = branchLabelPrefix;
    }

    public String getOpenStatus() {
        return openStatus;
    }

    public void setOpenStatus(String openStatus) {
        this.openStatus = openStatus;
    }

    public String getClosedStatus() {
        return closedStatus;
    }

    public void setClosedStatus(String closedStatus) {
        this.closedStatus = closedStatus;
    }

}
