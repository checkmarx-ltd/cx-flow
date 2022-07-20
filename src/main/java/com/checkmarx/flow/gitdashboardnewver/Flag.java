
package com.checkmarx.flow.gitdashboardnewver;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


/**
 * Informational flags identified and assigned to a vulnerability.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "origin",
    "description"
})
@Generated("jsonschema2pojo")
@Data
@Builder
public class Flag {

    /**
     * Result of the scan.
     * (Required)
     *
     */
    @JsonProperty("type")
    @JsonPropertyDescription("Result of the scan.")
    private Flag.Type type;
    /**
     * Tool that issued the flag.
     * (Required)
     *
     */
    @JsonProperty("origin")
    @JsonPropertyDescription("Tool that issued the flag.")
    private String origin;
    /**
     * What the flag is about.
     * (Required)
     *
     */
    @JsonProperty("description")
    @JsonPropertyDescription("What the flag is about.")
    private String description;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Result of the scan.
     * (Required)
     *
     */
    @JsonProperty("type")
    public Flag.Type getType() {
        return type;
    }

    /**
     * Result of the scan.
     * (Required)
     *
     */
    @JsonProperty("type")
    public void setType(Flag.Type type) {
        this.type = type;
    }

    /**
     * Tool that issued the flag.
     * (Required)
     *
     */
    @JsonProperty("origin")
    public String getOrigin() {
        return origin;
    }

    /**
     * Tool that issued the flag.
     * (Required)
     *
     */
    @JsonProperty("origin")
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * What the flag is about.
     * (Required)
     *
     */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     * What the flag is about.
     * (Required)
     *
     */
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }


    /**
     * Result of the scan.
     *
     */
    @Generated("jsonschema2pojo")
    public enum Type {

        FLAGGED_AS_LIKELY_FALSE_POSITIVE("flagged-as-likely-false-positive");
        private final String value;
        private final static Map<String, Flag.Type> CONSTANTS = new HashMap<String, Flag.Type>();

        static {
            for (Flag.Type c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Flag.Type fromValue(String value) {
            Flag.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
