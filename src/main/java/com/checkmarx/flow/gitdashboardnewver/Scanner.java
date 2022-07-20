
package com.checkmarx.flow.gitdashboardnewver;

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
 * Object defining the scanner used to perform the scan.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "name",
    "url",
    "version",
    "vendor"
})
@Generated("jsonschema2pojo")
@Data
@Builder
public class Scanner {

    /**
     * Unique id that identifies the scanner.
     * (Required)
     * 
     */
    @JsonProperty("id")
    @JsonPropertyDescription("Unique id that identifies the scanner.")
    @Builder.Default
    private String id="Checkmarx-SAST";
    /**
     * A human readable value that identifies the scanner, not required to be unique.
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("A human readable value that identifies the scanner, not required to be unique.")
    @Builder.Default
    private String name="Checkmarx";
    /**
     * A link to more information about the scanner.
     * 
     */
    @JsonProperty("url")
    @JsonPropertyDescription("A link to more information about the scanner.")
    @Builder.Default
    private String url="https://checkmarx.company.com/";
    /**
     * The version of the scanner.
     * (Required)
     * 
     */
    @JsonProperty("version")
    @JsonPropertyDescription("The version of the scanner.")
    @Builder.Default
    private String version="9.X";
    /**
     * The vendor/maintainer of the scanner.
     * (Required)
     * 
     */
    @JsonProperty("vendor")
    @JsonPropertyDescription("The vendor/maintainer of the scanner.")
    @Builder.Default
    private Vendor__1 vendor;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Unique id that identifies the scanner.
     * (Required)
     * 
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * Unique id that identifies the scanner.
     * (Required)
     * 
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * A human readable value that identifies the scanner, not required to be unique.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * A human readable value that identifies the scanner, not required to be unique.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * A link to more information about the scanner.
     * 
     */
    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    /**
     * A link to more information about the scanner.
     * 
     */
    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * The version of the scanner.
     * (Required)
     * 
     */
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    /**
     * The version of the scanner.
     * (Required)
     * 
     */
    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * The vendor/maintainer of the scanner.
     * (Required)
     * 
     */
    @JsonProperty("vendor")
    public Vendor__1 getVendor() {
        return vendor;
    }

    /**
     * The vendor/maintainer of the scanner.
     * (Required)
     * 
     */
    @JsonProperty("vendor")
    public void setVendor(Vendor__1 vendor) {
        this.vendor = vendor;
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
