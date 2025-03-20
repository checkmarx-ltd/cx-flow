package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Comment {
    @JsonProperty("id")
    private  Integer commentId;

    @JsonProperty("parentCommentId")
    private Integer parentCommentId;

    @JsonProperty("content")
    private String content;

    @JsonProperty("publishedDate")
    private  String publishedDate;

    @JsonProperty("lastUpdatedDate")
    private String lastUpdatedDate;
    @JsonProperty("lastContentUpdatedDate")
    private String lastContentUpdatedDate;

    @JsonProperty("commentType")
    private String commentType;

    @JsonProperty("_links")
    private CommentLinks links;

    @JsonProperty("id")
    public void setCommentId(Integer id){
        this.commentId=id;
    }

    @JsonProperty("id")
    public Integer getCommentId(){
        return commentId;
    }

    @JsonProperty("parentCommentId")
    public void setParentCommentId(Integer parentCommentId){
        this.parentCommentId=parentCommentId;
    }

    @JsonProperty("parentCommentId")
    public Integer getParentCommentId(){
        return parentCommentId;
    }

    @JsonProperty("content")
    public  void setContent(String content){
        this.content=content;
    }

    @JsonProperty("content")
    public String getContent(){
        return  content;
    }

    @JsonProperty("publishedDate")
    public  void setPublishedDate(String publishedDate){
        this.publishedDate=publishedDate;
    }
    @JsonProperty("publishedDate")
    public String getPublishedDate(){
        return publishedDate;
    }

    @JsonProperty("lastUpdatedDate")
     public  void setLastUpdatedDate(String lastUpdatedDate){
        this.lastUpdatedDate=lastUpdatedDate;
    }

    @JsonProperty("lastUpdatedDate")
    public String getLastUpdatedDate(){
        return lastUpdatedDate;
    }

    @JsonProperty("lastContentUpdatedDate")
    public void setLastContentUpdatedDate(){
        this.lastContentUpdatedDate=lastContentUpdatedDate;
    }

    @JsonProperty("lastContentUpdatedDate")
    public String getLastContentUpdatedDate(){
        return lastContentUpdatedDate;
    }

    @JsonProperty("commentType")
    public void setCommentType(){
        this.commentType=commentType;
    }

    @JsonProperty("commentType")
    public String getCommentType(){
        return  commentType;
    }
    @JsonProperty("_links")
    public void setLinks(CommentLinks links){
        this.links=links;
    }

    @JsonProperty("_links")
    public CommentLinks getLinks(){
        return links;
    }

}
