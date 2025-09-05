package com.checkmarx.flow.dto.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fields {
    private String summary;
    private String key;
    private Project project;
    @JsonProperty("issuetype")
    private IssueType issueType;
    private String updated;
    private String created;
    private Status status;
    private Set<String> labels;
    private Object description;
    private Priority priority;
}

