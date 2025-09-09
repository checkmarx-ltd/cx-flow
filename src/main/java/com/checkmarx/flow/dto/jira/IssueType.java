package com.checkmarx.flow.dto.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.net.URI;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueType {
    private URI self;
    private String description;
    private URI iconUrl;
    private String name;
    private boolean subtask;
    private long id;
    private long avatarId;
    private int hierarchyLevel;
}
