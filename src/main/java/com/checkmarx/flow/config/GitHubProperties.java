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
    private GitHubApp app=new GitHubApp();

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

    public GitHubApp getApp() {
        return app;
    }

    public void setApp(GitHubApp app) {
        this.app = app;
    }

    public static class GitHubApp{
        private String org="";
        private int id=0;
        private String secretKey="";

        public String getOrg() {
            return org;
        }

        public void setOrg(String org) {
            this.org = org;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }
}
