
package com.checkmarx.flow.gitdashboardnewver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "path",
    "package_manager",
    "dependencies"
})
@Generated("jsonschema2pojo")
public class DependencyFile {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("path")
    private String path;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("package_manager")
    private String packageManager;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("dependencies")
    private List<Dependency__1> dependencies = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("path")
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("package_manager")
    public String getPackageManager() {
        return packageManager;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("package_manager")
    public void setPackageManager(String packageManager) {
        this.packageManager = packageManager;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("dependencies")
    public List<Dependency__1> getDependencies() {
        return dependencies;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("dependencies")
    public void setDependencies(List<Dependency__1> dependencies) {
        this.dependencies = dependencies;
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
