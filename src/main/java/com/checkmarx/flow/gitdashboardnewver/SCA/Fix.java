
package com.checkmarx.flow.gitdashboardnewver.SCA;

import java.util.HashMap;
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
    "cve"
})
@Generated("jsonschema2pojo")
public class Fix {

    /**
     * (Deprecated - use vulnerabilities[].id instead) A fingerprint string value that represents a concrete finding. This is used to determine whether two findings are same, which may not be 100% accurate. Note that this is NOT a CVE as described by https://cve.mitre.org/.
     * (Required)
     * 
     */
    @JsonProperty("cve")
    @JsonPropertyDescription("(Deprecated - use vulnerabilities[].id instead) A fingerprint string value that represents a concrete finding. This is used to determine whether two findings are same, which may not be 100% accurate. Note that this is NOT a CVE as described by https://cve.mitre.org/.")
    private String cve;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * (Deprecated - use vulnerabilities[].id instead) A fingerprint string value that represents a concrete finding. This is used to determine whether two findings are same, which may not be 100% accurate. Note that this is NOT a CVE as described by https://cve.mitre.org/.
     * (Required)
     * 
     */
    @JsonProperty("cve")
    public String getCve() {
        return cve;
    }

    /**
     * (Deprecated - use vulnerabilities[].id instead) A fingerprint string value that represents a concrete finding. This is used to determine whether two findings are same, which may not be 100% accurate. Note that this is NOT a CVE as described by https://cve.mitre.org/.
     * (Required)
     * 
     */
    @JsonProperty("cve")
    public void setCve(String cve) {
        this.cve = cve;
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
