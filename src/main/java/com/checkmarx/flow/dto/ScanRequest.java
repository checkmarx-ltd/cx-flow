package com.checkmarx.flow.dto;

import com.checkmarx.sdk.dto.Filter;
import lombok.*;

import java.util.*;

/**
 * Object containing all applicable information about the scan request details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanRequest {
    private String id;
    private String namespace;
    private String application;
    private String org;
    private String team;
    private String project;
	private String altProject;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String altFields;

    private Map<String, String> cxFields;
    private String site;
    private String hash;
    private String repoUrl;
    private String repoUrlWithAuth;
    private String repoName;
    private String branch;
    private String mergeTargetBranch;
    private String mergeNoteUri;
    //project repoProjectId used by GitLab
    private Integer repoProjectId;
    private String refs;
    private List<String> email;
    private boolean forceScan;
    private boolean incremental;
    private String scanPreset;

    @Builder.Default
    private boolean scanPresetOverride = false;
    private List<String> excludeFiles;
    private List<String> excludeFolders;
    private Repository repoType;
    private Product product;
    private BugTracker bugTracker;
    private Type type;
    private List<String> activeBranches;
    private List<Filter> filters;
    private Map<String, String> additionalMetadata;

    public ScanRequest(ScanRequest other) {
        this.namespace = other.namespace;
        this.application = other.application;
        this.org = other.org;
        this.team = other.team;
        this.project = other.project;
        this.cxFields = other.cxFields;
		this.altProject = other.altProject;
        this.altFields = other.altFields;
        this.site = other.site;
        this.hash = other.hash;
        this.repoUrl = other.repoUrl;
        this.repoUrlWithAuth = other.repoUrlWithAuth;
        this.repoName = other.repoName;
        this.branch = other.branch;
        this.mergeTargetBranch = other.mergeTargetBranch;
        this.mergeNoteUri = other.mergeNoteUri;
        this.repoProjectId = other.repoProjectId;
        this.refs = other.refs;
        this.email = other.email;
        this.incremental = other.incremental;
        this.scanPreset = other.scanPreset;
        this.excludeFiles = other.excludeFiles;
        this.excludeFolders = other.excludeFolders;
        this.repoType = other.repoType;
        this.product = other.product;
        this.bugTracker = new BugTracker(other.getBugTracker());
        this.type = other.type;
        this.activeBranches = other.activeBranches;
        this.filters = other.filters;
        this.forceScan = other.forceScan;
    }

    public Map<String,String> getAltFields() {
        if(this.altFields == null){
            return Collections.emptyMap();
        }
        Map<String,String> map = new HashMap<>();
        for( String s :Arrays.asList(this.altFields.split(","))) {
            String[] split = s.split(":");
            map.put(split[0],split[1]);
        }
        return map;
    }

    public void putAdditionalMetadata(String key, String value){
        if(this.additionalMetadata == null){
            this.additionalMetadata = new HashMap<>();
        }
        this.additionalMetadata.put(key, value);
    }

    public String getAdditionalMetadata(String key){
        if (this.additionalMetadata != null) {
            return this.additionalMetadata.get(key);
        }
        return null;
    }

    public String getFilename(){
        return this.getAdditionalMetadata("filename");
    }

    public void setFilename(String filename){
        this.putAdditionalMetadata("filename", filename);
    }

    public String toString() {
        return "ScanRequest(namespace=" + this.getNamespace() + ", application=" + this.getApplication() + ", org=" + this.getOrg() + ", team=" + this.getTeam() + ", project=" + this.getProject() + ", cxFields=" + this.getCxFields() + ", site=" + this.getSite() + ", repoUrl=" + this.getRepoUrl() + ", repoName=" + this.getRepoName() + ", branch=" + this.getBranch() + ", mergeTargetBranch=" + this.getMergeTargetBranch() + ", mergeNoteUri=" + this.getMergeNoteUri() + ", repoProjectId=" + this.getRepoProjectId() + ", refs=" + this.getRefs() + ", email=" + this.getEmail() + ", incremental=" + this.isIncremental() + ", scanPreset=" + this.getScanPreset() + ", excludeFiles=" + this.getExcludeFiles() + ", excludeFolders=" + this.getExcludeFolders() + ", repoType=" + this.getRepoType() + ", product=" + this.getProduct() + ", bugTracker=" + this.getBugTracker() + ", type=" + this.getType() + ", activeBranches=" + this.getActiveBranches() + ", filters=" + this.getFilters() + ")";
    }

    public enum Product {
        CX("CX"),
        CXOSA("CXOSA"),
        FORTIFY("FORTIFY");

        private String product;

        Product(String product) {
            this.product = product;
        }

        public String getProduct() {
            return product;
        }
    }

    public enum Type {
        SAST("SAST"),
        DAST("DAST"),
        IAST("IAST");

        private String type;

        Type(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public enum Repository {
        GITHUB("GITHUB"),
        GITLAB("GITLAB"),
        BITBUCKET("BITBUCKET"),
        BITBUCKETSERVER("BITBUCKETSERVER"),
        ADO("ADO"),
        NA("NA");

        private String repository;

        Repository(String repository) {
            this.repository = repository;
        }

        public String getRepository() {
            return repository;
        }
    }
}
