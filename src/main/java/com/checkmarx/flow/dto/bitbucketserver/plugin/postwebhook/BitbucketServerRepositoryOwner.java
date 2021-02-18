package com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "username",
    "displayName",
    "emailAddress",
})
@Getter
public class BitbucketServerRepositoryOwner {
    private String username;
	private String displayName;
    private String emailAddress;
}
