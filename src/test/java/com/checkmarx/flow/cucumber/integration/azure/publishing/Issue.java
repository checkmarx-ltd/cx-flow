package com.checkmarx.flow.cucumber.integration.azure.publishing;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Used for getting and creating ADO issues.
 */
@Builder
@Getter
@ToString
class Issue {
    private String id;
    private String title;
    private String description;
    private String state;
    private String projectName;
}
