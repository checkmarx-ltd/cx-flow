
package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "oldObjectId",
    "newObjectId"
})
public class RefUpdate {

    @JsonProperty("name")
    private String name;
    @JsonProperty("oldObjectId")
    private String oldObjectId;
    @JsonProperty("newObjectId")
    private String newObjectId;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("oldObjectId")
    public String getOldObjectId() {
        return oldObjectId;
    }

    @JsonProperty("oldObjectId")
    public void setOldObjectId(String oldObjectId) {
        this.oldObjectId = oldObjectId;
    }

    @JsonProperty("newObjectId")
    public String getNewObjectId() {
        return newObjectId;
    }

    @JsonProperty("newObjectId")
    public void setNewObjectId(String newObjectId) {
        this.newObjectId = newObjectId;
    }

}
