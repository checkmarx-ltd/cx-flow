
package com.custodela.machina.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "commits",
    "self",
    "comments",
    "html"
})
public class Links {

    @JsonProperty("commits")
    private Commits commits;
    @JsonProperty("self")
    private Self self;
    @JsonProperty("html")
    private Html html;
    @JsonProperty("comments")
    private Comments comments;


    @JsonProperty("commits")
    public Commits getCommits() {
        return commits;
    }

    @JsonProperty("commits")
    public void setCommits(Commits commits) {
        this.commits = commits;
    }

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

    @JsonProperty("comments")
    public void setComments(Comments comments) {
        this.comments = comments;
    }

    @JsonProperty("comments")
    public Comments getComments() {
        return this.comments;
    }
}
