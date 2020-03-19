package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Locale;
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
    public boolean isMergeAllowed(ScanResults scanResults, RepoProperties repoProperties, PullRequestReport pullRequestReport) {
        if (!repoProperties.isErrorMerge()) {
            return true;
        }
        
        Map<FindingSeverity, Integer> thresholds = getEffectiveThresholds();
        writeToLog(thresholds);
        pullRequestReport.setThresholds(thresholds);
        
        return !isAnyThresholdExceeded(scanResults, thresholds, pullRequestReport);
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

    private static boolean isAnyThresholdExceeded(ScanResults scanResults, Map<FindingSeverity, Integer> thresholds, PullRequestReport pullRequestReport) {
        boolean result = false;
        Iterable<Map.Entry<FindingSeverity, Integer>> findingsPerSeverity = getFindingCountPerSeverity(scanResults);
        
        pullRequestReport.setFindingsPerSeverity(findingsPerSeverity);
        for (Map.Entry<FindingSeverity, Integer> entry : findingsPerSeverity) {
            if (exceedsThreshold(entry, thresholds)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static Iterable<Map.Entry<FindingSeverity, Integer>> getFindingCountPerSeverity(ScanResults scanResults) {
        Map<FindingSeverity, Integer> result = new EnumMap<>(FindingSeverity.class);

        // Cannot use scanResults.getScanSummary(), because it doesn't take CxFlow filtering into account.
        if (scanResults != null && scanResults.getAdditionalDetails() != null) {
            Object rawSummary = scanResults.getAdditionalDetails().get(Constants.SUMMARY_KEY);
            if (rawSummary instanceof Map) {
                Map<?, ?> cxFlowSummary = (Map<?, ?>) rawSummary;
                for (Map.Entry<?, ?> entry : cxFlowSummary.entrySet()) {
                    setFindingCount(result, entry);
                }
            }
        }
        return result.entrySet();
    }

    private static void setFindingCount(Map<FindingSeverity, Integer> target, Map.Entry<?, ?> entry) {
        String rawSeverity = entry.getKey().toString().toUpperCase(Locale.ROOT);
        if (EnumUtils.isValidEnum(FindingSeverity.class, rawSeverity) &&
                entry.getValue() instanceof Integer) {
            FindingSeverity severity = FindingSeverity.valueOf(rawSeverity);
            Integer findingCount = (Integer) entry.getValue();
            target.put(severity, findingCount);
        }
    }

    private static boolean exceedsThreshold(Map.Entry<FindingSeverity, Integer> findingCountEntry,
                                            Map<FindingSeverity, Integer> thresholds) {
        boolean result = false;
        if (findingCountEntry != null && thresholds != null) {
            FindingSeverity severity = findingCountEntry.getKey();
            Integer threshold = thresholds.get(severity);
            Integer findingCount = findingCountEntry.getValue();
            result = threshold != null && findingCount != null && findingCount > threshold;
        }
        return result;
    }

    private static Map<FindingSeverity, Integer> failIfResultHasAnyFindings() {
        Map<FindingSeverity, Integer> result = new EnumMap<>(FindingSeverity.class);
        result.put(FindingSeverity.HIGH, 0);
        result.put(FindingSeverity.MEDIUM, 0);
        result.put(FindingSeverity.LOW, 0);
        return result;
    }
}
