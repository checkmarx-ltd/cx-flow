package com.checkmarx.flow.dto.bitbucketserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "components",
        "parent",
        "name",
        "toString",
        "extension"
})
public class Path {

    @JsonProperty("components")
    private List<String> components = null;
    @JsonProperty("parent")
    private String parent;
    @JsonProperty("name")
    private String name;
    @JsonProperty("toString")
    private String toString;
    @JsonProperty("extension")
    private String extension;

    @JsonProperty("components")
    public List<String> getComponents() {
        return components;
    }

    @JsonProperty("components")
    public void setComponents(List<String> components) {
        this.components = components;
    }

    @JsonProperty("parent")
    public String getParent() {
        return parent;
    }

    @JsonProperty("parent")
    public void setParent(String parent) {
        this.parent = parent;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("toString")
    public String getToString() {
        return toString;
    }

    @JsonProperty("toString")
    public void setToString(String toString) {
        this.toString = toString;
    }

    @JsonProperty("extension")
    public String getExtension() {
        return extension;
    }

    @JsonProperty("extension")
    public void setExtension(String extension) {
        this.extension = extension;
    }

}