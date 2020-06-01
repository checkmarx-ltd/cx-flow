package com.checkmarx.flow.cucumber.integration.sast.scan;

public class ScanDTO {

    private final String teamId;

    public Integer getScanId() {
        return scanId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    private final Integer scanId;

    public String getTeamId() {
        return teamId;
    }

    private final Integer projectId;

    public ScanDTO(Integer projectId, Integer scanId, String teamId) {
        this.projectId = projectId;
        this.scanId = scanId;
        this.teamId = teamId;
    }
}
