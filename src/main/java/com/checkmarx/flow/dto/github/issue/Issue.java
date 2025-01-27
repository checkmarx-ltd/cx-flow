package com.checkmarx.flow.dto.github.issue;

import com.checkmarx.flow.dto.github.PullRequest;
import com.checkmarx.flow.dto.github.Reactions;
import com.checkmarx.flow.dto.github.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Issue {

   @JsonProperty("url")
   String url;

   @JsonProperty("repository_url")
   String repositoryUrl;

   @JsonProperty("labels_url")
   String labelsUrl;

   @JsonProperty("comments_url")
   String commentsUrl;

   @JsonProperty("events_url")
   String eventsUrl;

   @JsonProperty("html_url")
   String htmlUrl;

   @JsonProperty("id")
   int id;

   @JsonProperty("node_id")
   String nodeId;

   @JsonProperty("number")
   int number;

   @JsonProperty("title")
   String title;

   @JsonProperty("user")
   User user;

   @JsonProperty("labels")
   List<String> labels;

   @JsonProperty("state")
   String state;

   @JsonProperty("locked")
   boolean locked;

   @JsonProperty("assignee")
   String assignee;

   @JsonProperty("assignees")
   List<String> assignees;

   @JsonProperty("milestone")
   String milestone;

   @JsonProperty("comments")
   int comments;

   @JsonProperty("created_at")
   String createdAt;

   @JsonProperty("updated_at")
   String updatedAt;

   @JsonProperty("closed_at")

   String closedAt;

   @JsonProperty("author_association")
   String authorAssociation;

   @JsonProperty("sub_issues_summary")
   SubIssuesSummary subIssuesSummary;

   @JsonProperty("active_lock_reason")
   String activeLockReason;

   @JsonProperty("draft")
   boolean draft;

   @JsonProperty("pull_request")
   PullRequest pullRequest;

   @JsonProperty("body")
   String body;

   @JsonProperty("reactions")
   Reactions reactions;

   @JsonProperty("timeline_url")
   String timelineUrl;

   @JsonProperty("performed_via_github_app")
   String performedViaGithubApp;

   @JsonProperty("state_reason")
   String stateReason;


    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }
    
    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }
    public String getRepositoryUrl() {
        return repositoryUrl;
    }
    
    public void setLabelsUrl(String labelsUrl) {
        this.labelsUrl = labelsUrl;
    }
    public String getLabelsUrl() {
        return labelsUrl;
    }
    
    public void setCommentsUrl(String commentsUrl) {
        this.commentsUrl = commentsUrl;
    }
    public String getCommentsUrl() {
        return commentsUrl;
    }
    
    public void setEventsUrl(String eventsUrl) {
        this.eventsUrl = eventsUrl;
    }
    public String getEventsUrl() {
        return eventsUrl;
    }
    
    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }
    public String getHtmlUrl() {
        return htmlUrl;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNumber(int number) {
        this.number = number;
    }
    public int getNumber() {
        return number;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    public User getUser() {
        return user;
    }
    
    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
    public List<String> getLabels() {
        return labels;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    public String getState() {
        return state;
    }
    
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
    public boolean getLocked() {
        return locked;
    }
    
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    public String getAssignee() {
        return assignee;
    }
    
    public void setAssignees(List<String> assignees) {
        this.assignees = assignees;
    }
    public List<String> getAssignees() {
        return assignees;
    }
    
    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }
    public String getMilestone() {
        return milestone;
    }
    
    public void setComments(int comments) {
        this.comments = comments;
    }
    public int getComments() {
        return comments;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setClosedAt(String closedAt) {
        this.closedAt = closedAt;
    }
    public String getClosedAt() {
        return closedAt;
    }
    
    public void setAuthorAssociation(String authorAssociation) {
        this.authorAssociation = authorAssociation;
    }
    public String getAuthorAssociation() {
        return authorAssociation;
    }
    
    public void setSubIssuesSummary(SubIssuesSummary subIssuesSummary) {
        this.subIssuesSummary = subIssuesSummary;
    }
    public SubIssuesSummary getSubIssuesSummary() {
        return subIssuesSummary;
    }
    
    public void setActiveLockReason(String activeLockReason) {
        this.activeLockReason = activeLockReason;
    }
    public String getActiveLockReason() {
        return activeLockReason;
    }
    
    public void setDraft(boolean draft) {
        this.draft = draft;
    }
    public boolean getDraft() {
        return draft;
    }
    
    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }
    public PullRequest getPullRequest() {
        return pullRequest;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    public String getBody() {
        return body;
    }
    
    public void setReactions(Reactions reactions) {
        this.reactions = reactions;
    }
    public Reactions getReactions() {
        return reactions;
    }
    
    public void setTimelineUrl(String timelineUrl) {
        this.timelineUrl = timelineUrl;
    }
    public String getTimelineUrl() {
        return timelineUrl;
    }
    
    public void setPerformedViaGithubApp(String performedViaGithubApp) {
        this.performedViaGithubApp = performedViaGithubApp;
    }
    public String getPerformedViaGithubApp() {
        return performedViaGithubApp;
    }
    
    public void setStateReason(String stateReason) {
        this.stateReason = stateReason;
    }
    public String getStateReason() {
        return stateReason;
    }
    
}