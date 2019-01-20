
package com.custodela.machina.dto.github;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "url",
    "id",
    "html_url",
    "diff_url",
    "patch_url",
    "issue_url",
    "number",
    "state",
    "locked",
    "title",
    "user",
    "body",
    "created_at",
    "updated_at",
    "closed_at",
    "merged_at",
    "merge_commit_sha",
    "assignee",
    "milestone",
    "commits_url",
    "review_comments_url",
    "review_comment_url",
    "comments_url",
    "statuses_url",
    "head",
    "base",
    "_links",
    "merged",
    "mergeable",
    "mergeable_state",
    "merged_by",
    "comments",
    "review_comments",
    "commits",
    "additions",
    "deletions",
    "changed_files"
})
public class PullRequest {

    @JsonProperty("url")
    private String url;
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("html_url")
    private String htmlUrl;
    @JsonProperty("diff_url")
    private String diffUrl;
    @JsonProperty("patch_url")
    private String patchUrl;
    @JsonProperty("issue_url")
    private String issueUrl;
    @JsonProperty("number")
    private Integer number;
    @JsonProperty("state")
    private String state;
    @JsonProperty("locked")
    private Boolean locked;
    @JsonProperty("title")
    private String title;
    @JsonProperty("user")
    private User user;
    @JsonProperty("body")
    private String body;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("closed_at")
    private Object closedAt;
    @JsonProperty("merged_at")
    private Object mergedAt;
    @JsonProperty("merge_commit_sha")
    private Object mergeCommitSha;
    @JsonProperty("assignee")
    private Object assignee;
    @JsonProperty("milestone")
    private Object milestone;
    @JsonProperty("commits_url")
    private String commitsUrl;
    @JsonProperty("review_comments_url")
    private String reviewCommentsUrl;
    @JsonProperty("review_comment_url")
    private String reviewCommentUrl;
    @JsonProperty("comments_url")
    private String commentsUrl;
    @JsonProperty("statuses_url")
    private String statusesUrl;
    @JsonProperty("head")
    private Head head;
    @JsonProperty("base")
    private Base base;
    @JsonProperty("_links")
    private Links links;
    @JsonProperty("merged")
    private Boolean merged;
    @JsonProperty("mergeable")
    private Object mergeable;
    @JsonProperty("mergeable_state")
    private String mergeableState;
    @JsonProperty("merged_by")
    private Object mergedBy;
    @JsonProperty("comments")
    private Integer comments;
    @JsonProperty("review_comments")
    private Integer reviewComments;
    @JsonProperty("commits")
    private Integer commits;
    @JsonProperty("additions")
    private Integer additions;
    @JsonProperty("deletions")
    private Integer deletions;
    @JsonProperty("changed_files")
    private Integer changedFiles;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    @JsonProperty("html_url")
    public String getHtmlUrl() {
        return htmlUrl;
    }

    @JsonProperty("html_url")
    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    @JsonProperty("diff_url")
    public String getDiffUrl() {
        return diffUrl;
    }

    @JsonProperty("diff_url")
    public void setDiffUrl(String diffUrl) {
        this.diffUrl = diffUrl;
    }

    @JsonProperty("patch_url")
    public String getPatchUrl() {
        return patchUrl;
    }

    @JsonProperty("patch_url")
    public void setPatchUrl(String patchUrl) {
        this.patchUrl = patchUrl;
    }

    @JsonProperty("issue_url")
    public String getIssueUrl() {
        return issueUrl;
    }

    @JsonProperty("issue_url")
    public void setIssueUrl(String issueUrl) {
        this.issueUrl = issueUrl;
    }

    @JsonProperty("number")
    public Integer getNumber() {
        return number;
    }

