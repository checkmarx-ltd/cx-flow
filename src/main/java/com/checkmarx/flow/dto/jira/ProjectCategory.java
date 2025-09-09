package com.checkmarx.flow.dto.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectCategory {
    private String description;
    private String id;
    private String name;
    private String self;
}

