package com.checkmarx.flow.dto.cx;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.Valid;
import java.util.List;

@JsonPropertyOrder({
        "id",
        "teamId",
        "owningTeam",
        "name",
        "isPublic",
        "links"
})
public class CxCreateProject {
    @JsonProperty("id")
    public String id;
    @JsonProperty("teamId")
    public String teamId;
    @JsonProperty("owningTeam")
    public String owningTeam;
    @JsonProperty("name")
    public String name;
    @JsonProperty("isPublic")
    public Boolean isPublic;

    @JsonProperty("links")
    @Valid
    public List<Link> links;

    @java.beans.ConstructorProperties({"id", "teamId", "owningTeam", "name", "isPublic", "links"})
    CxCreateProject(String id, String teamId, String owningTeam, String name, Boolean isPublic, @Valid List<Link> links) {
        this.id = id;
        this.teamId = teamId;
        this.owningTeam = owningTeam;
        this.name = name;
        this.isPublic = isPublic;
        this.links = links;
    }

    public static CxCreateProjectBuilder builder() {
        return new CxCreateProjectBuilder();
    }

    public String getId() {
        return this.id;
    }

    public String getTeamId() {
        return this.teamId;
    }

    public String getOwningTeam() {
        return this.owningTeam;
    }

    public String getName() {
        return this.name;
    }

    public Boolean getIsPublic() {
        return this.isPublic;
    }

    public @Valid List<Link> getLinks() {
        return this.links;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public void setOwningTeam(String owningTeam) {
        this.owningTeam = owningTeam;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setLinks(@Valid List<Link> links) {
        this.links = links;
    }

    public String toString() {
        return "CxCreateProject(id=" + this.getId() + ", teamId=" + this.getTeamId() + ", owningTeam=" + this.getOwningTeam() + ", name=" + this.getName() + ", isPublic=" + this.getIsPublic() + ", links=" + this.getLinks() + ")";
    }

    public static class CxCreateProjectBuilder {
        private String id;
        private String teamId;
        private String owningTeam;
        private String name;
        private Boolean isPublic;
        private @Valid List<Link> links;

        CxCreateProjectBuilder() {
        }

        public CxCreateProject.CxCreateProjectBuilder id(String id) {
            this.id = id;
            return this;
        }

        public CxCreateProject.CxCreateProjectBuilder teamId(String teamId) {
            this.teamId = teamId;
            return this;
        }

        public CxCreateProject.CxCreateProjectBuilder owningTeam(String owningTeam) {
            this.owningTeam = owningTeam;
            return this;
        }

        public CxCreateProject.CxCreateProjectBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CxCreateProject.CxCreateProjectBuilder isPublic(Boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        public CxCreateProject.CxCreateProjectBuilder links(@Valid List<Link> links) {
            this.links = links;
            return this;
        }

        public CxCreateProject build() {
            return new CxCreateProject(id, teamId, owningTeam, name, isPublic, links);
        }

        public String toString() {
            return "CxCreateProject.CxCreateProjectBuilder(id=" + this.id + ", teamId=" + this.teamId + ", owningTeam=" + this.owningTeam + ", name=" + this.name + ", isPublic=" + this.isPublic + ", links=" + this.links + ")";
        }
    }

    @JsonPropertyOrder({
            "rel",
            "uri"
    })
    public class Link {
        @JsonProperty("rel")
        public String rel;
        @JsonProperty("uri")
        public String uri;

    }
}
