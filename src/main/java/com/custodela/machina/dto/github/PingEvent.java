
package com.custodela.machina.dto.github;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "zen",
    "hook_id",
    "hook",
    "repository",
    "sender"
})
public class PingEvent {

    @JsonProperty("zen")
    private String zen;
    @JsonProperty("hook_id")
    private Integer hookId;
    @JsonProperty("hook")
    private Hook hook;
    @JsonProperty("repository")
    private Repository repository;
    @JsonProperty("sender")
    private Sender sender;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("zen")
    public String getZen() {
        return zen;
    }

    @JsonProperty("zen")
    public void setZen(String zen) {
        this.zen = zen;
    }

    @JsonProperty("hook_id")
    public Integer getHookId() {
        return hookId;
    }

    @JsonProperty("hook_id")
    public void setHookId(Integer hookId) {
        this.hookId = hookId;
    }

    @JsonProperty("hook")
    public Hook getHook() {
        return hook;
    }

    @JsonProperty("hook")
    public void setHook(Hook hook) {
        this.hook = hook;
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

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
