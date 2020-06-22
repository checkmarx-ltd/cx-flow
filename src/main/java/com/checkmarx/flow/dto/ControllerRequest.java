package com.checkmarx.flow.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControllerRequest {
    private String application;
    private List<String> branch;
    private List<String> severity;
    private List<String> cwe;
    private List<String> category;
    private String project;
    private String team;
    private List<String> status;
    private String assignee;
    private String preset;
    private Boolean incremental;
    private List<String> excludeFiles;
    private List<String> excludeFolders;
    private String override;    // never used
    private String bug;
    private Boolean appOnly;
}
