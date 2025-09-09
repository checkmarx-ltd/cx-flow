package com.checkmarx.flow.dto.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.net.URI;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Status {
    private URI self;
    private String description;
    private long id;
    private StatusCategory statusCategory;
    private URI iconUrl;
    private String name;
}

