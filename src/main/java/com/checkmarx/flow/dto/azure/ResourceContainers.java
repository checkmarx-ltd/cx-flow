
package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "collection",
    "account",
    "project"
})
public class ResourceContainers {

    @JsonProperty("collection")
    private Collection collection;
    @JsonProperty("account")
    private Account account;
    @JsonProperty("project")
    private Project_ project;

    @JsonProperty("collection")
    public Collection getCollection() {
        return collection;
    }

    @JsonProperty("collection")
    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    @JsonProperty("account")
    public Account getAccount() {
        return account;
    }

    @JsonProperty("account")
    public void setAccount(Account account) {
        this.account = account;
    }

    @JsonProperty("project")
    public Project_ getProject() {
        return project;
    }

    @JsonProperty("project")
    public void setProject(Project_ project) {
        this.project = project;
    }

}
