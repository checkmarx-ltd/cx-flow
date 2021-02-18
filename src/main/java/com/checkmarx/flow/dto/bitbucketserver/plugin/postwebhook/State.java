package com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class State {

    private String type;
    private String name;
    private Target target;
}
