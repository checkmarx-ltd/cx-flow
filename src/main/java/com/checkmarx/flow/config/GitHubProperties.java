package com.checkmarx.flow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "github")
@Validated
public class GitHubProperties extends RepoProperties {
    @Getter
    @Setter
    private boolean useConfigAsCodeFromDefaultBranch;
    @Getter
    @Setter
    private String appId;
    @Getter
    @Setter
    private String appKeyFile;

    public String getMergeNoteUri(String namespace, String repo, String mergeId){
        String format = "%s/%s/%s/issues/%s/comments";
        return String.format(format, getApiUrl(), namespace, repo, mergeId);
        //sample: https://api.github.com/repos/octocat/Hello-World/issues/1347/comments
    }

    public String getGitUri(String namespace, String repo){
        String format = "%s/%s/%s.git";
        return String.format(format, getUrl(), namespace, repo);
        //sample: https://github.com/namespace/repo.git
    }
}
