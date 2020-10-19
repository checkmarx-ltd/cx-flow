
package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.*;


@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PingEvent extends EventCommon {

    @JsonProperty("zen")
    private String zen;
    @JsonProperty("hook_id")
    private Integer hookId;
    @JsonProperty("hook")
    private Hook hook;

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

}
