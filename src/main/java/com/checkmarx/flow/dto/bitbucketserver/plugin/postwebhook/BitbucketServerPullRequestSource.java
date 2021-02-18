package com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class BitbucketServerPullRequestSource {
    String latestCommit;
	String displayId;
	BitbucketServerRepository repository;
    BitbucketServerBranch branch;
	BitbucketServerCommit commit;
}
