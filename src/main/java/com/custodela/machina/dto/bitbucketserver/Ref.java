
package com.custodela.machina.dto.bitbucketserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "displayId",
    "type"
})
public class Ref {

    @JsonProperty("id")
    private String id;
    @JsonProperty("displayId")
    private String displayId;
    @JsonProperty("type")
    private String type;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("displayId")
    public String getDisplayId() {
        return displayId;
    }

    @JsonProperty("displayId")
    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

}
