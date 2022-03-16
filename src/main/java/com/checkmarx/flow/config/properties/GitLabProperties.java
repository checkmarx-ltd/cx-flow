package com.checkmarx.flow.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "gitlab")
@Validated
public class GitLabProperties extends RepoProperties {

    private static final String MERGE_NOTE = "%s/projects/%s/merge_requests/%s/notes";

    private String sastFilePath = "./gl-sast-report.json";

    private String scaFilePath = "./gl-dependency-scanning-report.json";


    public String getGitUri(String namespace, String repo) {
        String format = "%s/%s/%s.git";
        return String.format(format, getUrl(), namespace, repo);
        //sample: https://github.com/namespace/repo.git
    }

    public String getMergeNoteUri(String projectId, String mergeId) {
        return String.format(MERGE_NOTE, this.getApiUrl(), projectId, mergeId);
    }

    public String getSastFilePath() {
        return sastFilePath;
    }

    public void setSastFilePath(String sastFilePath) {
        this.sastFilePath = sastFilePath;
    }

    public String getScaFilePath() {
        return scaFilePath;
    }

    public void setScaFilePath(String scaFilePath) {
        this.scaFilePath = scaFilePath;
    }
}
