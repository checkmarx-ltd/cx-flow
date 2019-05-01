
package com.checkmarx.flow.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "url",
    "description",
    "homepage",
    "git_http_url",
    "git_ssh_url"
})
public class Repository {

    @JsonProperty("name")
    private String name;
    @JsonProperty("url")
    private String url;
    @JsonProperty("description")
    private String description;
    @JsonProperty("homepage")
    private String homepage;
    @JsonProperty("git_http_url")
    private String gitHttpUrl;
    @JsonProperty("git_ssh_url")
    private String gitSshUrl;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public Repository withName(String name) {
        this.name = name;
        return this;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    public Repository withUrl(String url) {
        this.url = url;
        return this;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    public Repository withDescription(String description) {
        this.description = description;
        return this;
    }

    @JsonProperty("homepage")
    public String getHomepage() {
        return homepage;
    }

    @JsonProperty("homepage")
    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public Repository withHomepage(String homepage) {
        this.homepage = homepage;
        return this;
    }

    @JsonProperty("git_http_url")
    public String getHttpUrl() {
        return gitHttpUrl;
    }

    @JsonProperty("git_http_url")
    public void setGitHttpUrl(String gitHttpUrl) {
        this.gitHttpUrl = gitHttpUrl;
    }

    public Repository withGitHttpUrl(String gitHttpUrl) {
        this.gitHttpUrl = gitHttpUrl;
        return this;
    }

    @JsonProperty("git_ssh_url")
    public String getGitSshUrl() {
        return gitSshUrl;
    }

    @JsonProperty("git_ssh_url")
    public void setGitSshUrl(String gitSshUrl) {
        this.gitSshUrl = gitSshUrl;
    }

    public Repository withGitSshUrl(String gitSshUrl) {
        this.gitSshUrl = gitSshUrl;
        return this;
    }
}
