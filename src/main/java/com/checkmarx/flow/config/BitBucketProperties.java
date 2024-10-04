package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "bitbucket")
@Validated
public class BitBucketProperties extends RepoProperties {
    private String apiPath;
    private List<String> ipAddresses;

    public String getApiPath() {
        return this.apiPath;
    }

    public List<String> getIpAddresses() {
        return this.ipAddresses;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public String getGitUri(String namespace, String repoName) {
        String format = "%s/%s/%s.git";
        return String.format(format, getUrl(), namespace, repoName);
    }

    public String getCloudMergeNoteUri(String namespace, String repo, String mergeId){
        String format = "%s/%s/repositories/%s/%s/pullRequests/%s/comments";
        return String.format(format, getApiUrl(),getApiPath(), namespace, repo, mergeId);
        //https://api.bitbucket.org/2.0/repositories/{namespace}/{repo}/pullRequests/{merge-id}/comments
    }

    public String getServerMergeNoteUri(String namespace, String repo, String mergeId){
        String format = "%s/%s/projects/%s/repos/%s/pull-requests/%s/comments";
        return String.format(format, getApiUrl(),getApiPath() ,namespace, repo, mergeId);
        //http://localhost:8080/projects/{namespace}/repos/{repo}/pull-requests/{merge-id}/comments
    }
}
