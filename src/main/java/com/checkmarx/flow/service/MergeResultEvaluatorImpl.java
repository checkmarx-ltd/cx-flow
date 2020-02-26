package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class MergeResultEvaluatorImpl implements MergeResultEvaluator {
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private final FlowProperties flowProperties;

    public MergeResultEvaluatorImpl(FlowProperties flowProperties) {
        this.flowProperties = flowProperties;
    }

    @Override
    public boolean isMergeAllowed(ScanResults scanResults, RepoProperties repoProperties) {
        if (!repoProperties.isErrorMerge()) {
            return true;
        }

        Map<FindingSeverity, Integer> thresholds = getEffectiveThresholds();
        writeToLog(thresholds);

        return !isAnyThresholdExceeded(scanResults.getScanSummary(), thresholds);
    }

    private Map<FindingSeverity, Integer> getEffectiveThresholds() {
        if (thresholdsAreDefined()) {
            return flowProperties.getThresholds();
        } else {
            return failIfResultHasAnyFindings();
        }
    }

    private static void writeToLog(Map<FindingSeverity, Integer> thresholds) {
        try {
            String json = jsonMapper.writeValueAsString(thresholds);
            log.info("Using thresholds: {}", json);
        } catch (JsonProcessingException e) {
            log.error("Error serializing thresholds.", e);
        }
    }

    private boolean thresholdsAreDefined() {
        Map<FindingSeverity, Integer> thresholds = flowProperties.getThresholds();
        return thresholds != null
                && !thresholds.isEmpty()
                && thresholds.values().stream().anyMatch(Objects::nonNull);
    }

    private static boolean isAnyThresholdExceeded(CxScanSummary summary, Map<FindingSeverity, Integer> thresholds) {
        return summary != null &&
                (isExceeded(thresholds, FindingSeverity.HIGH, summary.getHighSeverity()) ||
                        isExceeded(thresholds, FindingSeverity.MEDIUM, summary.getMediumSeverity()) ||
                        isExceeded(thresholds, FindingSeverity.LOW, summary.getLowSeverity()));
    }

    private static boolean isExceeded(Map<FindingSeverity, Integer> thresholds,
                                      FindingSeverity severityLevel,
                                      Integer findingCount) {
        Integer threshold = thresholds.get(severityLevel);
        return threshold != null && findingCount != null && findingCount > threshold;
    }

    private static Map<FindingSeverity, Integer> failIfResultHasAnyFindings() {
        Map<FindingSeverity, Integer> result = new EnumMap<>(FindingSeverity.class);
        result.put(FindingSeverity.HIGH, 0);
        result.put(FindingSeverity.MEDIUM, 0);
        result.put(FindingSeverity.LOW, 0);
        return result;
    }
}
