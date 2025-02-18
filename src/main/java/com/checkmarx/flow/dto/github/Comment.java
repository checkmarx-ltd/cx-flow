package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Comment {


    @JsonProperty("url")
    String url;

    @JsonProperty("html_url")
    String htmlUrl;

    @JsonProperty("issue_url")
    String issueUrl;

    @JsonProperty("id")
    String id;

    @JsonProperty("node_id")
    String nodeId;

    @JsonProperty("user")
    User user;

    @JsonProperty("created_at")
    String createdAt;

    @JsonProperty("updated_at")
    String updatedAt;

    @JsonProperty("author_association")
    String authorAssociation;

    @JsonProperty("body")
    String body;

    @JsonProperty("reactions")
    Reactions reactions;

    @JsonProperty("performed_via_github_app")
    String performedViaGithubApp;


    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }
    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setIssueUrl(String issueUrl) {
        this.issueUrl = issueUrl;
    }
    public String getIssueUrl() {
        return issueUrl;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    public String getNodeId() {
        return nodeId;
    }

    public void setUser(User user) {
        this.user = user;
    }
    public User getUser() {
        return user;
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

    public void setAuthorAssociation(String authorAssociation) {
        this.authorAssociation = authorAssociation;
    }
    public String getAuthorAssociation() {
        return authorAssociation;
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

    public void setPerformedViaGithubApp(String performedViaGithubApp) {
        this.performedViaGithubApp = performedViaGithubApp;
    }
    public String getPerformedViaGithubApp() {
        return performedViaGithubApp;
    }
}
