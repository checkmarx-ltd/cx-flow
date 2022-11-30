
package com.checkmarx.flow.jira9X;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;



@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@JsonPropertyOrder({
        "required",
        "schema",
        "name",
        "fieldId",
        "autoCompleteUrl",
        "hasDefaultValue",
        "operations",
        "allowedValues",
        "defaultValue"
})

public class IssueFields {
    @JsonProperty("required")
    private Boolean required;
    @JsonProperty("schema")
    private FieldSchema schema;
    @JsonProperty("name")
    private String name;
    @JsonProperty("fieldId")
    private String fieldId;
    @JsonProperty("autoCompleteUrl")
    private URI autoCompleteUrl;
    @JsonProperty("hasDefaultValue")
    private Boolean hasDefaultValue;
    @JsonProperty("operations")
    private Set<StandardOperation> operations = null;
    @JsonProperty("allowedValues")
    private Iterable<Object> allowedValues = null;

    @JsonProperty("defaultValue")
    private Map<String, URI> defaultValue;

    @JsonProperty("required")
    public Boolean getRequired() {
        return required;
    }

    @JsonProperty("required")
    public void setRequired(Boolean required) {
        this.required = required;
    }

    @JsonProperty("schema")
    public FieldSchema getSchema() {
        return schema;
    }

    @JsonProperty("schema")
    public void setSchema(FieldSchema schema) {
        this.schema = schema;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("fieldId")
    public String getFieldId() {
        return fieldId;
    }

    @JsonProperty("fieldId")
    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    @JsonProperty("autoCompleteUrl")
    public URI getAutoCompleteUrl() {
        return autoCompleteUrl;
    }

    @JsonProperty("autoCompleteUrl")
    public void setAutoCompleteUrl(URI autoCompleteUrl) {
        this.autoCompleteUrl = autoCompleteUrl;
    }

    @JsonProperty("hasDefaultValue")
    public Boolean getHasDefaultValue() {
        return hasDefaultValue;
    }

    @JsonProperty("hasDefaultValue")
    public void setHasDefaultValue(Boolean hasDefaultValue) {
        this.hasDefaultValue = hasDefaultValue;
    }

    @JsonProperty("operations")
    public Set<StandardOperation> getOperations() {
        return operations;
    }

    @JsonProperty("operations")
    public void setOperations(Set<StandardOperation> operations) {
        this.operations = operations;
    }

    @JsonProperty("allowedValues")
    public Iterable<Object> getAllowedValues() {
        return allowedValues;
    }

    @JsonProperty("allowedValues")
    public void setAllowedValues(Iterable<Object> allowedValues) {
        this.allowedValues = allowedValues;
    }

    @JsonProperty("defaultValue")
    public Map<String, URI> getDefaultValue() {
        return defaultValue;
    }

    @JsonProperty("defaultValue")
    public void setDefaultValue(Map<String, URI> defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(IssueFields.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("required");
        sb.append('=');
        sb.append(((this.required == null)?"<null>":this.required));
        sb.append(',');
        sb.append("schema");
        sb.append('=');
        sb.append(((this.schema == null)?"<null>":this.schema));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("fieldId");
        sb.append('=');
        sb.append(((this.fieldId == null)?"<null>":this.fieldId));
        sb.append(',');
        sb.append("autoCompleteUrl");
        sb.append('=');
        sb.append(((this.autoCompleteUrl == null)?"<null>":this.autoCompleteUrl));
        sb.append(',');
        sb.append("hasDefaultValue");
        sb.append('=');
        sb.append(((this.hasDefaultValue == null)?"<null>":this.hasDefaultValue));
        sb.append(',');
        sb.append("operations");
        sb.append('=');
        sb.append(((this.operations == null)?"<null>":this.operations));
        sb.append(',');
        sb.append("allowedValues");
        sb.append('=');
        sb.append(((this.allowedValues == null)?"<null>":this.allowedValues));
        sb.append(',');
        sb.append("defaultValue");
        sb.append('=');
        sb.append(((this.defaultValue == null)?"<null>":this.defaultValue));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
