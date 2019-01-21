
package com.custodela.machina.dto.bitbucketserver;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "self"
})
public class Links_ {

    @JsonProperty("self")
    private List<Self_> self = null;

    @JsonProperty("self")
    public List<Self_> getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(List<Self_> self) {
        this.self = self;
    }

}
