
package com.checkmarx.flow.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "push",
    "repository",
    "actor"
})
public class PushEvent {

    @JsonProperty("push")
    private Push push;
    @JsonProperty("repository")
    private Repository repository;
    @JsonProperty("actor")
    private Actor actor;

    @JsonProperty("push")
    public Push getPush() {
        return push;
    }

    @JsonProperty("push")
    public void setPush(Push push) {
        this.push = push;
    }

    @JsonProperty("repository")
    public Repository getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @JsonProperty("actor")
    public Actor getActor() {
        return actor;
    }

    @JsonProperty("actor")
    public void setActor(Actor actor) {
        this.actor = actor;
    }

}
