
package com.custodela.machina.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "raw",
    "markup",
    "html",
    "type"
})
public class Summary {

    @JsonProperty("raw")
    private String raw;
    @JsonProperty("markup")
    private String markup;
    @JsonProperty("html")
    private String html;
    @JsonProperty("type")
    private String type;

    @JsonProperty("raw")
    public String getRaw() {
        return raw;
    }

    @JsonProperty("raw")
    public void setRaw(String raw) {
        this.raw = raw;
    }

    @JsonProperty("markup")
    public String getMarkup() {
        return markup;
    }

    @JsonProperty("markup")
    public void setMarkup(String markup) {
        this.markup = markup;
    }

    @JsonProperty("html")
    public String getHtml() {
        return html;
    }

    @JsonProperty("html")
    public void setHtml(String html) {
        this.html = html;
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
