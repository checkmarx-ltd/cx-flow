
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
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "fixes",
    "summary",
    "diff"
})
@Generated("jsonschema2pojo")
public class Remediation {

    /**
     * An array of strings that represent references to vulnerabilities fixed by this remediation.
     * (Required)
     * 
     */
    @JsonProperty("fixes")
    @JsonPropertyDescription("An array of strings that represent references to vulnerabilities fixed by this remediation.")
    private List<Fix> fixes = null;
    /**
     * An overview of how the vulnerabilities were fixed.
     * (Required)
     * 
     */
    @JsonProperty("summary")
    @JsonPropertyDescription("An overview of how the vulnerabilities were fixed.")
    private String summary;
    /**
     * A base64-encoded remediation code diff, compatible with git apply.
     * (Required)
     * 
     */
    @JsonProperty("diff")
    @JsonPropertyDescription("A base64-encoded remediation code diff, compatible with git apply.")
    private String diff;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * An array of strings that represent references to vulnerabilities fixed by this remediation.
     * (Required)
     * 
     */
    @JsonProperty("fixes")
    public List<Fix> getFixes() {
        return fixes;
    }

    /**
     * An array of strings that represent references to vulnerabilities fixed by this remediation.
     * (Required)
     * 
     */
    @JsonProperty("fixes")
    public void setFixes(List<Fix> fixes) {
        this.fixes = fixes;
    }

    /**
     * An overview of how the vulnerabilities were fixed.
     * (Required)
     * 
     */
    @JsonProperty("summary")
    public String getSummary() {
        return summary;
    }

    /**
     * An overview of how the vulnerabilities were fixed.
     * (Required)
     * 
     */
    @JsonProperty("summary")
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * A base64-encoded remediation code diff, compatible with git apply.
     * (Required)
     * 
     */
    @JsonProperty("diff")
    public String getDiff() {
        return diff;
    }

    /**
     * A base64-encoded remediation code diff, compatible with git apply.
     * (Required)
     * 
     */
    @JsonProperty("diff")
    public void setDiff(String diff) {
        this.diff = diff;
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
