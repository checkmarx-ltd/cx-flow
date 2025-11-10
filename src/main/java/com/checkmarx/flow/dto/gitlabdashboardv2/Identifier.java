package com.checkmarx.flow.dto.gitlabdashboardv2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Identifier {
    @JsonProperty("type")
    public String type;
    @JsonProperty("name")
    public String name;
    @JsonProperty("value")
    public String value;
    @JsonProperty("url")
    public String url;
}

