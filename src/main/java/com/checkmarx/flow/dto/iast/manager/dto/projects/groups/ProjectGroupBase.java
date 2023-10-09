package com.checkmarx.flow.dto.iast.manager.dto.projects.groups;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProjectGroupBase {

    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$")
    @NotEmpty
    @Size(min = 1, max = 64)
    protected String projectGroupName;
}
