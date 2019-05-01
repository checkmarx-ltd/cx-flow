
package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;
@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "action",
    "number",
    "pull_request",
    "repository",
    "sender",
    "installation"
})
public class PullEvent {

    @JsonProperty("action")
    private String action;
    @JsonProperty("number")
    private Integer number;
    @JsonProperty("pull_request")
    private PullRequest pullRequest;
    @JsonProperty("repository")
    private Repository repository;
    @JsonProperty("sender")
    private Sender sender;
    @JsonProperty("installation")
    private Installation installation;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("action")
    public String getAction() {
        return action;
    }

    @JsonProperty("action")
    public void setAction(String action) {
        this.action = action;
    }

    @JsonProperty("number")
    public Integer getNumber() {
        return number;
    }

    @JsonProperty("number")
    public void setNumber(Integer number) {
        this.number = number;
    }

    @JsonProperty("pull_request")
    public PullRequest getPullRequest() {
        return pullRequest;
    }

    @JsonProperty("pull_request")
    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    @JsonProperty("repository")
    public Repository getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @JsonProperty("sender")
    public Sender getSender() {
        return sender;
    }

    @JsonProperty("sender")
    public void setSender(Sender sender) {
        this.sender = sender;
    }

    @JsonProperty("installation")
    public Installation getInstallation() {
        return installation;
    }

    @JsonProperty("installation")
    public void setInstallation(Installation installation) {
        this.installation = installation;
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
