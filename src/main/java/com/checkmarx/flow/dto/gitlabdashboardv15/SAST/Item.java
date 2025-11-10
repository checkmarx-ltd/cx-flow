package com.checkmarx.flow.dto.gitlabdashboardv15.SAST;



import com.fasterxml.jackson.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(of = {"fileLocation", "nodeType"})
public class Item {

    @JsonProperty("file_location")
    private FileLocation fileLocation;

    @JsonProperty("node_type")
    private String nodeType;

    @JsonProperty("type")
    private String type;

    @JsonProperty("file_location")
    public FileLocation getFileLocation() {
        return fileLocation;
    }


    @JsonProperty("file_location")
    public void setFileLocation(FileLocation fileLocation) {
        this.fileLocation = fileLocation;
    }

    @JsonProperty("node_type")
    public String getNodeType() {
        return nodeType;
    }

    @JsonProperty("node_type")
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
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
