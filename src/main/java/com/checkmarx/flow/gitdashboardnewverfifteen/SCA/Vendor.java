
package com.checkmarx.flow.gitdashboardnewverfifteen.SCA;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import javax.annotation.Generated;


/**
 * The vendor/maintainer of the analyzer.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name"
})
@Generated("jsonschema2pojo")
@Builder
@Data
public class Vendor {

    /**
     * The name of the vendor.
     * (Required)
     *
     */
    @JsonProperty("name")
    @JsonPropertyDescription("The name of the vendor.")
    @Builder.Default
    private String name="SCA-Checkmarx";


    /**
     * The name of the vendor.
     * (Required)
     *
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * The name of the vendor.
     * (Required)
     *
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }



}
