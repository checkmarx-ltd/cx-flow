package com.checkmarx.flow.dto.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraSearchResponse {
    @JsonProperty("isLast")
    private boolean isLast;
    private List<JiraIssue> issues;
    @JsonProperty("nextPageToken")
    private String nextPageToken;
    private Integer startAt;
    private Integer maxResults;
    private Integer total;

}
