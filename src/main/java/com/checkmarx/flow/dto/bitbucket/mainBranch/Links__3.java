
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
    "self",
    "html",
    "avatar"
})
@Generated("jsonschema2pojo")
public class Links__3 {

    @JsonProperty("self")
    private Self__3 self;
    @JsonProperty("html")
    private Html__3 html;
    @JsonProperty("avatar")
    private Avatar__3 avatar;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("self")
    public Self__3 getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(Self__3 self) {
        this.self = self;
    }

    @JsonProperty("html")
    public Html__3 getHtml() {
        return html;
    }

    @JsonProperty("html")
    public void setHtml(Html__3 html) {
        this.html = html;
    }

    @JsonProperty("avatar")
    public Avatar__3 getAvatar() {
        return avatar;
    }

    @JsonProperty("avatar")
    public void setAvatar(Avatar__3 avatar) {
        this.avatar = avatar;
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
