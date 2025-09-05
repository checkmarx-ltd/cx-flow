package com.checkmarx.flow.dto.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.net.URI;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssue {
    private String id;
    private String key;
    private URI self;
    private String expand;
    private Fields fields;
}