    @JsonProperty("number")
    public void setNumber(Integer number) {
        this.number = number;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("locked")
    public Boolean getLocked() {
        return locked;
    }

    @JsonProperty("locked")
    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("user")
    public User getUser() {
        return user;
    }

    @JsonProperty("user")
    public void setUser(User user) {
        this.user = user;
    }

    @JsonProperty("body")
    public String getBody() {
        return body;
    }

    @JsonProperty("body")
    public void setBody(String body) {
        this.body = body;
    }

    @JsonProperty("created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @JsonProperty("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }

    @JsonProperty("updated_at")
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @JsonProperty("closed_at")
    public Object getClosedAt() {
        return closedAt;
    }

    @JsonProperty("closed_at")
    public void setClosedAt(Object closedAt) {
        this.closedAt = closedAt;
    }

    @JsonProperty("merged_at")
    public Object getMergedAt() {
        return mergedAt;
    }

    @JsonProperty("merged_at")
    public void setMergedAt(Object mergedAt) {
        this.mergedAt = mergedAt;
    }

    @JsonProperty("merge_commit_sha")
    public Object getMergeCommitSha() {
        return mergeCommitSha;
    }

    @JsonProperty("merge_commit_sha")
    public void setMergeCommitSha(Object mergeCommitSha) {
        this.mergeCommitSha = mergeCommitSha;
    }

    @JsonProperty("assignee")
    public Object getAssignee() {
        return assignee;
    }

    @JsonProperty("assignee")
    public void setAssignee(Object assignee) {
        this.assignee = assignee;
    }

    @JsonProperty("milestone")
    public Object getMilestone() {
        return milestone;
    }

    @JsonProperty("milestone")
    public void setMilestone(Object milestone) {
        this.milestone = milestone;
    }

    @JsonProperty("commits_url")
    public String getCommitsUrl() {
        return commitsUrl;
    }

    @JsonProperty("commits_url")
    public void setCommitsUrl(String commitsUrl) {
        this.commitsUrl = commitsUrl;
    }

    @JsonProperty("review_comments_url")
    public String getReviewCommentsUrl() {
        return reviewCommentsUrl;
    }

    @JsonProperty("review_comments_url")
    public void setReviewCommentsUrl(String reviewCommentsUrl) {
        this.reviewCommentsUrl = reviewCommentsUrl;
    }

    @JsonProperty("review_comment_url")
    public String getReviewCommentUrl() {
        return reviewCommentUrl;
    }

    @JsonProperty("review_comment_url")
    public void setReviewCommentUrl(String reviewCommentUrl) {
        this.reviewCommentUrl = reviewCommentUrl;
    }

    @JsonProperty("comments_url")
    public String getCommentsUrl() {
        return commentsUrl;
    }

    @JsonProperty("comments_url")
    public void setCommentsUrl(String commentsUrl) {
        this.commentsUrl = commentsUrl;
    }

    @JsonProperty("statuses_url")
    public String getStatusesUrl() {
        return statusesUrl;
    }

    @JsonProperty("statuses_url")
    public void setStatusesUrl(String statusesUrl) {
        this.statusesUrl = statusesUrl;
    }

    @JsonProperty("head")
    public Head getHead() {
        return head;
    }

    @JsonProperty("head")
    public void setHead(Head head) {
        this.head = head;
    }

    @JsonProperty("base")
    public Base getBase() {
        return base;
    }

    @JsonProperty("base")
    public void setBase(Base base) {
        this.base = base;
    }

    @JsonProperty("_links")
    public Links getLinks() {
        return links;
    }

    @JsonProperty("_links")
    public void setLinks(Links links) {
        this.links = links;
    }

    @JsonProperty("merged")
    public Boolean getMerged() {
        return merged;
    }

    @JsonProperty("merged")
    public void setMerged(Boolean merged) {
        this.merged = merged;
    }

    @JsonProperty("mergeable")
    public Object getMergeable() {
        return mergeable;
    }

    @JsonProperty("mergeable")
    public void setMergeable(Object mergeable) {
        this.mergeable = mergeable;
    }

    @JsonProperty("mergeable_state")
    public String getMergeableState() {
        return mergeableState;
    }

    @JsonProperty("mergeable_state")
    public void setMergeableState(String mergeableState) {
        this.mergeableState = mergeableState;
    }

    @JsonProperty("merged_by")
    public Object getMergedBy() {
        return mergedBy;
    }

    @JsonProperty("merged_by")
    public void setMergedBy(Object mergedBy) {
        this.mergedBy = mergedBy;
    }

    @JsonProperty("comments")
    public Integer getComments() {
        return comments;
    }

    @JsonProperty("comments")
    public void setComments(Integer comments) {
        this.comments = comments;
    }

    @JsonProperty("review_comments")
    public Integer getReviewComments() {
        return reviewComments;
    }

    @JsonProperty("review_comments")
    public void setReviewComments(Integer reviewComments) {
        this.reviewComments = reviewComments;
    }

    @JsonProperty("commits")
    public Integer getCommits() {
        return commits;
    }

    @JsonProperty("commits")
    public void setCommits(Integer commits) {
        this.commits = commits;
    }

    @JsonProperty("additions")
    public Integer getAdditions() {
        return additions;
    }

    @JsonProperty("additions")
    public void setAdditions(Integer additions) {
        this.additions = additions;
    }

    @JsonProperty("deletions")
    public Integer getDeletions() {
        return deletions;
    }

    @JsonProperty("deletions")
    public void setDeletions(Integer deletions) {
        this.deletions = deletions;
    }

    @JsonProperty("changed_files")
    public Integer getChangedFiles() {
        return changedFiles;
    }

    @JsonProperty("changed_files")
    public void setChangedFiles(Integer changedFiles) {
        this.changedFiles = changedFiles;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
