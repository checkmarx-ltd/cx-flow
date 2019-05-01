
package com.checkmarx.flow.dto.bitbucketserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "eventKey",
    "date",
    "actor",
    "pullRequest"
})
public class PullEvent {

    @JsonProperty("eventKey")
    private String eventKey;
    @JsonProperty("date")
    private String date;
    @JsonProperty("actor")
    private Actor actor;
    @JsonProperty("pullRequest")
    private PullRequest pullRequest;

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

    @JsonProperty("pullRequest")
    public PullRequest getPullRequest() {
        return pullRequest;
    }

    @JsonProperty("pullRequest")
    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

}
