package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "gitlab")
@Validated
public class GitLabProperties extends RepoProperties {

    public String getGitUri(String namespace, String repo){
        String format = "%s/%s/%s.git";
        return String.format(format, getUrl(), namespace, repo);
        //sample: https://github.com/namespace/repo.git
    }

}
