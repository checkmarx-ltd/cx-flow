package com.custodela.machina.dto.cx;

import java.beans.ConstructorProperties;

public class CxScanRequest {
    private String namespace;
    private String repoUrl;
    private boolean incremental;
    private String excludeFiles;
    private String excludeFolders;

    @ConstructorProperties({"namespace", "repoUrl", "incremental", "excludeFiles", "excludeFolders"})
    public CxScanRequest(String namespace, String repoUrl, boolean incremental, String excludeFiles, String excludeFolders) {
        this.namespace = namespace;
        this.repoUrl = repoUrl;
        this.incremental = incremental;
        this.excludeFiles = excludeFiles;
        this.excludeFolders = excludeFolders;
    }

    public CxScanRequest() {
    }

    public static CxScanRequestBuilder builder() {
        return new CxScanRequestBuilder();
    }

    public String toString() {
        return "CxScanRequest(namespace=" + this.getNamespace() + ", repoUrl=" + this.getRepoUrl() + ", incremental=" + this.isIncremental() + ", excludeFiles=" + this.getExcludeFiles() + ", excludeFolders=" + this.getExcludeFolders() + ")";
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getRepoUrl() {
        return this.repoUrl;
    }

    public boolean isIncremental() {
        return this.incremental;
    }

    public String getExcludeFiles() {
        return this.excludeFiles;
    }

    public String getExcludeFolders() {
        return this.excludeFolders;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public void setExcludeFiles(String excludeFiles) {
        this.excludeFiles = excludeFiles;
    }

    public void setExcludeFolders(String excludeFolders) {
        this.excludeFolders = excludeFolders;
    }

    public static class CxScanRequestBuilder {
        private String namespace;
        private String repoUrl;
        private boolean incremental;
        private String excludeFiles;
        private String excludeFolders;

        CxScanRequestBuilder() {
        }

        public CxScanRequest.CxScanRequestBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public CxScanRequest.CxScanRequestBuilder repoUrl(String repoUrl) {
            this.repoUrl = repoUrl;
            return this;
        }

        public CxScanRequest.CxScanRequestBuilder incremental(boolean incremental) {
            this.incremental = incremental;
            return this;
        }

        public CxScanRequest.CxScanRequestBuilder excludeFiles(String excludeFiles) {
            this.excludeFiles = excludeFiles;
            return this;
        }

        public CxScanRequest.CxScanRequestBuilder excludeFolders(String excludeFolders) {
            this.excludeFolders = excludeFolders;
            return this;
        }

        public CxScanRequest build() {
            return new CxScanRequest(namespace, repoUrl, incremental, excludeFiles, excludeFolders);
        }

        public String toString() {
            return "CxScanRequest.CxScanRequestBuilder(namespace=" + this.namespace + ", repoUrl=" + this.repoUrl + ", incremental=" + this.incremental + ", excludeFiles=" + this.excludeFiles + ", excludeFolders=" + this.excludeFolders + ")";
        }
    }
}
