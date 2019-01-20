package com.custodela.machina.dto.cx;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({
        "id",
        "teamId",
        "name",
        "isPublic",
        "customFields",
        "links"
})
public class CxProject {

    @JsonProperty("id")
    public Integer id;
    @JsonProperty("teamId")
    public String teamId;
    @JsonProperty("name")
    public String name;
    @JsonProperty("isPublic")
    public Boolean isPublic;
    @JsonProperty("customFields")
    public List<CustomField> customFields;
    @JsonProperty("links")
    public List<Link> links;

    @java.beans.ConstructorProperties({"id", "teamId", "name", "isPublic", "customFields", "links"})
    CxProject(Integer id, String teamId, String name, Boolean isPublic, List<CustomField> customFields, List<Link> links) {
        this.id = id;
        this.teamId = teamId;
        this.name = name;
        this.isPublic = isPublic;
        this.customFields = customFields;
        this.links = links;
    }

    public static CxProjectBuilder builder() {
        return new CxProjectBuilder();
    }

    public Integer getId() {
        return this.id;
    }

    public String getTeamId() {
        return this.teamId;
    }

    public String getName() {
        return this.name;
    }

    public Boolean getIsPublic() {
        return this.isPublic;
    }

    public List<CustomField> getCustomFields() {
        return this.customFields;
    }

    public List<Link> getLinks() {
        return this.links;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setCustomFields(List<CustomField> customFields) {
        this.customFields = customFields;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public String toString() {
        return "CxProject(id=" + this.getId() + ", teamId=" + this.getTeamId() + ", name=" + this.getName() + ", isPublic=" + this.getIsPublic() + ", customFields=" + this.getCustomFields() + ", links=" + this.getLinks() + ")";
    }

    @JsonPropertyOrder({
            "id",
            "value",
            "name"
    })
    public static class CustomField {

        @JsonProperty("id")
        public Integer id;
        @JsonProperty("value")
        public String value;
        @JsonProperty("name")
        public String name;

        public Integer getId() {
            return this.id;
        }

        public String getValue() {
            return this.value;
        }

        public String getName() {
            return this.name;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonPropertyOrder({
            "type",
            "rel",
            "uri"
    })
    public static class Link {

        @JsonProperty("rel")
        public String rel;
        @JsonProperty("uri")
        public String uri;
        @JsonProperty("type")
        public String type;

        public String getRel() {
            return this.rel;
        }

        public String getUri() {
            return this.uri;
        }

        public String getType() {
            return this.type;
        }

        public void setRel(String rel) {
            this.rel = rel;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class CxProjectBuilder {
        private Integer id;
        private String teamId;
        private String name;
        private Boolean isPublic;
        private List<CustomField> customFields;
        private List<Link> links;

        CxProjectBuilder() {
        }

        public CxProject.CxProjectBuilder id(Integer id) {
            this.id = id;
            return this;
        }

        public CxProject.CxProjectBuilder teamId(String teamId) {
            this.teamId = teamId;
            return this;
        }

        public CxProject.CxProjectBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CxProject.CxProjectBuilder isPublic(Boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        public CxProject.CxProjectBuilder customFields(List<CustomField> customFields) {
            this.customFields = customFields;
            return this;
        }

        public CxProject.CxProjectBuilder links(List<Link> links) {
            this.links = links;
            return this;
        }

        public CxProject build() {
            return new CxProject(id, teamId, name, isPublic, customFields, links);
        }

        public String toString() {
            return "CxProject.CxProjectBuilder(id=" + this.id + ", teamId=" + this.teamId + ", name=" + this.name + ", isPublic=" + this.isPublic + ", customFields=" + this.customFields + ", links=" + this.links + ")";
        }
    }
}

