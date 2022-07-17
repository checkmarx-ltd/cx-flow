
package com.checkmarx.flow.gitdashboardnewver;

import java.net.URI;
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
public class SecurityDashboardNewVer {

    @JsonProperty("scan")
    @Builder.Default
    private Scan scan;
    /**
     * URI pointing to the validating security report schema.
     *
     */
    @JsonProperty("schema")
    @JsonPropertyDescription("URI pointing to the validating security report schema.")
    @Builder.Default
    private URI schema= URI.create("https://gitlab.com/gitlab-org/gitlab/-/blob/8a42b7e8ab41ec2920f02fb4b36f244bbbb4bfb8/lib/gitlab/ci/parsers/security/validators/schemas/14.1.2/sast-report-format.json");
    /**
     * The version of the schema to which the JSON report conforms.
     * (Required)
     *
     */

    @JsonProperty("version")
    @JsonPropertyDescription("The version of the schema to which the JSON report conforms.")
    @Builder.Default
    private String version="14.1.2";
    /**
     * Array of vulnerability objects.
     * (Required)
     *
     */
    @JsonProperty("vulnerabilities")
    @JsonPropertyDescription("Array of vulnerability objects.")
    private List<Vulnerability> vulnerabilities = null;
    /**
     * An array of objects containing information on available remediations, along with patch diffs to apply.
     *
     */
    @JsonProperty("remediations")
    @JsonPropertyDescription("An array of objects containing information on available remediations, along with patch diffs to apply.")
    private List<Remediation> remediations = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

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

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
