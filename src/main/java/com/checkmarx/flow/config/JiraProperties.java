package com.checkmarx.flow.config;

import com.checkmarx.flow.dto.Field;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "jira")
@Validated
public class JiraProperties {
    private String url;
    private String username;
    private String token;
    private String project;
    private String issueType;
    private String labelTracker = "labels";
    private String issuePrefix;
    private String issuePostfix;
    private String descriptionPrefix;
    private String descriptionPostfix;
    private String appLabelPrefix = "app";
    private String ownerLabelPrefix = "owner";
    private String repoLabelPrefix = "repo";
    private String branchLabelPrefix = "branch";
    private String falsePositiveLabel = "false-positive";
    private String falsePositiveStatus = "FALSE-POSITIVE";
    private Map<String, String> priorities;
    private String openTransition;
    private String closeTransitionField;
    private String closeTransitionValue;
    private String closeTransition;
    private boolean updateComment = false;
    private String updateCommentValue = "Issue still remains";
    private List<String> openStatus;
    private List<String> closedStatus;
    private List<Field> fields;
    private String ParentUrl = "";
    private boolean child = false;

    public String getUrl() {
        return this.url;
    }

    public String getUsername() {
        return this.username;
    }

    public String getToken() {
        return this.token;
    }

    public String getProject() {
        return this.project;
    }

    public String getIssueType() {
        return this.issueType;
    }

    public String getLabelTracker() {
        return this.labelTracker;
    }

    public String getIssuePrefix() {
        return this.issuePrefix;
    }

    public String getAppLabelPrefix() {
        return this.appLabelPrefix;
    }

    public String getOwnerLabelPrefix() {
        return this.ownerLabelPrefix;
    }

    public String getRepoLabelPrefix() {
        return this.repoLabelPrefix;
    }

    public String getBranchLabelPrefix() {
        return this.branchLabelPrefix;
    }

    public String getFalsePositiveLabel() {
        return this.falsePositiveLabel;
    }

    public String getFalsePositiveStatus() {
        return this.falsePositiveStatus;
    }

    public Map<String, String> getPriorities() {
        return this.priorities;
    }

    public String getOpenTransition() {
        return this.openTransition;
    }

    public String getCloseTransitionField() {
        return this.closeTransitionField;
    }

    public String getCloseTransitionValue() {
        return this.closeTransitionValue;
    }

    public String getCloseTransition() {
        return this.closeTransition;
    }

    public List<String> getOpenStatus() {
        return this.openStatus;
    }

    public List<String> getClosedStatus() {
        return this.closedStatus;
    }

    public List<Field> getFields() {
        return this.fields;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public void setLabelTracker(String labelTracker) {
        this.labelTracker = labelTracker;
    }

    public void setIssuePrefix(String issuePrefix) {
        this.issuePrefix = issuePrefix;
    }

    public void setAppLabelPrefix(String appLabelPrefix) {
        this.appLabelPrefix = appLabelPrefix;
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

    public void setFalsePositiveLabel(String falsePositiveLabel) {
        this.falsePositiveLabel = falsePositiveLabel;
    }

    public void setFalsePositiveStatus(String falsePositiveStatus) {
        this.falsePositiveStatus = falsePositiveStatus;
    }

    public void setPriorities(Map<String, String> priorities) {
        this.priorities = priorities;
    }

    public void setOpenTransition(String openTransition) {
        this.openTransition = openTransition;
    }

    public void setCloseTransitionField(String closeTransitionField) {
        this.closeTransitionField = closeTransitionField;
    }

    public void setCloseTransitionValue(String closeTransitionValue) {
        this.closeTransitionValue = closeTransitionValue;
    }

    public void setCloseTransition(String closeTransition) {
        this.closeTransition = closeTransition;
    }

    public void setOpenStatus(List<String> openStatus) {
        this.openStatus = openStatus;
    }

    public void setClosedStatus(List<String> closedStatus) {
        this.closedStatus = closedStatus;
    }

    public boolean isUpdateComment() {
        return updateComment;
    }

    public void setUpdateComment(boolean updateComment) {
        this.updateComment = updateComment;
    }

    public String getUpdateCommentValue() {
        return updateCommentValue;
    }

    public void setUpdateCommentValue(String updateCommentValue) {
        this.updateCommentValue = updateCommentValue;
    }

    public String getIssuePostfix() {
        return issuePostfix;
    }

    public void setIssuePostfix(String issuePostfix) {
        this.issuePostfix = issuePostfix;
    }

    public String getDescriptionPrefix() {
        return descriptionPrefix;
    }

    public void setDescriptionPrefix(String descriptionPrefix) {
        this.descriptionPrefix = descriptionPrefix;
    }

    public String getDescriptionPostfix() {
        return descriptionPostfix;
    }

    public void setDescriptionPostfix(String descriptionPostfix) {
        this.descriptionPostfix = descriptionPostfix;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public String getParentUrl() {
        return ParentUrl;
    }

    public void setParentUrl(String ParentUrl) {
        this.ParentUrl = ParentUrl;
    }

    public boolean isChild() {
        return child;
    }

    public void setChild(boolean child) {
        this.child = child;
    }
    
}
