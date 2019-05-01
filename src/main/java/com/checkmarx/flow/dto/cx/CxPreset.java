package com.checkmarx.flow.dto.cx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "name",
        "ownerName",
        "link"
})
public class CxPreset {
    @JsonProperty("id")
    public Integer id;
    @JsonProperty("name")
    public String name;
    @JsonProperty("ownerName")
    public String ownerName;

    @JsonProperty("link")
    @Valid
    public Link link;

    public CxPreset() {
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public @Valid Link getLink() {
        return this.link;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setLink(@Valid Link link) {
        this.link = link;
    }

    public String toString() {
        return "CxPreset(id=" + this.getId() + ", name=" + this.getName() + ", ownerName=" + this.getOwnerName() + ", link=" + this.getLink() + ")";
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "rel",
            "uri"
    })
    public static class Link {

        @JsonProperty("rel")
        public String rel;
        @JsonProperty("uri")
        public String uri;

    }
}