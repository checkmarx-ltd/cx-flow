
package com.checkmarx.flow.dto.github;

import com.checkmarx.flow.dto.RepoIssue;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.beans.ConstructorProperties;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Issue extends RepoIssue {

    @JsonProperty("pull_request")
    private PullRequest pullRequest;

    @JsonProperty("comments")
    private int comments;

    @JsonProperty("closed_at")
    private Object closedAt;

    @JsonProperty("assignees")
    private List<AssigneesItem> assignees;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("title")
    private String title;

    @JsonProperty("body")
    private String body;
    
    @JsonProperty("url")
    private String url;

    @JsonProperty("labels")
    private List<LabelsItem> labels;

    @JsonProperty("closed_by")
    private ClosedBy closedBy;

    @JsonProperty("labels_url")
    private String labelsUrl;

    @JsonProperty("number")
    private int number;

    @JsonProperty("milestone")
    private Milestone milestone;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("events_url")
    private String eventsUrl;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("comments_url")
    private String commentsUrl;

    @JsonProperty("id")
    private int id;

    @JsonProperty("repository_url")
    private String repositoryUrl;

    @JsonProperty("state")
    private String state;

    @JsonProperty("assignee")
    private Assignee assignee;

    @JsonProperty("locked")
    private boolean locked;

    @JsonProperty("user")
    private User user;

    @JsonProperty("href")
    private String href;

    @ConstructorProperties({"pullRequest", "comments", "closedAt", "assignees", "createdAt", "title", "body", "url", "labels", "closedBy", "labelsUrl", "number", "milestone", "updatedAt", "eventsUrl", "htmlUrl", "commentsUrl", "id", "repositoryUrl", "state", "assignee", "locked", "user", "href"})
    public Issue(PullRequest pullRequest, int comments, Object closedAt, List<AssigneesItem> assignees, String createdAt, String title, String body, String url, List<LabelsItem> labels, ClosedBy closedBy, String labelsUrl, int number, Milestone milestone, String updatedAt, String eventsUrl, String htmlUrl, String commentsUrl, int id, String repositoryUrl, String state, Assignee assignee, boolean locked, User user, String href) {
        this.pullRequest = pullRequest;
        this.comments = comments;
        this.closedAt = closedAt;
        this.assignees = assignees;
        this.createdAt = createdAt;
        this.title = title;
        this.body = body;
        this.url = url;
        this.labels = labels;
        this.closedBy = closedBy;
        this.labelsUrl = labelsUrl;
        this.number = number;
        this.milestone = milestone;
        this.updatedAt = updatedAt;
        this.eventsUrl = eventsUrl;
        this.htmlUrl = htmlUrl;
        this.commentsUrl = commentsUrl;
        this.id = id;
        this.repositoryUrl = repositoryUrl;
        this.state = state;
        this.assignee = assignee;
        this.locked = locked;
        this.user = user;
        this.href = href;
    }

    public Issue() {
    }

    public static IssueBuilder builder() {
        return new IssueBuilder();
    }

    public PullRequest getPullRequest() {
        return this.pullRequest;
    }

    public int getComments() {
        return this.comments;
    }

    public Object getClosedAt() {
        return this.closedAt;
    }

    public List<AssigneesItem> getAssignees() {
        return this.assignees;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    public String getBody() {
        return this.body;
    }

    public String getUrl() {
        return this.url;
    }

    public List<LabelsItem> getLabels() {
        return this.labels;
    }

    public ClosedBy getClosedBy() {
        return this.closedBy;
    }

    public String getLabelsUrl() {
        return this.labelsUrl;
    }

    public int getNumber() {
        return this.number;
    }

    public Milestone getMilestone() {
        return this.milestone;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public String getEventsUrl() {
        return this.eventsUrl;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public String getCommentsUrl() {
        return this.commentsUrl;
    }

    public int getId() {
        return this.id;
    }

    public String getRepositoryUrl() {
        return this.repositoryUrl;
    }

    public String getState() {
        return this.state;
    }

    public Assignee getAssignee() {
        return this.assignee;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public User getUser() {
        return this.user;
    }

    public String getHref() {
        return this.href;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public void setClosedAt(Object closedAt) {
        this.closedAt = closedAt;
    }

    public void setAssignees(List<AssigneesItem> assignees) {
        this.assignees = assignees;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setLabels(List<LabelsItem> labels) {
        this.labels = labels;
    }

    public void setClosedBy(ClosedBy closedBy) {
        this.closedBy = closedBy;
    }

    public void setLabelsUrl(String labelsUrl) {
        this.labelsUrl = labelsUrl;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setMilestone(Milestone milestone) {
        this.milestone = milestone;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setEventsUrl(String eventsUrl) {
        this.eventsUrl = eventsUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public void setCommentsUrl(String commentsUrl) {
        this.commentsUrl = commentsUrl;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setAssignee(Assignee assignee) {
        this.assignee = assignee;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String toString() {
        return "Issue(pullRequest=" + this.getPullRequest() + ", comments=" + this.getComments() + ", closedAt=" + this.getClosedAt() + ", assignees=" + this.getAssignees() + ", createdAt=" + this.getCreatedAt() + ", title=" + this.getTitle() + ", body=" + this.getBody() + ", url=" + this.getUrl() + ", labels=" + this.getLabels() + ", closedBy=" + this.getClosedBy() + ", labelsUrl=" + this.getLabelsUrl() + ", number=" + this.getNumber() + ", milestone=" + this.getMilestone() + ", updatedAt=" + this.getUpdatedAt() + ", eventsUrl=" + this.getEventsUrl() + ", htmlUrl=" + this.getHtmlUrl() + ", commentsUrl=" + this.getCommentsUrl() + ", id=" + this.getId() + ", repositoryUrl=" + this.getRepositoryUrl() + ", state=" + this.getState() + ", assignee=" + this.getAssignee() + ", locked=" + this.isLocked() + ", user=" + this.getUser() + ", href=" + this.getHref() + ")";
    }

    public static class IssueBuilder {
        private PullRequest pullRequest;
        private int comments;
        private Object closedAt;
        private List<AssigneesItem> assignees;
        private String createdAt;
        private String title;
        private String body;
        private String url;
        private List<LabelsItem> labels;
        private ClosedBy closedBy;
        private String labelsUrl;
        private int number;
        private Milestone milestone;
        private String updatedAt;
        private String eventsUrl;
        private String htmlUrl;
        private String commentsUrl;
        private int id;
        private String repositoryUrl;
        private String state;
        private Assignee assignee;
        private boolean locked;
        private User user;
        private String href;

        IssueBuilder() {
        }

        public Issue.IssueBuilder pullRequest(PullRequest pullRequest) {
            this.pullRequest = pullRequest;
            return this;
        }

        public Issue.IssueBuilder comments(int comments) {
            this.comments = comments;
            return this;
        }

        public Issue.IssueBuilder closedAt(Object closedAt) {
            this.closedAt = closedAt;
            return this;
        }

        public Issue.IssueBuilder assignees(List<AssigneesItem> assignees) {
            this.assignees = assignees;
            return this;
        }

        public Issue.IssueBuilder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Issue.IssueBuilder title(String title) {
            this.title = title;
            return this;
        }

        public Issue.IssueBuilder body(String body) {
            this.body = body;
            return this;
        }

        public Issue.IssueBuilder url(String url) {
            this.url = url;
            return this;
        }

        public Issue.IssueBuilder labels(List<LabelsItem> labels) {
            this.labels = labels;
            return this;
        }

        public Issue.IssueBuilder closedBy(ClosedBy closedBy) {
            this.closedBy = closedBy;
            return this;
        }

        public Issue.IssueBuilder labelsUrl(String labelsUrl) {
            this.labelsUrl = labelsUrl;
            return this;
        }

        public Issue.IssueBuilder number(int number) {
            this.number = number;
            return this;
        }

        public Issue.IssueBuilder milestone(Milestone milestone) {
            this.milestone = milestone;
            return this;
        }

        public Issue.IssueBuilder updatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Issue.IssueBuilder eventsUrl(String eventsUrl) {
            this.eventsUrl = eventsUrl;
            return this;
        }

        public Issue.IssueBuilder htmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
            return this;
        }

        public Issue.IssueBuilder commentsUrl(String commentsUrl) {
            this.commentsUrl = commentsUrl;
            return this;
        }

        public Issue.IssueBuilder id(int id) {
            this.id = id;
            return this;
        }

        public Issue.IssueBuilder repositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
            return this;
        }

        public Issue.IssueBuilder state(String state) {
            this.state = state;
            return this;
        }

        public Issue.IssueBuilder assignee(Assignee assignee) {
            this.assignee = assignee;
            return this;
        }

        public Issue.IssueBuilder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public Issue.IssueBuilder user(User user) {
            this.user = user;
            return this;
        }

        public Issue.IssueBuilder href(String href) {
            this.href = href;
            return this;
        }

        public Issue build() {
            return new Issue(pullRequest, comments, closedAt, assignees, createdAt, title, body, url, labels, closedBy, labelsUrl, number, milestone, updatedAt, eventsUrl, htmlUrl, commentsUrl, id, repositoryUrl, state, assignee, locked, user, href);
        }

        public String toString() {
            return "Issue.IssueBuilder(pullRequest=" + this.pullRequest + ", comments=" + this.comments + ", closedAt=" + this.closedAt + ", assignees=" + this.assignees + ", createdAt=" + this.createdAt + ", title=" + this.title + ", body=" + this.body + ", url=" + this.url + ", labels=" + this.labels + ", closedBy=" + this.closedBy + ", labelsUrl=" + this.labelsUrl + ", number=" + this.number + ", milestone=" + this.milestone + ", updatedAt=" + this.updatedAt + ", eventsUrl=" + this.eventsUrl + ", htmlUrl=" + this.htmlUrl + ", commentsUrl=" + this.commentsUrl + ", id=" + this.id + ", repositoryUrl=" + this.repositoryUrl + ", state=" + this.state + ", assignee=" + this.assignee + ", locked=" + this.locked + ", user=" + this.user + ", href=" + this.href + ")";
        }
    }
}
