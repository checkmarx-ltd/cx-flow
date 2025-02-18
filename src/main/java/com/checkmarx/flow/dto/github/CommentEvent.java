package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentEvent extends EventCommon{
    @JsonProperty("action")
    String action;

    @JsonProperty("issue")
    com.checkmarx.flow.dto.github.issue.Issue issue;

    @JsonProperty("comment")
    Comment comment;

    @JsonProperty("repository")
    Repository repository;

    @JsonProperty("sender")
    Sender sender;


    public void setAction(String action) {
        this.action = action;
    }
    public String getAction() {
        return action;
    }

    public void setIssue(com.checkmarx.flow.dto.github.issue.Issue issue) {
        this.issue = issue;
    }
    public com.checkmarx.flow.dto.github.issue.Issue getIssue() {
        return issue;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }
    public Comment getComment() {
        return comment;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    public Repository getRepository() {
        return repository;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }
    public Sender getSender() {
        return sender;
    }
}
