package com.checkmarx.flow.dto.jira;

import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.net.URI;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {
    private Map<String, String> avatarUrls;
    private Long id;
    private String key;
    private String name;
    private ProjectCategory projectCategory;
    private URI self;
    private boolean simplified;
    private String style;
}

