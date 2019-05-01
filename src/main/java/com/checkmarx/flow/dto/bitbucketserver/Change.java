
package com.checkmarx.flow.dto.bitbucketserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "ref",
    "refId",
    "fromHash",
    "toHash",
    "type"
})
public class Change {

    @JsonProperty("ref")
    private Ref ref;
    @JsonProperty("refId")
    private String refId;
    @JsonProperty("fromHash")
    private String fromHash;
    @JsonProperty("toHash")
    private String toHash;
    @JsonProperty("type")
    private String type;

    @JsonProperty("ref")
    public Ref getRef() {
        return ref;
    }

    @JsonProperty("ref")
    public void setRef(Ref ref) {
        this.ref = ref;
    }

    @JsonProperty("refId")
    public String getRefId() {
        return refId;
    }

    @JsonProperty("refId")
    public void setRefId(String refId) {
        this.refId = refId;
    }

    @JsonProperty("fromHash")
    public String getFromHash() {
        return fromHash;
    }

    @JsonProperty("fromHash")
    public void setFromHash(String fromHash) {
        this.fromHash = fromHash;
    }

    @JsonProperty("toHash")
    public String getToHash() {
        return toHash;
    }

    @JsonProperty("toHash")
    public void setToHash(String toHash) {
        this.toHash = toHash;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

}
