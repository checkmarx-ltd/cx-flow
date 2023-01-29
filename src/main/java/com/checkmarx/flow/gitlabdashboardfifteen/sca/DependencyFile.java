
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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "path",
    "package_manager",
    "dependencies"
})
@Generated("jsonschema2pojo")
@Data
@Builder
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
    @Builder.Default
    private List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Dependency__1> dependencies = null;

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


}
