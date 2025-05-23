package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class Thread {

    @JsonProperty("href")
    private  String href;

    @JsonProperty("href")
    public void setHref(String href){
        this.href=href;
    }

    @JsonProperty("href")
    public String getHref(){
        return href;
    }

}
