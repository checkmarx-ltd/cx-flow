
package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "reviewerUrl",
    "vote",
    "displayName",
    "url",
    "id",
    "uniqueName",
    "imageUrl",
    "isContainer"
})
public class Reviewer {

    @JsonProperty("reviewerUrl")
    private Object reviewerUrl;
    @JsonProperty("vote")
    private Integer vote;
    @JsonProperty("displayName")
    private String displayName;
    @JsonProperty("url")
    private String url;
    @JsonProperty("id")
    private String id;
    @JsonProperty("uniqueName")
    private String uniqueName;
    @JsonProperty("imageUrl")
    private String imageUrl;
    @JsonProperty("isContainer")
    private Boolean isContainer;

    @JsonProperty("reviewerUrl")
    public Object getReviewerUrl() {
        return reviewerUrl;
    }

    @JsonProperty("reviewerUrl")
    public void setReviewerUrl(Object reviewerUrl) {
        this.reviewerUrl = reviewerUrl;
    }

    @JsonProperty("vote")
    public Integer getVote() {
        return vote;
    }

    @JsonProperty("vote")
    public void setVote(Integer vote) {
        this.vote = vote;
    }

    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    @JsonProperty("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("uniqueName")
    public String getUniqueName() {
        return uniqueName;
    }

    @JsonProperty("uniqueName")
    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    @JsonProperty("imageUrl")
    public String getImageUrl() {
        return imageUrl;
    }

    @JsonProperty("imageUrl")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @JsonProperty("isContainer")
    public Boolean getIsContainer() {
        return isContainer;
    }

    @JsonProperty("isContainer")
    public void setIsContainer(Boolean isContainer) {
        this.isContainer = isContainer;
    }

}
