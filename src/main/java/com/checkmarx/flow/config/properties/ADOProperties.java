package com.checkmarx.flow.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "azure")
@Validated
public class ADOProperties extends RepoProperties {

    private boolean deleteCxProject = false;
    private String issueType = "issue";
    private String issueBody = "Description";
    private String appTagPrefix = "app";
    private String ownerTagPrefix = "owner";
    private String repoTagPrefix = "repo";
    private String branchLabelPrefix = "branch";
    private String apiVersion = "5.0";
    private String testRepository = "Fabrikam";
    private String openStatus = "To Do";
    private String closedStatus = "Done";
    private String projectName;
    private String namespace;
    
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

            
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }
    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getIssueBody() {
        return issueBody;
    }

    public void setIssueBody(String issueBody) {
        this.issueBody = issueBody;
    }

    public String getAppTagPrefix() {
        return appTagPrefix;
    }

    public void setAppTagPrefix(String appTagPrefix) {
        this.appTagPrefix = appTagPrefix;
    }

    public String getOwnerTagPrefix() {
        return ownerTagPrefix;
    }

    public void setOwnerTagPrefix(String ownerTagPrefix) {
        this.ownerTagPrefix = ownerTagPrefix;
    }

    public String getRepoTagPrefix() {
        return repoTagPrefix;
    }

    public void setRepoTagPrefix(String repoTagPrefix) {
        this.repoTagPrefix = repoTagPrefix;
    }

    public String getBranchLabelPrefix() {
        return branchLabelPrefix;
    }

    public void setBranchLabelPrefix(String branchLabelPrefix) {
        this.branchLabelPrefix = branchLabelPrefix;
    }

    public String getOpenStatus() {
        return openStatus;
    }

    public void setOpenStatus(String openStatus) {
        this.openStatus = openStatus;
    }

    public String getClosedStatus() {
        return closedStatus;
    }

    public void setClosedStatus(String closedStatus) {
        this.closedStatus = closedStatus;
    }

    public String getTestRepository() {
        return testRepository;
    }

    public void setTestRepository(String testRepository) {
        this.testRepository = testRepository;
    }

    public boolean getDeleteCxProject(){
        return deleteCxProject;
    }

    public void setDeleteCxProject(boolean deleteProject){
        this.deleteCxProject = deleteProject;
    }

    public String getMergeNoteUri(String namespace, String repo, String mergeId){
        String format = "%s/%s/_apis/git/repositories/%s/pullRequests/%s/threads";
        return String.format(format, getUrl(), namespace, repo, mergeId);
        //http://localhost:8080/tfs/DefaultCollection/Checkmarx/_apis/git/repositories/Checkmarx/pullRequests/2/threads
    }
}
