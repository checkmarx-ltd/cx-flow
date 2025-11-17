
package com.checkmarx.flow.dto.gitlabdashboardv14.SCA;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;


/**
 * Identifies the vulnerability's location.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "file",
    "dependency"
})
@Generated("jsonschema2pojo")
@Data
@Builder
public class LocationSCA {

    /**
     * Path to the manifest or lock file where the dependency is declared (such as yarn.lock).
     * (Required)
     * 
     */
    @JsonProperty("file")
    @JsonPropertyDescription("Path to the manifest or lock file where the dependency is declared (such as yarn.lock).")
    private String file;
    /**
     * Describes the dependency of a project where the vulnerability is located.
     * (Required)
     * 
     */
    @JsonProperty("dependency")
    @JsonPropertyDescription("Describes the dependency of a project where the vulnerability is located.")
    private Dependency dependency;

    /**
     * Path to the manifest or lock file where the dependency is declared (such as yarn.lock).
     * (Required)
     * 
     */
    @JsonProperty("file")
    public String getFile() {
        return file;
    }

    /**
     * Path to the manifest or lock file where the dependency is declared (such as yarn.lock).
     * (Required)
     * 
     */
    @JsonProperty("file")
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Describes the dependency of a project where the vulnerability is located.
     * (Required)
     * 
     */
    @JsonProperty("dependency")
    public Dependency getDependency() {
        return dependency;
    }

    /**
     * Describes the dependency of a project where the vulnerability is located.
     * (Required)
     * 
     */
    @JsonProperty("dependency")
    public void setDependency(Dependency dependency) {
        this.dependency = dependency;
    }


}
