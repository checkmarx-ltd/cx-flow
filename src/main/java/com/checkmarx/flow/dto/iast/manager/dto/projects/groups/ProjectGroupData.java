package com.checkmarx.flow.dto.iast.manager.dto.projects.groups;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Yevgeny Kuznetsov
 * @since 3.4, 21 November 2019
 **/
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProjectGroupData extends ProjectGroupBase {
    private Long projectGroupId;

}
