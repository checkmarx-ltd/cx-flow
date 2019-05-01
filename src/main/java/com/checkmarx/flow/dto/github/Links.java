
package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "self",
    "html",
    "issue",
    "comments",
    "review_comments",
    "review_comment",
    "commits",
    "statuses"
})
public class Links {

    @JsonProperty("self")
    private Self self;
    @JsonProperty("html")
    private Html html;
    @JsonProperty("issue")
    private Issue issue;
    @JsonProperty("comments")
    private Comments comments;
    @JsonProperty("review_comments")
    private ReviewComments reviewComments;
    @JsonProperty("review_comment")
    private ReviewComment reviewComment;
    @JsonProperty("commits")
    private Commits commits;
    @JsonProperty("statuses")
    private Statuses statuses;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("self")
    public Self getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(Self self) {
        this.self = self;
    }

    @JsonProperty("html")
    public Html getHtml() {
        return html;
    }

    @JsonProperty("html")
    public void setHtml(Html html) {
        this.html = html;
    }

    @JsonProperty("issue")
    public Issue getIssue() {
        return issue;
    }

    @JsonProperty("issue")
    public void setIssue(Issue issue) {
        this.issue = issue;
    }

    @JsonProperty("comments")
    public Comments getComments() {
        return comments;
    }

    @JsonProperty("comments")
    public void setComments(Comments comments) {
        this.comments = comments;
    }

    @JsonProperty("review_comments")
    public ReviewComments getReviewComments() {
        return reviewComments;
    }

    @JsonProperty("review_comments")
    public void setReviewComments(ReviewComments reviewComments) {
        this.reviewComments = reviewComments;
    }

    @JsonProperty("review_comment")
    public ReviewComment getReviewComment() {
        return reviewComment;
    }

    @JsonProperty("review_comment")
    public void setReviewComment(ReviewComment reviewComment) {
        this.reviewComment = reviewComment;
    }

    @JsonProperty("commits")
    public Commits getCommits() {
        return commits;
    }

    @JsonProperty("commits")
    public void setCommits(Commits commits) {
        this.commits = commits;
    }

    @JsonProperty("statuses")
    public Statuses getStatuses() {
        return statuses;
    }

    @JsonProperty("statuses")
    public void setStatuses(Statuses statuses) {
        this.statuses = statuses;
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
