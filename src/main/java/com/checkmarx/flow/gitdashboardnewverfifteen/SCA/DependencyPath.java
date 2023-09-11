
package com.checkmarx.flow.gitdashboardnewverfifteen.SCA;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "iid"
})
@Generated("jsonschema2pojo")
@Builder
@Data
public class DependencyPath {

    /**
     * ID that is unique in the scope of a parent object, and specific to the resource type.
     * (Required)
     *
     */
    @JsonProperty("iid")
    @JsonPropertyDescription("ID that is unique in the scope of a parent object, and specific to the resource type.")
    @Builder.Default
    private Double iid=123.0;


    /**
     * ID that is unique in the scope of a parent object, and specific to the resource type.
     * (Required)
     *
     */
    @JsonProperty("iid")
    public Double getIid() {
        return iid;
    }

    /**
     * ID that is unique in the scope of a parent object, and specific to the resource type.
     * (Required)
     *
     */
    @JsonProperty("iid")
    public void setIid(Double iid) {
        this.iid = iid;
    }



}
