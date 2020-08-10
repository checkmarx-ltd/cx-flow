package com.checkmarx.flow.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "self",
        "html"
})
public class Links__ {

    @JsonProperty("self")
    private Self self;
    @JsonProperty("html")
    private Html html;

    @JsonProperty("self")
    public Self getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(Self self) {
        this.self = self;
    }

    @JsonProperty("html")
    public Html getHtml() {
        return html;
    }

    @JsonProperty("html")
    public void setHtml(Html html) {
        this.html = html;
    }

}