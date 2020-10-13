
package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "ref",
    "ref_type",
    "repository",
    "sender",
    "installation"
})
public class DeleteEvent {

    @JsonProperty("ref")
    private String ref;
    @JsonProperty("ref_type")
    private String ref_type;
    @JsonProperty("repository")
    private Repository repository;
    @JsonProperty("sender")
    private Sender sender;
    @JsonProperty("installation")
    private Installation installation;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonProperty("ref")
    public String getRef() {
        return ref;
    }

    @JsonProperty("ref")
    public void setRef(String ref) {
        this.ref = ref;
    }

    @JsonProperty("ref_type")
    public String getRefType() {
        return ref_type;
    }

    @JsonProperty("ref_type")
    public void setRefType(String ref_type) {
        this.ref_type = ref_type;
    }
    
    @JsonProperty("repository")
    public Repository getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @JsonProperty("sender")
    public Sender getSender() {
        return sender;
    }

    @JsonProperty("sender")
    public void setSender(Sender sender) {
        this.sender = sender;
    }

    @JsonProperty("installation")
    public Installation getInstallation() {
        return installation;
    }

    @JsonProperty("installation")
    public void setInstallation(Installation installation) {
        this.installation = installation;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
