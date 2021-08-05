package com.checkmarx.flow.dto.iast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

@JsonIgnoreProperties
@Getter
@Setter
public class CreateIssue {
    @JsonProperty("assignee")
    @Valid
    @Pattern(regexp = "^[\\w@\\.-]{4,255}$")    //can be email and nickname
    private String assignee;

    @Pattern(regexp = "^[\\w\\.-]{1,255}$")
    @JsonProperty("namespace")
    @Valid
    private String namespace;

    @Pattern(regexp = "^[\\w\\.-]{1,255}$")
    @JsonProperty("repoName")
    @Valid
    private String repoName;

    @JsonProperty("project-id")
    @Valid
    @Min(0)
    private Integer projectId;

}
