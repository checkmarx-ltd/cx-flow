package com.checkmarx.flow.dto.cx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "name",
        "version",
        "highVulnerabilityCount",
        "mediumVulnerabilityCount",
        "lowVulnerabilityCount",
        "newestVersion",
        "newestVersionReleaseDate",
        "numberOfVersionsSinceLastUpdate",
        "confidenceLevel"
})
public class CxOsaLib {

    @JsonProperty("id")
    public String id;
    @JsonProperty("name")
    public String name;
    @JsonProperty("version")
    public String version;
    @JsonProperty("severity")
    public String severity;
    @JsonProperty("highVulnerabilityCount")
    public Integer highVulnerabilityCount;
    @JsonProperty("mediumVulnerabilityCount")
    public Integer mediumVulnerabilityCount;
    @JsonProperty("lowVulnerabilityCount")
    public Integer lowVulnerabilityCount;
    @JsonProperty("newestVersion")
    public String newestVersion;
    @JsonProperty("newestVersionReleaseDate")
    public String newestVersionReleaseDate;
    @JsonProperty("numberOfVersionsSinceLastUpdate")
    public String numberOfVersionsSinceLastUpdate;
    @JsonProperty("confidenceLevel")
    public String confidenceLevel;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CxOsaLib)) return false;
        return this.getId().equals(((CxOsaLib) o).getId());

    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getSeverity() {
        return this.severity;
    }

    public Integer getHighVulnerabilityCount() {
        return this.highVulnerabilityCount;
    }

    public Integer getMediumVulnerabilityCount() {
        return this.mediumVulnerabilityCount;
    }

    public Integer getLowVulnerabilityCount() {
        return this.lowVulnerabilityCount;
    }

    public String getNewestVersion() {
        return this.newestVersion;
    }

    public String getNewestVersionReleaseDate() {
        return this.newestVersionReleaseDate;
    }

    public String getNumberOfVersionsSinceLastUpdate() {
        return this.numberOfVersionsSinceLastUpdate;
    }

    public String getConfidenceLevel() {
        return this.confidenceLevel;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setHighVulnerabilityCount(Integer highVulnerabilityCount) {
        this.highVulnerabilityCount = highVulnerabilityCount;
    }

    public void setMediumVulnerabilityCount(Integer mediumVulnerabilityCount) {
        this.mediumVulnerabilityCount = mediumVulnerabilityCount;
    }

    public void setLowVulnerabilityCount(Integer lowVulnerabilityCount) {
        this.lowVulnerabilityCount = lowVulnerabilityCount;
    }

    public void setNewestVersion(String newestVersion) {
        this.newestVersion = newestVersion;
    }

    public void setNewestVersionReleaseDate(String newestVersionReleaseDate) {
        this.newestVersionReleaseDate = newestVersionReleaseDate;
    }

    public void setNumberOfVersionsSinceLastUpdate(String numberOfVersionsSinceLastUpdate) {
        this.numberOfVersionsSinceLastUpdate = numberOfVersionsSinceLastUpdate;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }
}
