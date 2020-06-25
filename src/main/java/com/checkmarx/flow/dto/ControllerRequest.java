package com.checkmarx.flow.dto;

import lombok.*;

import java.util.List;

/**
 * Represents a standard list of parameters that are passed to webhook controllers.
 * All the parameters are optional.
 *
 * Field names need to be kept as is for backward compatibility unless we find a better solution.
 */
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
    private String override;

    // Bug tracker.
    private String bug;

    // trackApplicationOnly
    private Boolean appOnly;
}
