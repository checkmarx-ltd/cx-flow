
package com.custodela.machina.dto.bitbucketserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "displayId",
    "latestCommit",
    "repository"
})
public class ToRef {

    @JsonProperty("id")
    private String id;
    @JsonProperty("displayId")
    private String displayId;
    @JsonProperty("latestCommit")
    private String latestCommit;
    @JsonProperty("repository")
    private Repository_ repository;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("displayId")
    public String getDisplayId() {
        return displayId;
    }

    @JsonProperty("displayId")
    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    @JsonProperty("latestCommit")
    public String getLatestCommit() {
        return latestCommit;
    }

    @JsonProperty("latestCommit")
    public void setLatestCommit(String latestCommit) {
        this.latestCommit = latestCommit;
    }

    @JsonProperty("repository")
    public Repository_ getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(Repository_ repository) {
        this.repository = repository;
    }

}
