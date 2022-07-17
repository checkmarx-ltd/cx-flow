
package com.checkmarx.flow.gitdashboardnewver.SCA;

import com.checkmarx.flow.gitdashboardnewver.DependencyPath__1;
import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
public class Dependency__1 {

    /**
     * Provides information on the package where the vulnerability is located.
     * 
     */
    @JsonProperty("package")
    @JsonPropertyDescription("Provides information on the package where the vulnerability is located.")
    private Package__1 _package;
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
    private List<DependencyPath__1> dependencyPath = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Provides information on the package where the vulnerability is located.
     * 
     */
    @JsonProperty("package")
    public Package__1 getPackage() {
        return _package;
    }

    /**
     * Provides information on the package where the vulnerability is located.
     * 
     */
    @JsonProperty("package")
    public void setPackage(Package__1 _package) {
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
    public List<DependencyPath__1> getDependencyPath() {
        return dependencyPath;
    }

    /**
     * Ancestors of the dependency, starting from a direct project dependency, and ending with an immediate parent of the dependency. The dependency itself is excluded from the path. Direct dependencies have no path.
     * 
     */
    @JsonProperty("dependency_path")
    public void setDependencyPath(List<DependencyPath__1> dependencyPath) {
        this.dependencyPath = dependencyPath;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
