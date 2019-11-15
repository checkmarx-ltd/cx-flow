package com.checkmarx.flow.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CxProfile implements Serializable {

    @JsonProperty("name:")
    private String name;
    @JsonProperty("preset")
    private String preset;
    @JsonProperty("files")
    private List<String> files = null;
    @JsonProperty("weight")
    private List<Weight> weight = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = -3477897284989429131L;

    @JsonProperty("name:")
    public String getName() {
        return name;
    }

    @JsonProperty("name:")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("preset")
    public String getPreset() {
        return preset;
    }

    @JsonProperty("preset")
    public void setPreset(String preset) {
        this.preset = preset;
    }

    @JsonProperty("files")
    public List<String> getFiles() {
        return files;
    }

    @JsonProperty("files")
    public void setFiles(List<String> files) {
        this.files = files;
    }

    @JsonProperty("weight")
    public List<Weight> getWeight() {
        return weight;
    }

    @JsonProperty("weight")
    public void setWeight(List<Weight> weight) {
        this.weight = weight;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("preset", preset).append("files", files).append("weight", weight).append("additionalProperties", additionalProperties).toString();
    }

    public static class Weight implements Serializable {

        @JsonProperty("type")
        private String type;
        @JsonProperty("weight")
        private Integer weight;
        @JsonIgnore
        private Map<String, Object> additionalProperties = new HashMap<String, Object>();
        private final static long serialVersionUID = -7417991974576528003L;

        @JsonProperty("type")
        public String getType() {
            return type;
        }

        @JsonProperty("type")
        public void setType(String type) {
            this.type = type;
        }

        @JsonProperty("weight")
        public Integer getWeight() {
            return weight;
        }

        @JsonProperty("weight")
        public void setWeight(Integer weight) {
            this.weight = weight;
        }

        @JsonAnyGetter
        public Map<String, Object> getAdditionalProperties() {
            return this.additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperty(String name, Object value) {
            this.additionalProperties.put(name, value);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("type", type).append("weight", weight).append("additionalProperties", additionalProperties).toString();
        }
    }
}