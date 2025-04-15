package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PRCommentEvent extends  PullEvent{
    @JsonProperty("resource")
    private  ResourceComment resource;

    @JsonProperty("resource")
    public void setResource(ResourceComment resource){
        this.resource=resource;
    }
    @JsonProperty("resource")
    public ResourceComment getResource(){
        return resource;
    }

}
