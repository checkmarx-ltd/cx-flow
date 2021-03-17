package com.checkmarx.flow.dto.iast.manager.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class RunningScanAggregation extends Scan {
    /**
     * count of running scans for the project (multiple agents)
     */
    private int aggregationCount;


    public RunningScanAggregation(Long projectId, String projectName) {
        setProjectId(projectId);
        setProjectName(projectName);
    }

    @Override
    public Integer getState() {
        return ScanState.STATE_RUNNING.getStateNum();
    }
}
