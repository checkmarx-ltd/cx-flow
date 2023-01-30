
package com.checkmarx.flow.gitlabdashboardfifteen.sca;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;


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
@Builder
@Data
public class Dependency {

    /**
     * Provides information on the package where the vulnerability is located.
     * (Required)
     * 
     */
    @JsonProperty("package")
    @JsonPropertyDescription("Provides information on the package where the vulnerability is located.")
    private Package _package;
    /**
     * Version of the vulnerable package.
     * (Required)
     * 
     */
    @JsonProperty("version")
    @JsonPropertyDescription("Version of the vulnerable package.")
    private String version;
    /**
     * ID that identifies the dependency in the scope of a dependency file.
     * 
     */
    @JsonProperty("iid")
    @JsonPropertyDescription("ID that identifies the dependency in the scope of a dependency file.")
    private Double iid;
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
     * (Required)
     * 
     */
    @JsonProperty("package")
    public Package getPackage() {
        return _package;
    }

    /**
     * Provides information on the package where the vulnerability is located.
     * (Required)
     * 
     */
    @JsonProperty("package")
    public void setPackage(Package _package) {
        this._package = _package;
    }

    /**
     * Version of the vulnerable package.
     * (Required)
     * 
     */
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    /**
     * Version of the vulnerable package.
     * (Required)
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
    @JsonProperty("iid")
    public Double getIid() {
        return iid;
    }

    /**
     * ID that identifies the dependency in the scope of a dependency file.
     * 
     */
    @JsonProperty("iid")
    public void setIid(Double iid) {
        this.iid = iid;
    }

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
