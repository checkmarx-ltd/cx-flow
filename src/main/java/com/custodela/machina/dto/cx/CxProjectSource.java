package com.custodela.machina.dto.cx;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "url",
        "branch"
})
public class CxProjectSource {
    @JsonProperty("url")
    public String url;
    @JsonProperty("branch")
    public String branch;

    @java.beans.ConstructorProperties({"url", "branch"})
    CxProjectSource(String url, String branch) {
        this.url = url;
        this.branch = branch;
    }

    public static CxProjectSourceBuilder builder() {
        return new CxProjectSourceBuilder();
    }

    public String getUrl() {
        return this.url;
    }

    public String getBranch() {
        return this.branch;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String toString() {
        return "CxProjectSource(url=" + this.getUrl() + ", branch=" + this.getBranch() + ")";
    }

    public static class CxProjectSourceBuilder {
        private String url;
        private String branch;

        CxProjectSourceBuilder() {
        }

        public CxProjectSource.CxProjectSourceBuilder url(String url) {
            this.url = url;
            return this;
        }

        public CxProjectSource.CxProjectSourceBuilder branch(String branch) {
            this.branch = branch;
            return this;
        }

        public CxProjectSource build() {
            return new CxProjectSource(url, branch);
        }

        public String toString() {
            return "CxProjectSource.CxProjectSourceBuilder(url=" + this.url + ", branch=" + this.branch + ")";
        }
    }
}