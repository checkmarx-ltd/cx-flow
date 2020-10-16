
package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PullEvent extends EventCommon {

    @JsonProperty("action")
    private String action;
    @JsonProperty("number")
    private Integer number;
    @JsonProperty("pull_request")
    private PullRequest pullRequest;

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

}
