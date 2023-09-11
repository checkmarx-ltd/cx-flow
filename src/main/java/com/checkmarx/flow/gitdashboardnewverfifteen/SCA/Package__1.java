
package com.checkmarx.flow.gitdashboardnewverfifteen.SCA;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import javax.annotation.Generated;


/**
 * Provides information on the package where the vulnerability is located.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name"
})
@Generated("jsonschema2pojo")
@Builder
@Data
public class Package__1 {

    /**
     * Name of the package where the vulnerability is located.
     * (Required)
     *
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Name of the package where the vulnerability is located.")
    private String name;


    /**
     * Name of the package where the vulnerability is located.
     * (Required)
     *
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Name of the package where the vulnerability is located.
     * (Required)
     *
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }



}
