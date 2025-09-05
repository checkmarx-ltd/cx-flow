package com.checkmarx.flow.dto.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.net.URI;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusCategory {
    private URI self;
    private String colorName;
    private String name;
    private long id;
    private String key;
}
