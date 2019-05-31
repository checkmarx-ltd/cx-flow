
package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "text",
    "html",
    "markdown"
})
public class Message {

    @JsonProperty("text")
    private String text;
    @JsonProperty("html")
    private String html;
    @JsonProperty("markdown")
    private String markdown;

    @JsonProperty("text")
    public String getText() {
        return text;
    }

    @JsonProperty("text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("html")
    public String getHtml() {
        return html;
    }

    @JsonProperty("html")
    public void setHtml(String html) {
        this.html = html;
    }

    @JsonProperty("markdown")
    public String getMarkdown() {
        return markdown;
    }

    @JsonProperty("markdown")
    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

}
