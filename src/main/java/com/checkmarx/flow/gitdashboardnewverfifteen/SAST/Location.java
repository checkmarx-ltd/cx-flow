
package com.checkmarx.flow.gitdashboardnewverfifteen.SAST;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;


/**
 * Identifies the vulnerability's location.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "file",
        "start_line",
        "end_line",
        "class",
        "method"
})
@Generated("jsonschema2pojo")
@Data
@Builder
public class Location {

    /**
     * Path to the file where the vulnerability is located.
     *
     */
    @JsonProperty("file")
    @JsonPropertyDescription("Path to the file where the vulnerability is located.")
    private String file;
    /**
     * The first line of the code affected by the vulnerability.
     *
     */
    @JsonProperty("start_line")
    @JsonPropertyDescription("The first line of the code affected by the vulnerability.")
    private Integer startLine;
    /**
     * The last line of the code affected by the vulnerability.
     *
     */
    @JsonProperty("end_line")
    @JsonPropertyDescription("The last line of the code affected by the vulnerability.")
    private Integer endLine;
    /**
     * Provides the name of the class where the vulnerability is located.
     *
     */
    @JsonProperty("class")
    @JsonPropertyDescription("Provides the name of the class where the vulnerability is located.")
    private String _class;
    /**
     * Provides the name of the method where the vulnerability is located.
     *
     */
    @JsonProperty("method")
    @JsonPropertyDescription("Provides the name of the method where the vulnerability is located.")
    private String method;


    /**
     * Path to the file where the vulnerability is located.
     *
     */
    @JsonProperty("file")
    public String getFile() {
        return file;
    }

    /**
     * Path to the file where the vulnerability is located.
     *
     */
    @JsonProperty("file")
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * The first line of the code affected by the vulnerability.
     *
     */
    @JsonProperty("start_line")
    public Integer getStartLine() {
        return startLine;
    }

    /**
     * The first line of the code affected by the vulnerability.
     *
     */
    @JsonProperty("start_line")
    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    /**
     * The last line of the code affected by the vulnerability.
     *
     */
    @JsonProperty("end_line")
    public Integer getEndLine() {
        return endLine;
    }

    /**
     * The last line of the code affected by the vulnerability.
     *
     */
    @JsonProperty("end_line")
    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    /**
     * Provides the name of the class where the vulnerability is located.
     *
     */
    @JsonProperty("class")
    public String getClass_() {
        return _class;
    }

    /**
     * Provides the name of the class where the vulnerability is located.
     *
     */
    @JsonProperty("class")
    public void setClass_(String _class) {
        this._class = _class;
    }

    /**
     * Provides the name of the method where the vulnerability is located.
     *
     */
    @JsonProperty("method")
    public String getMethod() {
        return method;
    }

    /**
     * Provides the name of the method where the vulnerability is located.
     *
     */
    @JsonProperty("method")
    public void setMethod(String method) {
        this.method = method;
    }



}
