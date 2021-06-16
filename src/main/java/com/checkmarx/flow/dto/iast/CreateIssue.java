package com.checkmarx.flow.dto.iast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;

@JsonIgnoreProperties
@Getter
@Setter
public class CreateIssue {
    @JsonProperty("assignee")
    @Valid
    private String assignee;

    @JsonProperty("namespace")
    @Valid
    private String namespace;

    @JsonProperty("repoName")
    @Valid
    private String repoName;
}
