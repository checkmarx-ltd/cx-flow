
package com.checkmarx.flow.dto.bitbucket.mainBranch;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "avatar",
    "html",
    "self"
})
@Generated("jsonschema2pojo")
public class Links__2 {

    @JsonProperty("avatar")
    private Avatar__2 avatar;
    @JsonProperty("html")
    private Html__2 html;
    @JsonProperty("self")
    private Self__2 self;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("avatar")
    public Avatar__2 getAvatar() {
        return avatar;
    }

    @JsonProperty("avatar")
    public void setAvatar(Avatar__2 avatar) {
        this.avatar = avatar;
    }

    @JsonProperty("html")
    public Html__2 getHtml() {
        return html;
    }

    @JsonProperty("html")
    public void setHtml(Html__2 html) {
        this.html = html;
    }

    @JsonProperty("self")
    public Self__2 getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(Self__2 self) {
        this.self = self;
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
