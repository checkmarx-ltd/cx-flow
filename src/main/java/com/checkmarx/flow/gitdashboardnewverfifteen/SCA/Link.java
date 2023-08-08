
package com.checkmarx.flow.gitdashboardnewverfifteen.SCA;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "url"
})
@Generated("jsonschema2pojo")
@Data
@Builder
public class Link {

    /**
     * Name of the vulnerability details link.
     *
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Name of the vulnerability details link.")
    private String name;
    /**
     * URL of the vulnerability details document.
     * (Required)
     *
     */
    @JsonProperty("url")
    @JsonPropertyDescription("URL of the vulnerability details document.")
    private String url;

    /**
     * Name of the vulnerability details link.
     *
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Name of the vulnerability details link.
     *
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * URL of the vulnerability details document.
     * (Required)
     *
     */
    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    /**
     * URL of the vulnerability details document.
     * (Required)
     *
     */
    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }



}
