package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceComment {

    @JsonProperty("comment")
    private Comment comment;

    @JsonProperty("pullRequest")
    private PullRequest pullRequest;

    @JsonProperty("comment")
    public void setComment(Comment comment){
        this.comment=comment;
    }
    @JsonProperty("comment")
   public Comment getComment(){
        return  comment;
   }

   @JsonProperty("pullRequest")
    public void setPullRequest(PullRequest pullRequest){
        this.pullRequest=pullRequest;
   }

   @JsonProperty("pullRequest")
    public PullRequest getPullRequest(){
        return pullRequest;
   }



}
