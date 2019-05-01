package com.checkmarx.flow.dto.cx;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "projectId",
        "presetId",
        "engineConfigurationId"
})

public class CxScanSettings {
    @JsonProperty("projectId")
    public Integer projectId;
    @JsonProperty("presetId")
    public Integer presetId;
    @JsonProperty("engineConfigurationId")
    public Integer engineConfigurationId;

    @java.beans.ConstructorProperties({"projectId", "presetId", "engineConfigurationId"})
    CxScanSettings(Integer projectId, Integer presetId, Integer engineConfigurationId) {
        this.projectId = projectId;
        this.presetId = presetId;
        this.engineConfigurationId = engineConfigurationId;
    }

    public static CxScanSettingsBuilder builder() {
        return new CxScanSettingsBuilder();
    }

    public Integer getProjectId() {
        return this.projectId;
    }

    public Integer getPresetId() {
        return this.presetId;
    }

    public Integer getEngineConfigurationId() {
        return this.engineConfigurationId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public void setPresetId(Integer presetId) {
        this.presetId = presetId;
    }

    public void setEngineConfigurationId(Integer engineConfigurationId) {
        this.engineConfigurationId = engineConfigurationId;
    }

    public String toString() {
        return "CxScanSettings(projectId=" + this.getProjectId() + ", presetId=" + this.getPresetId() + ", engineConfigurationId=" + this.getEngineConfigurationId() + ")";
    }

    public static class CxScanSettingsBuilder {
        private Integer projectId;
        private Integer presetId;
        private Integer engineConfigurationId;

        CxScanSettingsBuilder() {
        }

        public CxScanSettings.CxScanSettingsBuilder projectId(Integer projectId) {
            this.projectId = projectId;
            return this;
        }

        public CxScanSettings.CxScanSettingsBuilder presetId(Integer presetId) {
            this.presetId = presetId;
            return this;
        }

        public CxScanSettings.CxScanSettingsBuilder engineConfigurationId(Integer engineConfigurationId) {
            this.engineConfigurationId = engineConfigurationId;
            return this;
        }

        public CxScanSettings build() {
            return new CxScanSettings(projectId, presetId, engineConfigurationId);
        }

        public String toString() {
            return "CxScanSettings.CxScanSettingsBuilder(projectId=" + this.projectId + ", presetId=" + this.presetId + ", engineConfigurationId=" + this.engineConfigurationId + ")";
        }
    }
}