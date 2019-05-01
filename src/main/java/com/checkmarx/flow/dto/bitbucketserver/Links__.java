
package com.checkmarx.flow.dto.bitbucketserver;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "clone",
    "self"
})
public class Links__ {

    @JsonProperty("clone")
    private List<Clone> clone = null;
    @JsonProperty("self")
    private List<Self__> self = null;

    @JsonProperty("clone")
    public List<Clone> getClone() {
        return clone;
    }

    @JsonProperty("clone")
    public void setClone(List<Clone> clone) {
        this.clone = clone;
    }

    @JsonProperty("self")
    public List<Self__> getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(List<Self__> self) {
        this.self = self;
    }

}
