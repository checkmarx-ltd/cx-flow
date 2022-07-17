
package com.checkmarx.flow.gitdashboardnewver;

import java.net.URI;
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
import lombok.Builder;
import lombok.Data;


/**
 * Object defining the analyzer used to perform the scan. Analyzers typically delegate to an underlying scanner to run the scan.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "name",
    "url",
    "vendor",
    "version"
})
@Generated("jsonschema2pojo")
@Data
@Builder
public class Analyzer {

    /**
     * Unique id that identifies the analyzer.
     * (Required)
     * 
     */
    @JsonProperty("id")
    @JsonPropertyDescription("Unique id that identifies the analyzer.")
    @Builder.Default
    private String id="Checkmarx-SAST";
    /**
     * A human readable value that identifies the analyzer, not required to be unique.
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("A human readable value that identifies the analyzer, not required to be unique.")
    @Builder.Default
    private String name="Checkmarx";
    /**
     * A link to more information about the analyzer.
     * 
     */
    @JsonProperty("url")
    @JsonPropertyDescription("A link to more information about the analyzer.")
    @Builder.Default
    private URI url= URI.create("https://checkmarx.company.com/");
    /**
     * The vendor/maintainer of the analyzer.
     * (Required)
     * 
     */
    @JsonProperty("vendor")
    @JsonPropertyDescription("The vendor/maintainer of the analyzer.")
    @Builder.Default
    private Vendor vendor;
    /**
     * The version of the analyzer.
     * (Required)
     * 
     */
    @JsonProperty("version")
    @JsonPropertyDescription("The version of the analyzer.")
    @Builder.Default
    private String version="9.X";
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Unique id that identifies the analyzer.
     * (Required)
     * 
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * Unique id that identifies the analyzer.
     * (Required)
     * 
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * A human readable value that identifies the analyzer, not required to be unique.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * A human readable value that identifies the analyzer, not required to be unique.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * A link to more information about the analyzer.
     * 
     */
    @JsonProperty("url")
    public URI getUrl() {
        return url;
    }

    /**
     * A link to more information about the analyzer.
     * 
     */
    @JsonProperty("url")
    public void setUrl(URI url) {
        this.url = url;
    }

    /**
     * The vendor/maintainer of the analyzer.
     * (Required)
     * 
     */
    @JsonProperty("vendor")
    public Vendor getVendor() {
        return vendor;
    }

    /**
     * The vendor/maintainer of the analyzer.
     * (Required)
     * 
     */
    @JsonProperty("vendor")
    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    /**
     * The version of the analyzer.
     * (Required)
     * 
     */
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    /**
     * The version of the analyzer.
     * (Required)
     * 
     */
    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
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
