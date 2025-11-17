package com.checkmarx.flow.dto.gitlabdashboardv15.SAST;


import com.fasterxml.jackson.annotation.*;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class FileLocation {

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("line_end")
    private Integer lineEnd;

    @JsonProperty("line_start")
    private Integer lineStart;

    @JsonProperty("type")
    private String type;

    @JsonProperty("file_name")
    public String getFileName() {
        return fileName;
    }

    @JsonProperty("file_name")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @JsonProperty("line_end")
    public Integer getLineEnd() {
        return lineEnd;
    }

    @JsonProperty("line_end")
    public void setLineEnd(Integer lineEnd) {
        this.lineEnd = lineEnd;
    }

    @JsonProperty("line_start")
    public Integer getLineStart() {
        return lineStart;
    }

    @JsonProperty("line_start")
    public void setLineStart(Integer lineStart) {
        this.lineStart = lineStart;
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