package com.checkmarx.flow.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiFlowControllerComponentTestProperties {

    @Value("${test.params.branch:master}")
    private String branch;

    @Value("${test.params.application:App1}")
    private String application;

    @Value("${test.params.project:CodeInjection1}")
    private String project;

    @Value("${test.params.resultUrl:http://localhost/result}")
    private String resultUrl;

    @Value("${test.params.namespace:compTest}")
    private String namespace;

    @Value("${test.params.repoName:repo}")
    private String repoName;

    @Value("${test.params.team:CxServer}")
    private String team;

    @Value("${test.params.gitUrl:http://localhost/repo.git}")
    private String gitUrl;

    @Value("${test.params.product:CX}")
    private String product;

    @Value("${test.params.preset:Checkmarx Default}")
    private String preset;

    public String getBranch() {
        return branch;
    }

    public String getApplication() {
        return application;
    }

    public String getProject() {
        return project;
    }

    public String getResultUrl() {
        return resultUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getTeam() {
        return team;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public String getProduct() {
        return product;
    }

    public String getPreset() {
        return preset;
    }
}
