package com.checkmarx.flow.dto.iast.manager.dto;


import com.checkmarx.flow.dto.iast.common.model.agent.ProgrammingLanguage;
import com.checkmarx.flow.dto.iast.manager.dto.projects.groups.ProjectGroupData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties("createdTime")
public class ProjectSummary {
    private Long projectId;

    private String projectName;

    private String artifactName;

    private String displayName;

    private Instant createdTime;

    private Scan completed;

    private RunningScanAggregation running;

    private boolean deletable;

    private boolean aggregationEnabled;

    private ProgrammingLanguage programmingLanguage;
    private List<String> teamNames;
    private Set<String> hosts;

    private Set<ProjectGroupData> projectGroups;

}
