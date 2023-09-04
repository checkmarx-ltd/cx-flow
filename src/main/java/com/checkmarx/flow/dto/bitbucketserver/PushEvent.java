
package com.checkmarx.flow.dto.bitbucketserver;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "eventKey",
        "date",
        "actor",
        "repository",
        "changes",
        "commits",
        "toCommit"
})
public class PushEvent {

    @JsonProperty("eventKey")
    private String eventKey;
    @JsonProperty("date")
    private String date;
    @JsonProperty("actor")
    private Actor actor;
    @JsonProperty("repository")
    private Repository repository;
    @JsonProperty("changes")
    private List<Change> changes = null;

    @JsonIgnore
    @JsonProperty("commits")
    private List<Commit> commits;
    @JsonIgnore
    @JsonProperty("toCommit")
    private ToCommit toCommit;


    @JsonProperty("eventKey")
    public String getEventKey() {
        return eventKey;
    }

    @JsonProperty("eventKey")
    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    @JsonProperty("date")
    public String getDate() {
        return date;
    }

    @JsonProperty("date")
    public void setDate(String date) {
        this.date = date;
    }

    @JsonProperty("actor")
    public Actor getActor() {
        return actor;
    }

    @JsonProperty("actor")
    public void setActor(Actor actor) {
        this.actor = actor;
    }

    @JsonProperty("repository")
    public Repository getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @JsonProperty("changes")
    public List<Change> getChanges() {
        return changes;
    }

    @JsonProperty("changes")
    public void setChanges(List<Change> changes) {
        this.changes = changes;
    }

    @JsonProperty("commits")
    public List<Commit> getCommits() {
        return commits;
    }

    @JsonProperty("commits")
    public void setCommits(List<Commit> commits) {
        this.commits = commits;
    }

    @JsonProperty("toCommit")
    public ToCommit getToCommit() {
        return toCommit;
    }

    @JsonProperty("toCommit")
    public void setToCommit(ToCommit toCommit) {
        this.toCommit = toCommit;
    }

}
