
package com.checkmarx.flow.gitdashboardnewverfifteen.SAST;

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
 * Report format for GitLab SAST
 * <p>
 * This schema provides the report format for Static Application Security Testing analyzers (https://docs.gitlab.com/ee/user/application_security/sast).
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "scan",
        "schema",
        "version",
        "vulnerabilities",
        "remediations"
})
@Generated("jsonschema2pojo")
@Data
@Builder
public class Gitlabdashboard {

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
    @Builder.Default
    private String version="15.10.3";
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


}
