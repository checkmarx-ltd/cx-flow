
package com.checkmarx.flow.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "pullrequest",
    "actor",
    "repository"
})
public class MergeEvent {

    @JsonProperty("pullrequest")
    private Pullrequest pullrequest;
    @JsonProperty("actor")
    private Actor actor;
    @JsonProperty("repository")
    private Repository repository;

    @JsonProperty("pullrequest")
    public Pullrequest getPullrequest() {
        return pullrequest;
    }

    @JsonProperty("pullrequest")
    public void setPullrequest(Pullrequest pullrequest) {
        this.pullrequest = pullrequest;
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

}
