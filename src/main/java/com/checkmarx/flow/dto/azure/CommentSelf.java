package com.checkmarx.flow.dto.azure;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class CommentSelf {

    @JsonProperty("href")
    private String href;

    @JsonProperty("href")
    public void setHref(String href){
        this.href=href;
    }

    @JsonProperty("href")
    public String getHref(){
        return href;
    }
}
