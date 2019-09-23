
package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "commits",
    "refUpdates",
    "repository",
    "pushedBy",
    "pushId",
    "date",
    "pullRequestId",
    "codeReviewId",
    "status",
    "createdBy",
    "creationDate",
    "title",
    "description",
    "sourceRefName",
    "targetRefName",
    "mergeStatus",
    "isDraft",
    "mergeId",
    "lastMergeSourceCommit",
    "lastMergeTargetCommit",
    "lastMergeCommit",
    "reviewers",
    "url",
    "_links",
    "supportsIterations",
    "artifactId"
})
public class Resource {

    @JsonProperty("commits")
    private List<Commit> commits = null;
    @JsonProperty("refUpdates")
    private List<RefUpdate> refUpdates = null;
    @JsonProperty("repository")
    private Repository repository;
    @JsonProperty("pushedBy")
    private PushedBy pushedBy;
    @JsonProperty("pushId")
    private Integer pushId;
    @JsonProperty("date")
    private String date;
    @JsonProperty("pullRequestId")
    private Integer pullRequestId;
    @JsonProperty("codeReviewId")
    private Integer codeReviewId;
    @JsonProperty("status")
    private String status;
    @JsonProperty("createdBy")
    private CreatedBy createdBy;
    @JsonProperty("creationDate")
    private String creationDate;
    @JsonProperty("title")
    private String title;
    @JsonProperty("description")
    private String description;
    @JsonProperty("sourceRefName")
    private String sourceRefName;
    @JsonProperty("targetRefName")
    private String targetRefName;
    @JsonProperty("mergeStatus")
    private String mergeStatus;
    @JsonProperty("isDraft")
    private Boolean isDraft;
    @JsonProperty("mergeId")
    private String mergeId;
    @JsonProperty("lastMergeSourceCommit")
    private LastMergeSourceCommit lastMergeSourceCommit;
    @JsonProperty("lastMergeTargetCommit")
    private LastMergeTargetCommit lastMergeTargetCommit;
    @JsonProperty("lastMergeCommit")
    private LastMergeCommit lastMergeCommit;
    @JsonProperty("reviewers")
    private List<Object> reviewers = null;
    @JsonProperty("url")
    private String url;
    @JsonProperty("_links")
    private Links_ links;
    @JsonProperty("supportsIterations")
    private Boolean supportsIterations;
    @JsonProperty("artifactId")
    private String artifactId;

    @JsonProperty("pullRequestId")
    public Integer getPullRequestId() {
        return pullRequestId;
    }

    @JsonProperty("pullRequestId")
    public void setPullRequestId(Integer pullRequestId) {
        this.pullRequestId = pullRequestId;
    }

    @JsonProperty("codeReviewId")
    public Integer getCodeReviewId() {
        return codeReviewId;
    }

    @JsonProperty("codeReviewId")
    public void setCodeReviewId(Integer codeReviewId) {
        this.codeReviewId = codeReviewId;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("createdBy")
    public CreatedBy getCreatedBy() {
        return createdBy;
    }

    @JsonProperty("createdBy")
    public void setCreatedBy(CreatedBy createdBy) {
        this.createdBy = createdBy;
    }

    @JsonProperty("creationDate")
    public String getCreationDate() {
        return creationDate;
    }

    @JsonProperty("creationDate")
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("commits")
    public List<Commit> getCommits() {
        return commits;
    }

    @JsonProperty("commits")
    public void setCommits(List<Commit> commits) {
        this.commits = commits;
    }

    @JsonProperty("refUpdates")
    public List<RefUpdate> getRefUpdates() {
        return refUpdates;
    }

    @JsonProperty("refUpdates")
    public void setRefUpdates(List<RefUpdate> refUpdates) {
        this.refUpdates = refUpdates;
    }

    @JsonProperty("repository")
    public Repository getRepository() {
        return repository;
    }
    
    @JsonProperty("repository")
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("sourceRefName")
    public String getSourceRefName() {
        return sourceRefName;
    }

    @JsonProperty("sourceRefName")
    public void setSourceRefName(String sourceRefName) {
        this.sourceRefName = sourceRefName;
    }

    @JsonProperty("targetRefName")
    public String getTargetRefName() {
        return targetRefName;
    }

    @JsonProperty("targetRefName")
    public void setTargetRefName(String targetRefName) {
        this.targetRefName = targetRefName;
    }

    @JsonProperty("mergeStatus")
    public String getMergeStatus() {
        return mergeStatus;
    }

    @JsonProperty("mergeStatus")
    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    @JsonProperty("isDraft")
    public Boolean getIsDraft() {
        return isDraft;
    }

    @JsonProperty("isDraft")
    public void setIsDraft(Boolean isDraft) {
        this.isDraft = isDraft;
    }

    @JsonProperty("mergeId")
    public String getMergeId() {
        return mergeId;
    }

    @JsonProperty("mergeId")
    public void setMergeId(String mergeId) {
        this.mergeId = mergeId;
    }

    @JsonProperty("lastMergeSourceCommit")
    public LastMergeSourceCommit getLastMergeSourceCommit() {
        return lastMergeSourceCommit;
    }

    @JsonProperty("lastMergeSourceCommit")
    public void setLastMergeSourceCommit(LastMergeSourceCommit lastMergeSourceCommit) {
        this.lastMergeSourceCommit = lastMergeSourceCommit;
    }

    @JsonProperty("lastMergeTargetCommit")
    public LastMergeTargetCommit getLastMergeTargetCommit() {
        return lastMergeTargetCommit;
    }

    @JsonProperty("lastMergeTargetCommit")
    public void setLastMergeTargetCommit(LastMergeTargetCommit lastMergeTargetCommit) {
        this.lastMergeTargetCommit = lastMergeTargetCommit;
    }

    @JsonProperty("lastMergeCommit")
    public LastMergeCommit getLastMergeCommit() {
        return lastMergeCommit;
    }

    @JsonProperty("lastMergeCommit")
    public void setLastMergeCommit(LastMergeCommit lastMergeCommit) {
        this.lastMergeCommit = lastMergeCommit;
    }

    @JsonProperty("reviewers")
    public List<Object> getReviewers() {
        return reviewers;
    }

    @JsonProperty("reviewers")
    public void setReviewers(List<Object> reviewers) {
        this.reviewers = reviewers;
    }

    @JsonProperty("pushedBy")
    public PushedBy getPushedBy() {
        return pushedBy;
    }

    @JsonProperty("pushedBy")
    public void setPushedBy(PushedBy pushedBy) {
        this.pushedBy = pushedBy;
    }

    @JsonProperty("pushId")
    public Integer getPushId() {
        return pushId;
    }

    @JsonProperty("pushId")
    public void setPushId(Integer pushId) {
        this.pushId = pushId;
    }

    @JsonProperty("date")
    public String getDate() {
        return date;
    }

    @JsonProperty("date")
    public void setDate(String date) {
        this.date = date;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("_links")
    public Links_ getLinks() {
        return links;
    }

    @JsonProperty("_links")
    public void setLinks(Links_ links) {
        this.links = links;
    }

    @JsonProperty("supportsIterations")
    public Boolean getSupportsIterations() {
        return supportsIterations;
    }

    @JsonProperty("supportsIterations")
    public void setSupportsIterations(Boolean supportsIterations) {
        this.supportsIterations = supportsIterations;
    }

    @JsonProperty("artifactId")
    public String getArtifactId() {
        return artifactId;
    }

    @JsonProperty("artifactId")
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

}
