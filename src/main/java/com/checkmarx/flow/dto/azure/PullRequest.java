package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PullRequest {
    @JsonProperty("repository")
    private Repository repository;

    @JsonProperty("pullRequestId")
    private  Integer pullRequestId;

    @JsonProperty("status")
    private  String status;

    @JsonProperty("createdBy")
    private CreatedBy createdBy;

    @JsonProperty("creationDate")
    private String creationDate;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("sourceRefName")
    private String sourceRefName;


    @JsonProperty("targetRefName")
    private String targetRefName;


    @JsonProperty("mergeStatus")
    private String mergeStatus;

    @JsonProperty("mergeId")
    private String  mergeId;

    @JsonProperty("lastMergeSourceCommit")
    private LastMergeSourceCommit lastMergeSourceCommit;

    @JsonProperty("lastMergeTargetCommit")
    private LastMergeTargetCommit lastMergeTargetCommit;

    @JsonProperty("commits")
    private List<Commit>commit;


    @JsonProperty("url")
    private  String url;

    @JsonProperty("repository")
    public void setRepository(Repository repository){
        this.repository=repository;
    }

    @JsonProperty("repository")
    public  Repository getRepository(){
        return repository;
    }

    @JsonProperty("pullRequestId")
    public void setPullRequestId(Integer pullRequestId){
        this.pullRequestId=pullRequestId;
    }

    @JsonProperty("pullRequestId")
    public  Integer getPullRequestId(){
        return pullRequestId;
    }

    @JsonProperty("status")
    public  void setStatus(String status){
        this.status=status;
    }

    @JsonProperty("status")
    public  String getStatus(){
        return status;
    }

    @JsonProperty("createdBy")
    public  void setCreatedBy(CreatedBy createdBy){
        this.createdBy=createdBy;
    }

    @JsonProperty("createdBy")
    public  CreatedBy getCreatedBy(){
        return  createdBy;
    }

    @JsonProperty("creationDate")
    public  void setCreationDate(String creationDate){
        this.creationDate=creationDate;
    }

    @JsonProperty("creationDate")
    public  String getCreationDate(){
        return creationDate;
    }

    @JsonProperty("title")
    public void setTitle(String title){
        this.title=title;
    }

    @JsonProperty("title")
    public String getTitle(){
        return title;
    }

    @JsonProperty("description")

    public void setDescription(String description){
        this.description=description;
    }

    @JsonProperty("description")
    public  String getDescription(){
        return description;
    }

    @JsonProperty("sourceRefName")
    public  void setSourceRefName(String sourceRefName){
        this.sourceRefName=sourceRefName;
    }

    @JsonProperty("sourceRefName")
    public String getSourceRefName(){
        return sourceRefName;
    }

    @JsonProperty("targetRefName")
    public  void setTargetRefName(String targetRefName){
        this.targetRefName=targetRefName;
    }

    @JsonProperty("targetRefName")
    public  String getTargetRefName(){
        return targetRefName;
    }

    @JsonProperty("mergeStatus")
    public void setMergeStatus(String mergeStatus){
        this.mergeStatus=mergeStatus;
    }

    @JsonProperty("mergeStatus")
    public String getMergeStatus(){
        return mergeStatus;
    }

    @JsonProperty("mergeId")
    public void setMergeId(String mergeId){
        this.mergeId=mergeId;
    }

    @JsonProperty("mergeId")
     public  String getMergeId(){
        return mergeId;
    }

    @JsonProperty("commits")
    public  void setCommit(List<Commit>commit){
        this.commit=commit;
    }

    @JsonProperty("commits")
    public  List<Commit>getCommit(){
        return commit;
    }

    @JsonProperty("url")
    public void setUrl(String url){
        this.url=url;
    }

    @JsonProperty("url")
    public String getUrl(){
        return url;
    }

}
