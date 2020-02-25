package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Service
public class MergeResultEvaluatorImpl implements MergeResultEvaluator {

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
        return !isAnyThresholdExceeded(scanResults.getScanSummary(), thresholds);
    }

    private Map<FindingSeverity, Integer> getEffectiveThresholds() {
        if (thresholdsAreDefined()) {
            return flowProperties.getThresholds();
        } else {
            return failIfResultHasAnyFindings();
        }
    }

    private static boolean isAnyThresholdExceeded(CxScanSummary summary, Map<FindingSeverity, Integer> thresholds) {
        return summary != null &&
                (isExceeded(thresholds, FindingSeverity.HIGH, summary.getHighSeverity()) ||
                        isExceeded(thresholds, FindingSeverity.MEDIUM, summary.getMediumSeverity()) ||
                        isExceeded(thresholds, FindingSeverity.LOW, summary.getLowSeverity()));
    }

    private static boolean isExceeded(Map<FindingSeverity, Integer> thresholds,
                               FindingSeverity severityType,
                               Integer findingCount) {
        Integer threshold = thresholds.get(severityType);
        return threshold != null && findingCount != null && findingCount > threshold;
    }

    private boolean thresholdsAreDefined() {
        Map<FindingSeverity, Integer> thresholds = flowProperties.getThresholds();
        return thresholds != null
                && !thresholds.isEmpty()
                && thresholds.values().stream().anyMatch(Objects::nonNull);
    }

    private static Map<FindingSeverity, Integer> failIfResultHasAnyFindings() {
        Map<FindingSeverity, Integer> result = new EnumMap<>(FindingSeverity.class);
        result.put(FindingSeverity.HIGH, 0);
        result.put(FindingSeverity.MEDIUM, 0);
        result.put(FindingSeverity.LOW, 0);
        return result;
    }
}
