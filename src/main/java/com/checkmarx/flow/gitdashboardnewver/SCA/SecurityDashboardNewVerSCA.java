
package com.checkmarx.flow.gitdashboardnewver.SCA;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

import com.checkmarx.flow.gitdashboardnewver.DependencyFile;
import com.checkmarx.flow.gitdashboardnewver.Remediation;
import com.checkmarx.flow.gitdashboardnewver.Scan;
import com.checkmarx.flow.gitdashboardnewver.Vulnerability;
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
@Data
@Builder
public class SecurityDashboardNewVerSCA {

    @JsonProperty("scan")
    private com.checkmarx.flow.gitdashboardnewver.SCA.Scan scan;
    /**
     * URI pointing to the validating security report schema.
     * 
     */
    @JsonProperty("schema")
    @JsonPropertyDescription("URI pointing to the validating security report schema.")
    private URI schema;
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
    private List<com.checkmarx.flow.gitdashboardnewver.SCA.Vulnerability> vulnerabilities = null;
    /**
     * An array of objects containing information on available remediations, along with patch diffs to apply.
     * 
     */
    @JsonProperty("remediations")
    @JsonPropertyDescription("An array of objects containing information on available remediations, along with patch diffs to apply.")
    private List<Remediation> remediations = null;
    /**
     * List of dependency files identified in the project.
     * (Required)
     * 
     */
    @JsonProperty("dependency_files")
    @JsonPropertyDescription("List of dependency files identified in the project.")
    private List<com.checkmarx.flow.gitdashboardnewver.SCA.DependencyFile> dependencyFiles = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("scan")
    public com.checkmarx.flow.gitdashboardnewver.SCA.Scan getScan() {
        return scan;
    }

    @JsonProperty("scan")
    public void setScan(com.checkmarx.flow.gitdashboardnewver.SCA.Scan scan) {
        this.scan = scan;
    }

    /**
     * URI pointing to the validating security report schema.
     * 
     */
    @JsonProperty("schema")
    public URI getSchema() {
        return schema;
    }

    /**
     * URI pointing to the validating security report schema.
     * 
     */
    @JsonProperty("schema")
    public void setSchema(URI schema) {
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
    public List<com.checkmarx.flow.gitdashboardnewver.SCA.Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    /**
     * Array of vulnerability objects.
     * (Required)
     * 
     */
    @JsonProperty("vulnerabilities")
    public void setVulnerabilities(List<com.checkmarx.flow.gitdashboardnewver.SCA.Vulnerability> vulnerabilities) {
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
    public List<com.checkmarx.flow.gitdashboardnewver.SCA.DependencyFile> getDependencyFiles() {
        return dependencyFiles;
    }

    /**
     * List of dependency files identified in the project.
     * (Required)
     * 
     */
    @JsonProperty("dependency_files")
    public void setDependencyFiles(List<com.checkmarx.flow.gitdashboardnewver.SCA.DependencyFile> dependencyFiles) {
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
