package com.checkmarx.flow.dto.cx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "highSeverity",
        "mediumSeverity",
        "lowSeverity",
        "infoSeverity",
        "statisticsCalculationDate"
})
public class CxScanSummary {

    @JsonProperty("highSeverity")
    private Integer highSeverity;
    @JsonProperty("mediumSeverity")
    private Integer mediumSeverity;
    @JsonProperty("lowSeverity")
    private Integer lowSeverity;
    @JsonProperty("infoSeverity")
    private Integer infoSeverity;
    @JsonProperty("statisticsCalculationDate")
    private String statisticsCalculationDate;

    public CxScanSummary() { }

    public Integer getHighSeverity() {
        return highSeverity;
    }

    public void setHighSeverity(Integer highSeverity) {
        this.highSeverity = highSeverity;
    }

    public Integer getMediumSeverity() {
        return mediumSeverity;
    }

    public void setMediumSeverity(Integer mediumSeverity) {
        this.mediumSeverity = mediumSeverity;
    }

    public Integer getLowSeverity() {
        return lowSeverity;
    }

    public void setLowSeverity(Integer lowSeverity) {
        this.lowSeverity = lowSeverity;
    }

    public Integer getInfoSeverity() {
        return infoSeverity;
    }

    public void setInfoSeverity(Integer infoSeverity) {
        this.infoSeverity = infoSeverity;
    }

    public String getStatisticsCalculationDate() {
        return statisticsCalculationDate;
    }

    public void setStatisticsCalculationDate(String statisticsCalculationDate) {
        this.statisticsCalculationDate = statisticsCalculationDate;
    }

    @Override
    public String toString() {
        return String.format("high: %s, medium: %s, low: %s, info: %s", highSeverity, mediumSeverity, lowSeverity, infoSeverity);
    }
}