package com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class BitbucketServerProject {
    String key;
	String name;
}
