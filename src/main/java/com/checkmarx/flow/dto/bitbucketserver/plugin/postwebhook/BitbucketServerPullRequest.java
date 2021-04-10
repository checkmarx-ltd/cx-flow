package com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class BitbucketServerPullRequest {

    String id;
	String title;
	String link;
	String authorLogin;
	BitbucketServerPullRequestSource fromRef;
	BitbucketServerPullRequestSource toRef;
}
