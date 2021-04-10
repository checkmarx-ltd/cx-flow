package com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class BitbucketPushChange {

    private boolean created;
    private boolean closed;
    @JsonProperty("new")
    private State newState;
    @JsonProperty("old")
    private State oldState;
}
