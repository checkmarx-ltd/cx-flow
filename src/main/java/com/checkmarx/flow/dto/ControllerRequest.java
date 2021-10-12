package com.checkmarx.flow.dto;

import lombok.*;

import java.util.List;

/**
 * Represents a standard list of parameters that are passed to webhook controllers.
 * All the parameters are optional.
 *
 * Field names need to be kept as is for backward compatibility unless we find a better solution.
 *
 * Raw query string values use kebab case (e.g. exclude-files). To support this, a custom filter had to be used.
 * See {@link com.checkmarx.flow.filter.CaseTransformingFilter}.
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
    private List<String> state;
    private String assignee;
    private String preset;
    private Boolean incremental;
    private List<String> excludeFiles;
    private List<String> excludeFolders;
    private String override;
    private String scmInstance;

    // Bug tracker.
    private String bug;

    // trackApplicationOnly
    private Boolean appOnly;

    public ControllerRequest(List<String> severity,
                             List<String> cwe,
                             List<String> category,
                             List<String> status,
                             List<String> state) {
        this.severity = severity;
        this.cwe = cwe;
        this.category = category;
        this.status = status;
        this.state = state;
    }
}
