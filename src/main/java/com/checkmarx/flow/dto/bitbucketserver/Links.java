
package com.checkmarx.flow.dto.bitbucketserver;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "self"
})
public class Links {

    @JsonProperty("self")
    private List<Object> self = null;

    @JsonProperty("self")
    public List<Object> getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(List<Object> self) {
        this.self = self;
    }

}
