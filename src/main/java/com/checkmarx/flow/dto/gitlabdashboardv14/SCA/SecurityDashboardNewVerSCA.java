
package com.checkmarx.flow.dto.gitlabdashboardv14.SCA;

import java.net.URI;
import java.util.List;
import javax.annotation.Generated;

import com.checkmarx.flow.dto.gitlabdashboardv14.Remediation;
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
    private Scan scan;
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
    @Builder.Default
    private List<Vulnerability> vulnerabilities = null;
    /**
     * An array of objects containing information on available remediations, along with patch diffs to apply.
     * 
     */
    @JsonProperty("remediations")
    @JsonPropertyDescription("An array of objects containing information on available remediations, along with patch diffs to apply.")
    @Builder.Default
    private List<Remediation> remediations = null;
    /**
     * List of dependency files identified in the project.
     * (Required)
     * 
     */
    @JsonProperty("dependency_files")
    @JsonPropertyDescription("List of dependency files identified in the project.")
    @Builder.Default
    private List<DependencyFile> dependencyFiles = null;

    @JsonProperty("scan")
    public Scan getScan() {
        return scan;
    }

    @JsonProperty("scan")
    public void setScan(Scan scan) {
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


}
