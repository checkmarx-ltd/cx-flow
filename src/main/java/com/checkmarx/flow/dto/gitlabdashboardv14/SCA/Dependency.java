
package com.checkmarx.flow.dto.gitlabdashboardv14.SCA;

import com.fasterxml.jackson.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Generated;
import java.util.List;


/**
 * Describes the dependency of a project where the vulnerability is located.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "package",
    "version",
    "iid",
    "direct",
    "dependency_path"
})
@Generated("jsonschema2pojo")
@Data
@Builder
public class Dependency {

    /**
     * Provides information on the package where the vulnerability is located.
     * 
     */
    @JsonProperty("package")
    @JsonPropertyDescription("Provides information on the package where the vulnerability is located.")
    private Package _package;
    /**
     * Version of the vulnerable package.
     * 
     */
    @JsonProperty("version")
    @JsonPropertyDescription("Version of the vulnerable package.")
    private String version;
    /**
     * ID that identifies the dependency in the scope of a dependency file.
     * 
     */
    @Getter
    @Setter
    @JsonProperty("iid")
    @JsonPropertyDescription("ID that identifies the dependency in the scope of a dependency file.")
    @Builder.Default
    private int iid=123;
    /**
     * Tells whether this is a direct, top-level dependency of the scanned project.
     * 
     */
    @JsonProperty("direct")
    @JsonPropertyDescription("Tells whether this is a direct, top-level dependency of the scanned project.")
    private Boolean direct;
    /**
     * Ancestors of the dependency, starting from a direct project dependency, and ending with an immediate parent of the dependency. The dependency itself is excluded from the path. Direct dependencies have no path.
     * 
     */
    @JsonProperty("dependency_path")
    @JsonPropertyDescription("Ancestors of the dependency, starting from a direct project dependency, and ending with an immediate parent of the dependency. The dependency itself is excluded from the path. Direct dependencies have no path.")
    @Builder.Default
    private List<DependencyPath> dependencyPath = null;

    /**
     * Provides information on the package where the vulnerability is located.
     * 
     */
    @JsonProperty("package")
    public Package getPackage() {
        return _package;
    }

    /**
     * Provides information on the package where the vulnerability is located.
     * 
     */
    @JsonProperty("package")
    public void setPackage(Package _package) {
        this._package = _package;
    }

    /**
     * Version of the vulnerable package.
     * 
     */
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    /**
     * Version of the vulnerable package.
     * 
     */
    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * ID that identifies the dependency in the scope of a dependency file.
     * 
     */


    /**
     * Tells whether this is a direct, top-level dependency of the scanned project.
     * 
     */
    @JsonProperty("direct")
    public Boolean getDirect() {
        return direct;
    }

    /**
     * Tells whether this is a direct, top-level dependency of the scanned project.
     * 
     */
    @JsonProperty("direct")
    public void setDirect(Boolean direct) {
        this.direct = direct;
    }

    /**
     * Ancestors of the dependency, starting from a direct project dependency, and ending with an immediate parent of the dependency. The dependency itself is excluded from the path. Direct dependencies have no path.
     * 
     */
    @JsonProperty("dependency_path")
    public List<DependencyPath> getDependencyPath() {
        return dependencyPath;
    }

    /**
     * Ancestors of the dependency, starting from a direct project dependency, and ending with an immediate parent of the dependency. The dependency itself is excluded from the path. Direct dependencies have no path.
     * 
     */
    @JsonProperty("dependency_path")
    public void setDependencyPath(List<DependencyPath> dependencyPath) {
        this.dependencyPath = dependencyPath;
    }


}
