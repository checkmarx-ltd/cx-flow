package com.checkmarx.flow.dto;

import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.Filter;

import java.beans.ConstructorProperties;
import java.util.*;

/**
 * Object containing all applicable information about the scan request details
 */
public class ScanRequest {
    private String id;
    private String namespace;
    private String application;
    private String org;
    private String team;
    private String project;
	private String altProject;
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
    private boolean incremental;
    private String scanPreset;
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

    @ConstructorProperties({"namespace", "application", "org", "team", "project", "cxFields", "site", "repoUrl",
            "repoUrlWithAuth", "repoName", "branch", "mergeTargetBranch", "mergeNoteUri", "repoProjectId", "refs", "email",
            "incremental", "scanPreset", "excludeFiles", "excludeFolders", "repoType", "product", "bugTracker",
            "type", "activeBranches", "filters","altProject","altFields"})
    ScanRequest(String namespace, String application, String org, String team, String project, Map<String, String> cxFields, String site, String repoUrl, String repoUrlWithAuth, String repoName, String branch, String mergeTargetBranch, String mergeNoteUri, Integer id, String refs, List<String> email, boolean incremental, String scanPreset, List<String> excludeFiles, List<String> excludeFolders, Repository repoType, Product product, BugTracker bugTracker, Type type, List<String> activeBranches, List<Filter> filters, String altProject,String altFields) {
        this.namespace = namespace;
        this.application = application;
        this.org = org;
        this.team = team;
        this.project = project;
		this.altProject = altProject;
        this.altFields = altFields;
        this.cxFields = cxFields;
        this.site = site;
        this.repoUrl = repoUrl;
        this.repoUrlWithAuth = repoUrlWithAuth;
        this.repoName = repoName;
        this.branch = branch;
        this.mergeTargetBranch = mergeTargetBranch;
        this.mergeNoteUri = mergeNoteUri;
        this.repoProjectId = id;
        this.refs = refs;
        this.email = email;
        this.incremental = incremental;
        this.scanPreset = scanPreset;
        this.excludeFiles = excludeFiles;
        this.excludeFolders = excludeFolders;
        this.repoType = repoType;
        this.product = product;
        this.bugTracker = bugTracker;
        this.type = type;
        this.activeBranches = activeBranches;
        this.filters = filters;
		this.altProject = altProject;
    }

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
    }

    public static ScanRequestBuilder builder() {
        return new ScanRequestBuilder();
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplication() {
        return this.application;
    }

    public String getOrg() {
        return this.org;
    }

    public String getTeam() {
        return this.team;
    }

    public String getProject() {
        return this.project;
    }

   public String getAltProject() { return this.altProject; }

    public Map<String,String> getAltFields() {
        if(this.altFields == null){
            return Collections.emptyMap();
        }
        Map<String,String> map = new HashMap<String,String>();
        for( String s :Arrays.asList(this.altFields.split(","))) {
            String[] split = s.split(":");
            map.put(split[0],split[1]);
        }
        return map;
    }

    public Map<String, String> getAdditionalMetadata() {
        return additionalMetadata;
    }

    public void setAdditionalMetadata(Map<String, String> additionalMetadata) {
        this.additionalMetadata = additionalMetadata;
    }

    public Map<String, String> getCxFields() {
        return this.cxFields;
    }

    public String getSite() {
        return this.site;
    }

    public String getHash() {
        return this.hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getRepoUrl() {
        return this.repoUrl;
    }

    public String getRepoUrlWithAuth() {
        return this.repoUrlWithAuth;
    }

    public String getRepoName() {
        return this.repoName;
    }

    public String getBranch() {
        return this.branch;
    }

    public String getMergeTargetBranch() {
        return this.mergeTargetBranch;
    }

    public String getMergeNoteUri() {
        return this.mergeNoteUri;
    }

    public Integer getRepoProjectId() {
        return this.repoProjectId;
    }

    public String getRefs() {
        return this.refs;
    }

    public List<String> getEmail() {
        return this.email;
    }

    public boolean isIncremental() {
        return this.incremental;
    }

    public String getScanPreset() {
        return this.scanPreset;
    }

    public List<String> getExcludeFiles() {
        return this.excludeFiles;
    }

    public List<String> getExcludeFolders() {
        return this.excludeFolders;
    }

    public Repository getRepoType() {
        return this.repoType;
    }

    public Product getProduct() {
        return this.product;
    }

    public BugTracker getBugTracker() {
        return this.bugTracker;
    }

    public Type getType() {
        return this.type;
    }

    public List<String> getActiveBranches() {
        return this.activeBranches;
    }

    public List<com.checkmarx.sdk.dto.Filter> getFilters() {
        return this.filters;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public void setCxFields(Map<String, String> cxFields) {
        this.cxFields = cxFields;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public void setRepoUrlWithAuth(String repoUrlWithAuth) {
        this.repoUrlWithAuth = repoUrlWithAuth;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setMergeTargetBranch(String mergeTargetBranch) {
        this.mergeTargetBranch = mergeTargetBranch;
    }

    public void setMergeNoteUri(String mergeNoteUri) {
        this.mergeNoteUri = mergeNoteUri;
    }

    public void setRepoProjectId(Integer repoProjectId) {
        this.repoProjectId = repoProjectId;
    }

    public void setRefs(String refs) {
        this.refs = refs;
    }

    public void setEmail(List<String> email) {
        this.email = email;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public void setScanPreset(String scanPreset) {
        this.scanPreset = scanPreset;
    }

    public boolean isScanPresetOverride() {
        return scanPresetOverride;
    }

    public void setScanPresetOverride(boolean scanPresetOverride) {
        this.scanPresetOverride = scanPresetOverride;
    }

    public void setExcludeFiles(List<String> excludeFiles) {
        this.excludeFiles = excludeFiles;
    }

    public void setExcludeFolders(List<String> excludeFolders) {
        this.excludeFolders = excludeFolders;
    }

    public void setRepoType(Repository repoType) {
        this.repoType = repoType;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

	    public void setAltProject(String altProduct) {
        this.altProject = altProject;
    }

    public void setAltFields(String altFields) {
         this.altFields = altFields;
    }
    public void setBugTracker(BugTracker bugTracker) {
        this.bugTracker = bugTracker;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setActiveBranches(List<String> activeBranches) {
        this.activeBranches = activeBranches;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
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
        return "ScanRequest(namespace=" + this.getNamespace() + ", application=" + this.getApplication() + ", org=" + this.getOrg() + ", team=" + this.getTeam() + ", project=" + this.getProject() + ", cxFields=" + this.getCxFields() + ", site=" + this.getSite() + ", repoUrl=" + this.getRepoUrl() + ", repoUrlWithAuth=" + this.getRepoUrlWithAuth() + ", repoName=" + this.getRepoName() + ", branch=" + this.getBranch() + ", mergeTargetBranch=" + this.getMergeTargetBranch() + ", mergeNoteUri=" + this.getMergeNoteUri() + ", repoProjectId=" + this.getRepoProjectId() + ", refs=" + this.getRefs() + ", email=" + this.getEmail() + ", incremental=" + this.isIncremental() + ", scanPreset=" + this.getScanPreset() + ", excludeFiles=" + this.getExcludeFiles() + ", excludeFolders=" + this.getExcludeFolders() + ", repoType=" + this.getRepoType() + ", product=" + this.getProduct() + ", bugTracker=" + this.getBugTracker() + ", type=" + this.getType() + ", activeBranches=" + this.getActiveBranches() + ", filters=" + this.getFilters() + ")";
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

    public static class ScanRequestBuilder {
        private String namespace;
        private String application;
        private String org;
        private String team;
        private String project;
        private Map<String, String> cxFields;
        private String site;
        private String hash;
        private String repoUrl;
        private String repoUrlWithAuth;
        private String repoName;
        private String branch;
        private String mergeTargetBranch;
        private String mergeNoteUri;
        private Integer id;
        private String refs;
        private List<String> email;
        private boolean incremental;
        private String scanPreset;
        private List<String> excludeFiles;
        private List<String> excludeFolders;
        private Repository repoType;
        private Product product;
		private String altProject;
        private String altFields;
        private BugTracker bugTracker;
        private Type type;
        private List<String> activeBranches;
        private List<Filter> filters;

        ScanRequestBuilder(){
        }

        public ScanRequest.ScanRequestBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public ScanRequest.ScanRequestBuilder application(String application) {
            this.application = application;
            return this;
        }

        public ScanRequest.ScanRequestBuilder org(String org) {
            this.org = org;
            return this;
        }

        public ScanRequest.ScanRequestBuilder team(String team) {
            this.team = team;
            return this;
        }

        public ScanRequest.ScanRequestBuilder project(String project) {
            this.project = project;
            return this;
        }

        public ScanRequest.ScanRequestBuilder cxFields(Map<String, String> cxFields) {
            this.cxFields = cxFields;
            return this;
        }

        public ScanRequest.ScanRequestBuilder site(String site) {
            this.site = site;
            return this;
        }


        public ScanRequest.ScanRequestBuilder hash(String hash) {
            this.hash = hash;
            return this;
        }

        public ScanRequest.ScanRequestBuilder repoUrl(String repoUrl) {
            this.repoUrl = repoUrl;
            return this;
        }

        public ScanRequest.ScanRequestBuilder repoUrlWithAuth(String repoUrlWithAuth) {
            this.repoUrlWithAuth = repoUrlWithAuth;
            return this;
        }

        public ScanRequest.ScanRequestBuilder repoName(String repoName) {
            this.repoName = repoName;
            return this;
        }

        public ScanRequest.ScanRequestBuilder branch(String branch) {
            this.branch = branch;
            return this;
        }

        public ScanRequest.ScanRequestBuilder mergeTargetBranch(String mergeTargetBranch) {
            this.mergeTargetBranch = mergeTargetBranch;
            return this;
        }

        public ScanRequest.ScanRequestBuilder mergeNoteUri(String mergeNoteUri) {
            this.mergeNoteUri = mergeNoteUri;
            return this;
        }

        public ScanRequest.ScanRequestBuilder id(Integer id) {
            this.id = id;
            return this;
        }

        public ScanRequest.ScanRequestBuilder refs(String refs) {
            this.refs = refs;
            return this;
        }

        public ScanRequest.ScanRequestBuilder email(List<String> email) {
            this.email = email;
            return this;
        }

        public ScanRequest.ScanRequestBuilder incremental(boolean incremental) {
            this.incremental = incremental;
            return this;
        }

        public ScanRequest.ScanRequestBuilder scanPreset(String scanPreset) {
            this.scanPreset = scanPreset;
            return this;
        }

        public ScanRequest.ScanRequestBuilder excludeFiles(List<String> excludeFiles) {
            this.excludeFiles = excludeFiles;
            return this;
        }

        public ScanRequest.ScanRequestBuilder excludeFolders(List<String> excludeFolders) {
            this.excludeFolders = excludeFolders;
            return this;
        }

        public ScanRequest.ScanRequestBuilder repoType(Repository repoType) {
            this.repoType = repoType;
            return this;
        }

        public ScanRequest.ScanRequestBuilder product(Product product) {
            this.product = product;
            return this;
        }

        public ScanRequest.ScanRequestBuilder bugTracker(BugTracker bugTracker) {
            this.bugTracker = bugTracker;
            return this;
        }

		public ScanRequest.ScanRequestBuilder altProject(String altProject) {
            this.altProject = altProject;
            return this;
        }

        public ScanRequest.ScanRequestBuilder altFields(String altFields) {
            this.altFields = altFields;
            return this;
        }


        public ScanRequest.ScanRequestBuilder type(Type type) {
            this.type = type;
            return this;
        }

        public ScanRequest.ScanRequestBuilder activeBranches(List<String> activeBranches) {
            this.activeBranches = activeBranches;
            return this;
        }

        public ScanRequest.ScanRequestBuilder filters(List<Filter> filters) {
            this.filters = filters;
            return this;
        }

        public ScanRequest build() {
            return new ScanRequest(namespace, application, org, team, project, cxFields, site, repoUrl, repoUrlWithAuth, repoName, branch, mergeTargetBranch, mergeNoteUri, id, refs, email, incremental, scanPreset, excludeFiles, excludeFolders, repoType, product, bugTracker, type, activeBranches, filters, altProject, altFields);
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            if(!ScanUtils.empty(application)){
                builder.append("app=").append(application).append("|");
            }
            if(!ScanUtils.empty(repoName)){
                builder.append("repo=").append(repoName).append("|");
            }
            if(!ScanUtils.empty(branch)){
                builder.append("branch=").append(branch).append("|");
            }
            if(!ScanUtils.empty(application)){
                builder.append("app=").append(application);
            }
            builder.append("]");
            return builder.toString();
        }
    }
}
