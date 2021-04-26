package com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class BitbucketServerRepository {
    String scmId;
	BitbucketServerProject project;
	String slug;
	Map<String,List<Link>> links;
	@JsonProperty("public")
    boolean isPublic;
	String fullName;
    BitbucketServerRepositoryOwner owner;
	String ownerName;
}
