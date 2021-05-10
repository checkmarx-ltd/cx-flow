package com.checkmarx.flow.dto.iast.manager.dto;


import com.checkmarx.flow.dto.iast.manager.dal.entity.InstantAttributeConverter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Transient;
import java.time.Instant;

@Slf4j
@Data
@EqualsAndHashCode(exclude = {"startTime", "finishTime"})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties({"latestActivityTime"})
public class Scan {

    @Transient
    private static final InstantAttributeConverter converter = new InstantAttributeConverter();

    public static final int RUNNING_SCANS_AGGREGATION_SCAN_ID = 0;
    public static final String PROJECT_ID_DESCRIPTION = "The id of the scans' project";
    private static final String TIME_EXAMPLE = "1577836800000"; // Jan 1, 2020 00:00:00+0

    private Long projectId;

    private String projectName;

    private Long scanId;

    private Long aggregationId;

    // Don't know why it's ignored 🙈 🤷 ¯\_(ツ)_/¯
    @JsonIgnore
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Instant lastRequestTime;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Instant startTime;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Instant finishTime;

    private int high;
    private int medium;
    private int low;
    private int info;
    private int newHigh;
    private int newMedium;
    private int newLow;

    @Getter(AccessLevel.NONE)
    private Integer riskScore;

    private Integer uniqueRequests;

    private Integer state;

    private Integer aggregatedScansCount;

    private double coverage;

    private String tag;

    private Double apiCoverage;

    // computed risk score getter
    public Integer getRiskScore() {
        return Math.min(100, high * 8 + medium * 4 + low);
    }

    private int extractIntValue(Number num) {
        return num != null ? num.intValue() : 0;
    }

    private double extractDoubleValue(Number num) {
        return num != null ? num.doubleValue() : 0;
    }


    public void setRiskScore(Number riskScore) {
        this.riskScore = riskScore.intValue();
    }
}
