
package com.checkmarx.flow.gitdashboardnewverfifteen.SCA;

import java.util.LinkedHashMap;
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
 * Report format for GitLab Dependency Scanning
 * <p>
 * This schema provides the the report format for Dependency Scanning analyzers (https://docs.gitlab.com/ee/user/application_security/dependency_scanning).
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "scan",
    "schema",
    "version",
    "vulnerabilities",
    "remediations",
    "dependency_files"
})
@Generated("jsonschema2pojo")
@Builder
@Data
public class SCASecurityDashboard {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("scan")
    private Scan scan;
    /**
     * URI pointing to the validating security report schema.
     * 
     */
    @JsonProperty("schema")
    @JsonPropertyDescription("URI pointing to the validating security report schema.")
    private String schema;
    /**
     * The version of the schema to which the JSON report conforms.
     * (Required)
     * 
     */
    @JsonProperty("version")
    @JsonPropertyDescription("The version of the schema to which the JSON report conforms.")
    private String version;
    /**
     * Array of vulnerability objects.
     * (Required)
     * 
     */
    @JsonProperty("vulnerabilities")
    @JsonPropertyDescription("Array of vulnerability objects.")
    private List<Vulnerability> vulnerabilities;
    /**
     * An array of objects containing information on available remediations, along with patch diffs to apply.
     * 
     */
    @JsonProperty("remediations")
    @JsonPropertyDescription("An array of objects containing information on available remediations, along with patch diffs to apply.")
    private List<Remediation> remediations;
    /**
     * List of dependency files identified in the project.
     * (Required)
     * 
     */
    @JsonProperty("dependency_files")
    @JsonPropertyDescription("List of dependency files identified in the project.")
    private List<DependencyFile> dependencyFiles;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("scan")
    public Scan getScan() {
        return scan;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("scan")
    public void setScan(Scan scan) {
        this.scan = scan;
    }

    /**
     * URI pointing to the validating security report schema.
     * 
     */
    @JsonProperty("schema")
    public String getSchema() {
        return schema;
    }

    /**
     * URI pointing to the validating security report schema.
     * 
     */
    @JsonProperty("schema")
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * The version of the schema to which the JSON report conforms.
     * (Required)
     * 
     */
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    /**
     * The version of the schema to which the JSON report conforms.
     * (Required)
     * 
     */
    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Array of vulnerability objects.
     * (Required)
     * 
     */
    @JsonProperty("vulnerabilities")
    public List<Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    /**
     * Array of vulnerability objects.
     * (Required)
     * 
     */
    @JsonProperty("vulnerabilities")
    public void setVulnerabilities(List<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

    /**
     * An array of objects containing information on available remediations, along with patch diffs to apply.
     * 
     */
    @JsonProperty("remediations")
    public List<Remediation> getRemediations() {
        return remediations;
    }

    /**
     * An array of objects containing information on available remediations, along with patch diffs to apply.
     * 
     */
    @JsonProperty("remediations")
    public void setRemediations(List<Remediation> remediations) {
        this.remediations = remediations;
    }

    /**
     * List of dependency files identified in the project.
     * (Required)
     * 
     */
    @JsonProperty("dependency_files")
    public List<DependencyFile> getDependencyFiles() {
        return dependencyFiles;
    }

    /**
     * List of dependency files identified in the project.
     * (Required)
     * 
     */
    @JsonProperty("dependency_files")
    public void setDependencyFiles(List<DependencyFile> dependencyFiles) {
        this.dependencyFiles = dependencyFiles;
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
