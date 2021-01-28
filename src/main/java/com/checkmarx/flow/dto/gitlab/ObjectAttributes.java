
package com.checkmarx.flow.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import javax.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "target_branch",
    "source_branch",
    "source_project_id",
    "author_id",
    "assignee_id",
    "title",
    "created_at",
    "updated_at",
    "milestone_id",
    "state",
    "merge_status",
    "target_project_id",
    "iid",
    "description",
    "source",
    "target",
    "last_commit",
    "work_in_progress",
    "url",
    "action",
    "assignee"
})
@Builder
public class ObjectAttributes {

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("target_branch")
    private String targetBranch;
    @JsonProperty("source_branch")
    private String sourceBranch;
    @JsonProperty("source_project_id")
    private Integer sourceProjectId;
    @JsonProperty("author_id")
    private Integer authorId;
    @JsonProperty("assignee_id")
    private Integer assigneeId;
    @JsonProperty("title")
    private String title;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("milestone_id")
    private Object milestoneId;
    @JsonProperty("state")
    private String state;
    @JsonProperty("merge_status")
    private String mergeStatus;
    @JsonProperty("target_project_id")
    private Integer targetProjectId;
    @JsonProperty("iid")
    private Integer iid;
    @JsonProperty("description")
    private String description;
    @JsonProperty("source")
    @Valid
    private Source source;
    @JsonProperty("target")
    @Valid
    private Target target;
    @JsonProperty("last_commit")
    @Valid
    private LastCommit lastCommit;
    @JsonProperty("work_in_progress")
    private Boolean workInProgress;
    @JsonProperty("url")
    private String url;
    @JsonProperty("action")
    private String action;
    @JsonProperty("assignee")
    @Valid
    private Assignee assignee;

    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    public ObjectAttributes withId(Integer id) {
        this.id = id;
        return this;
    }

    @JsonProperty("target_branch")
    public String getTargetBranch() {
        return targetBranch;
    }

    @JsonProperty("target_branch")
    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public ObjectAttributes withTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
        return this;
    }

    @JsonProperty("source_branch")
    public String getSourceBranch() {
        return sourceBranch;
    }

    @JsonProperty("source_branch")
    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public ObjectAttributes withSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
        return this;
    }

    @JsonProperty("source_project_id")
    public Integer getSourceProjectId() {
        return sourceProjectId;
    }

    @JsonProperty("source_project_id")
    public void setSourceProjectId(Integer sourceProjectId) {
        this.sourceProjectId = sourceProjectId;
    }

    public ObjectAttributes withSourceProjectId(Integer sourceProjectId) {
        this.sourceProjectId = sourceProjectId;
        return this;
    }

    @JsonProperty("author_id")
    public Integer getAuthorId() {
        return authorId;
    }

    @JsonProperty("author_id")
    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public ObjectAttributes withAuthorId(Integer authorId) {
        this.authorId = authorId;
        return this;
    }

    @JsonProperty("assignee_id")
    public Integer getAssigneeId() {
        return assigneeId;
    }

    @JsonProperty("assignee_id")
    public void setAssigneeId(Integer assigneeId) {
        this.assigneeId = assigneeId;
    }

    public ObjectAttributes withAssigneeId(Integer assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    public ObjectAttributes withTitle(String title) {
        this.title = title;
        return this;
    }

    @JsonProperty("created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public ObjectAttributes withCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @JsonProperty("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }

    @JsonProperty("updated_at")
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ObjectAttributes withUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    @JsonProperty("milestone_id")
    public Object getMilestoneId() {
        return milestoneId;
    }

    @JsonProperty("milestone_id")
    public void setMilestoneId(Object milestoneId) {
        this.milestoneId = milestoneId;
    }

    public ObjectAttributes withMilestoneId(Object milestoneId) {
        this.milestoneId = milestoneId;
        return this;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    public ObjectAttributes withState(String state) {
        this.state = state;
        return this;
    }

    @JsonProperty("merge_status")
    public String getMergeStatus() {
        return mergeStatus;
    }

    @JsonProperty("merge_status")
    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    public ObjectAttributes withMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
        return this;
    }

    @JsonProperty("target_project_id")
    public Integer getTargetProjectId() {
        return targetProjectId;
    }

    @JsonProperty("target_project_id")
    public void setTargetProjectId(Integer targetProjectId) {
        this.targetProjectId = targetProjectId;
    }

    public ObjectAttributes withTargetProjectId(Integer targetProjectId) {
        this.targetProjectId = targetProjectId;
        return this;
    }

    @JsonProperty("iid")
    public Integer getIid() {
        return iid;
    }

    @JsonProperty("iid")
    public void setIid(Integer iid) {
        this.iid = iid;
    }

    public ObjectAttributes withIid(Integer iid) {
        this.iid = iid;
        return this;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    public ObjectAttributes withDescription(String description) {
        this.description = description;
        return this;
    }

    @JsonProperty("source")
    public Source getSource() {
        return source;
    }

    @JsonProperty("source")
    public void setSource(Source source) {
        this.source = source;
    }

    public ObjectAttributes withSource(Source source) {
        this.source = source;
        return this;
    }

    @JsonProperty("target")
    public Target getTarget() {
        return target;
    }

    @JsonProperty("target")
    public void setTarget(Target target) {
        this.target = target;
    }

    public ObjectAttributes withTarget(Target target) {
        this.target = target;
        return this;
    }

    @JsonProperty("last_commit")
    public LastCommit getLastCommit() {
        return lastCommit;
    }

    @JsonProperty("last_commit")
    public void setLastCommit(LastCommit lastCommit) {
        this.lastCommit = lastCommit;
    }

    public ObjectAttributes withLastCommit(LastCommit lastCommit) {
        this.lastCommit = lastCommit;
        return this;
    }

    @JsonProperty("work_in_progress")
    public Boolean getWorkInProgress() {
        return workInProgress;
    }

    @JsonProperty("work_in_progress")
    public void setWorkInProgress(Boolean workInProgress) {
        this.workInProgress = workInProgress;
    }

    public ObjectAttributes withWorkInProgress(Boolean workInProgress) {
        this.workInProgress = workInProgress;
        return this;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    public ObjectAttributes withUrl(String url) {
        this.url = url;
        return this;
    }

    @JsonProperty("action")
    public String getAction() {
        return action;
    }

    @JsonProperty("action")
    public void setAction(String action) {
        this.action = action;
    }

    public ObjectAttributes withAction(String action) {
        this.action = action;
        return this;
    }

    @JsonProperty("assignee")
    public Assignee getAssignee() {
        return assignee;
    }

    @JsonProperty("assignee")
    public void setAssignee(Assignee assignee) {
        this.assignee = assignee;
    }

    public ObjectAttributes withAssignee(Assignee assignee) {
        this.assignee = assignee;
        return this;
    }

}
