package com.checkmarx.flow.config;

import com.checkmarx.flow.dto.Field;
import com.checkmarx.flow.dto.LabelField;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

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
    @Getter
    @Setter
    private String appHeader = "application/vnd.github.machine-man-preview+json, application/vnd.github.v3+json";
    @Getter
    @Setter
    private String appUrl = "https://api.github.com/app/";

    @Getter
    @Setter
    private int maxDescriptionLength =50000;

    @Getter
    @Setter
    private int maxDelay;

    @Getter
    @Setter
    private Map<FindingSeverity,String> issueslabel;

    @Getter
    @Setter
    private boolean commentUpdate =true;
    @Getter
    @Setter
    private List<LabelField> fields;

    @Getter
    @Setter
    private boolean enableAddComment = false;

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
