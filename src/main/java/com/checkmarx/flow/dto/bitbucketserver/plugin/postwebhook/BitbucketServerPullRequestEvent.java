package com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class BitbucketServerPullRequestEvent {
    private BitbucketServerRepositoryOwner actor;
    private BitbucketServerPullRequest pullrequest;
    private BitbucketServerRepository repository;
    private String comment;
}
