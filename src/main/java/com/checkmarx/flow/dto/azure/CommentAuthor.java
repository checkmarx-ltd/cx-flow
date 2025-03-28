package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentAuthor {

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    @JsonProperty("displayName")
    public  void setDisplayName(String displayName){
        this.displayName=displayName;
    }
}
