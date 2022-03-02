package com.checkmarx.flow.dto;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.external.ASTConfig;
import com.checkmarx.flow.service.VulnerabilityScanner;
import com.checkmarx.sdk.config.ScaConfig;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
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
    private Map<String, String> scanFields;
    private String site;

    /**
     * git commit ID, also known as 'SHA' or 'commit hash'.
     * <br>- For push event: ID of the last commit in the push event.
     * <br>- For pull request event: ID of the last commit in the pull request source branch.
     * <br>Currently supported for Bitbucket Cloud/Server, GitHub and GitLab.
     */
    private String hash;

    private String repoUrl;
    private String repoUrlWithAuth;
    private String repoName;
    private String branch;
    private String defaultBranch;
    private String mergeTargetBranch;
    private String mergeNoteUri;
    //project repoProjectId used by GitLab
    private Integer repoProjectId;
    private String refs;
    private List<String> email;
    private boolean forceScan;
    @Getter @Setter
    private String scanResubmit;
    private Boolean incremental;
    private String scanPreset;

    /**
     * Getting populated from the ControllerRequest.
     * Indicates whether we got a scm-instance parameter from a webhook event.
     * In case not null it will override the configuration default scm credentials
     */
    private String scmInstance;

    /**
     * Indicates whether scan preset has been overridden.
     * Overrides may come from a webhook parameter or config-as-code.
     */
    @Builder.Default
    private boolean scanPresetOverride = false;

    /**
    Also known as scan engine configuration.
     */
    private String scanConfiguration;

    private List<String> excludeFiles;
    private List<String> excludeFolders;
    private Repository repoType;
    private Product product;
    private BugTracker bugTracker;
    private Type type;
    private List<String> activeBranches;
    private FilterConfiguration filter;
    private Map<FindingSeverity, Integer> thresholds;
    private Map<String, String> additionalMetadata;
    private List<VulnerabilityScanner> vulnerabilityScanners;
    private ScaConfig scaConfig;
    private ASTConfig astConfig;

    @Getter @Setter
    private String scannerApiSec;

    /**
     * 'Organization' here means the top-most level of project hierarchy.
     * E.g. if SCM supports several levels of hierarchy, path to the project may look like org1/suborg/my-project.
     * In such case the value of organizationId should be 'org1'.
     */
    @Getter @Setter
    private String organizationId;

    @Getter @Setter
    private String gitUrl;

    @Getter @Setter
    private boolean disableCertificateValidation;
    
    //SSH Key per repo
    @Getter @Setter
    private String sshKeyIdentifier;

    //SSH Key per repo
    @Getter @Setter
    private String cliMode;

    public ScanRequest(ScanRequest other) {
        this.namespace = other.namespace;
        this.application = other.application;
        this.org = other.org;
        this.team = other.team;
        this.project = other.project;
        this.cxFields = other.cxFields;
        this.scanFields = other.scanFields;
		this.altProject = other.altProject;
        this.altFields = other.altFields;
        this.site = other.site;
        this.hash = other.hash;
        this.repoUrl = other.repoUrl;
        this.repoUrlWithAuth = other.repoUrlWithAuth;
        this.repoName = other.repoName;
        this.branch = other.branch;
        this.defaultBranch = other.defaultBranch;
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
        this.filter = other.filter;
        this.forceScan = other.forceScan;
        this.scanResubmit = other.scanResubmit;
        this.vulnerabilityScanners = other.vulnerabilityScanners;
        this.scaConfig = other.scaConfig;
        this.astConfig = other.astConfig;
        this.thresholds = other.thresholds;
        this.scannerApiSec = other.scannerApiSec;
        this.organizationId = other.organizationId;
        this.gitUrl = other.gitUrl;
        this.disableCertificateValidation = other.disableCertificateValidation;
        this.sshKeyIdentifier = other.sshKeyIdentifier;
        this.cliMode = other.cliMode;
    }

    public Map<String,String> getAltFields() {
        if(this.altFields == null){
            return Collections.emptyMap();
        }
        Map<String,String> map = new HashMap<>();
        for( String s : this.altFields.split(",")) {
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
        return "ScanRequest(namespace=" + this.getNamespace() + ", application=" + this.getApplication() + ", org=" + this.getOrg() + ", team=" + this.getTeam() + ", project=" + this.getProject() + ", cxFields=" + this.getCxFields() + ", site=" + this.getSite() + ", repoUrl=" + this.getRepoUrl() + ", repoName=" + this.getRepoName() + ", branch=" + this.getBranch() + ", mergeTargetBranch=" + this.getMergeTargetBranch() + ", mergeNoteUri=" + this.getMergeNoteUri() + ", repoProjectId=" + this.getRepoProjectId() + ", refs=" + this.getRefs() + ", email=" + this.getEmail() + ", incremental=" + this.isIncremental() + ", scanPreset=" + this.getScanPreset() + ", excludeFiles=" + this.getExcludeFiles() + ", excludeFolders=" + this.getExcludeFolders() + ", repoType=" + this.getRepoType() + ", product=" + this.getProduct() + ", bugTracker=" + this.getBugTracker() + ", type=" + this.getType() + ", activeBranches=" + this.getActiveBranches() + ", filter=" + this.getFilter()+ ", scanResubmit=" + this.getScanResubmit() + ")";
    }

    public Boolean isIncremental() {
        return Optional.ofNullable(incremental).orElse(Boolean.FALSE);
    }
    public Boolean getIncrementalField() {
        return incremental;
    }
    
    public enum Product {
        CX("CX"),
        CXOSA("CXOSA");

        private final String value;

        Product(String value) {
            this.value = value;
        }

        public String getProduct() {
            return value;
        }
    }

    public enum Type {
        SAST("SAST"),
        DAST("DAST"),
        IAST("IAST");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String getType() {
            return value;
        }
    }

    public enum Repository {
        GITHUB("GITHUB"),
        GITLAB("GITLAB"),
        BITBUCKET("BITBUCKET"),
        BITBUCKETSERVER("BITBUCKETSERVER"),
        ADO("ADO"),
        NA("NA");

        private final String value;

        Repository(String value) {
            this.value = value;
        }

        public String getRepository() {
            return value;
        }
    }
}
