
package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeleteEvent extends EventCommon {

    @JsonProperty("ref")
    private String ref;
    @JsonProperty("ref_type")
    private String ref_type;

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

}
