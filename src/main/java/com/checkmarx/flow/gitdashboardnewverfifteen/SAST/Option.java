
package com.checkmarx.flow.gitdashboardnewverfifteen.SAST;

import java.util.HashMap;
import java.util.LinkedHashMap;
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


/**
 * A configuration option used for this scan.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "source",
    "value"
})
@Generated("jsonschema2pojo")
public class Option {

    /**
     * The configuration option name.
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("The configuration option name.")
    private String name;
    /**
     * The source of this option.
     * 
     */
    @JsonProperty("source")
    @JsonPropertyDescription("The source of this option.")
    private Source source;
    /**
     * The value used for this scan.
     * (Required)
     * 
     */
    @JsonProperty("value")
    @JsonPropertyDescription("The value used for this scan.")
    private Boolean value;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * The configuration option name.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * The configuration option name.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The source of this option.
     * 
     */
    @JsonProperty("source")
    public Source getSource() {
        return source;
    }

    /**
     * The source of this option.
     * 
     */
    @JsonProperty("source")
    public void setSource(Source source) {
        this.source = source;
    }

    /**
     * The value used for this scan.
     * (Required)
     * 
     */
    @JsonProperty("value")
    public Boolean getValue() {
        return value;
    }

    /**
     * The value used for this scan.
     * (Required)
     * 
     */
    @JsonProperty("value")
    public void setValue(Boolean value) {
        this.value = value;
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
     * The source of this option.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Source {

        ARGUMENT("argument"),
        FILE("file"),
        ENV_VARIABLE("env_variable"),
        OTHER("other");
        private final String value;
        private final static Map<String, Source> CONSTANTS = new HashMap<String, Source>();

        static {
            for (Source c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Source(String value) {
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
        public static Source fromValue(String value) {
            Source constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
