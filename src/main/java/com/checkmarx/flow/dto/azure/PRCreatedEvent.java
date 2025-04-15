package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**This PRCreated Event is used  for the create and Update PR event as they consist of same JSON structure
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PRCreatedEvent extends PullEvent{
    @JsonProperty("resource")
    private  Resource resource;

    @JsonProperty("resource")
    public void setResource(Resource resource){
        this.resource=resource;
    }
    @JsonProperty("resource")
    public Resource getResource(){
        return resource;
    }
}
