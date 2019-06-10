
package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "web",
    "statuses",
    "self",
    "repository",
    "commits",
    "pusher",
    "refs"
})
public class Links_ {

    @JsonProperty("web")
    private Web web;
    @JsonProperty("statuses")
    private Statuses statuses;
    @JsonProperty("self")
    private Self self;
    @JsonProperty("repository")
    private Repository_ repository;
    @JsonProperty("commits")
    private Commits commits;
    @JsonProperty("pusher")
    private Pusher pusher;
    @JsonProperty("refs")
    private Refs refs;
    @JsonProperty("web")
    public Web getWeb() {
        return web;
    }

    @JsonProperty("web")
    public void setWeb(Web web) {
        this.web = web;
    }

    @JsonProperty("statuses")
    public Statuses getStatuses() {
        return statuses;
    }

    @JsonProperty("statuses")
    public void setStatuses(Statuses statuses) {
        this.statuses = statuses;
    }
    @JsonProperty("self")
    public Self getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(Self self) {
        this.self = self;
    }

    @JsonProperty("repository")
    public Repository_ getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(Repository_ repository) {
        this.repository = repository;
    }

    @JsonProperty("commits")
    public Commits getCommits() {
        return commits;
    }

    @JsonProperty("commits")
    public void setCommits(Commits commits) {
        this.commits = commits;
    }

    @JsonProperty("pusher")
    public Pusher getPusher() {
        return pusher;
    }

    @JsonProperty("pusher")
    public void setPusher(Pusher pusher) {
        this.pusher = pusher;
    }

    @JsonProperty("refs")
    public Refs getRefs() {
        return refs;
    }

    @JsonProperty("refs")
    public void setRefs(Refs refs) {
        this.refs = refs;
    }
}
