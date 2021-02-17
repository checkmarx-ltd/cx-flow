package com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class BitbucketServerRepository {
    // String scmId;
	BitbucketServerProject project;
	String slug;
	Map<String,List<Link>> links;
    // boolean public;
	// String fullName; // repository full name
    // BitbucketServerRepositoryOwner owner;
	// String ownerName; // project name
}
