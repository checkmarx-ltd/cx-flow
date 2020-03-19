package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;

import java.util.*;

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
            log.info("Merge is allowed, because error-merge is set to false.");
            return true;
        }

        log.debug("Checking if pull request merge is allowed.");
        Map<FindingSeverity, Integer> thresholds = getEffectiveThresholds();
        writeMapToLog(thresholds, "Using thresholds");

        boolean isAllowed = !isAnyThresholdExceeded(scanResults, thresholds);
        log.info(isAllowed ? "Merge is allowed, because no thresholds were exceeded." :
                "Merge is not allowed, because some of the thresholds were exceeded.");

        return isAllowed;
    }

    private Map<FindingSeverity, Integer> getEffectiveThresholds() {
        if (thresholdsAreDefined()) {
            return flowProperties.getThresholds();
        } else {
            return failIfResultHasAnyFindings();
        }
    }

    private boolean thresholdsAreDefined() {
        Map<FindingSeverity, Integer> thresholds = flowProperties.getThresholds();
        return thresholds != null
                && !thresholds.isEmpty()
                && thresholds.values().stream().anyMatch(Objects::nonNull);
    }

    private static boolean isAnyThresholdExceeded(ScanResults scanResults, Map<FindingSeverity, Integer> thresholds) {
        boolean result = false;
        for (Map.Entry<FindingSeverity, Integer> entry : getFindingCountPerSeverity(scanResults)) {
            if (exceedsThreshold(entry, thresholds)) {
                result = true;
                // Don't break here, because we want to log validation for all the thresholds.
            }
        }
        return result;
    }

    private static Iterable<Map.Entry<FindingSeverity, Integer>> getFindingCountPerSeverity(ScanResults scanResults) {
        log.debug("Calculating finding counts per severity, after the filters were applied.");
        Map<FindingSeverity, Integer> result = new EnumMap<>(FindingSeverity.class);

        Map<?,?> cxFlowSummary = extractSummary(scanResults);
        if (cxFlowSummary != null) {
            for (Map.Entry<?, ?> entry : cxFlowSummary.entrySet()) {
                setFindingCount(result, entry);
            }
        }

        writeMapToLog(result, "Finding counts");
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
        boolean exceedsThreshold = false;
        if (findingCountEntry != null && thresholds != null) {
            FindingSeverity severity = findingCountEntry.getKey();
            Integer threshold = thresholds.get(severity);
            int findingCount = Optional.ofNullable(findingCountEntry.getValue()).orElse(0);

            exceedsThreshold = threshold != null && findingCount > threshold;
            logThresholdCheck(exceedsThreshold, severity, threshold, findingCount);
        }
        return exceedsThreshold;
    }

    private static Map<FindingSeverity, Integer> failIfResultHasAnyFindings() {
        Map<FindingSeverity, Integer> result = new EnumMap<>(FindingSeverity.class);
        result.put(FindingSeverity.HIGH, 0);
        result.put(FindingSeverity.MEDIUM, 0);
        result.put(FindingSeverity.LOW, 0);
        return result;
    }

    private static void writeMapToLog(Map<FindingSeverity, Integer> map, String message) {
        String json;
        try {
            json = jsonMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            json = "<error>";
            log.error("JSON serialization error.", e);
        }
        log.info("{}: {}", message, json);
    }

    private static void logThresholdCheck(
            boolean exceedsThreshold, FindingSeverity severity, Integer threshold, int findingCount) {

        if (threshold == null) {
            log.debug("Threshold for the {} severity is not defined, skipping.", severity.name());
        } else {
            String message;
            if (exceedsThreshold) {
                message = "Finding count ({}) is above the threshold ({}) for the {} severity.";
            } else {
                message = "Finding count ({}) does not exceed the threshold ({}) for the {} severity.";
            }
            log.debug(message, findingCount, threshold, severity.name());
        }
    }

    private static Map<?, ?> extractSummary(ScanResults scanResults) {
        Map<?, ?> result = null;

        // Cannot use scanResults.getScanSummary(), because it doesn't take CxFlow filtering into account.
        if (scanResults != null && scanResults.getAdditionalDetails() != null) {
            Object rawSummary = scanResults.getAdditionalDetails().get(Constants.SUMMARY_KEY);
            if (rawSummary instanceof Map) {
                result = (Map<?, ?>) rawSummary;
            } else {
                log.warn("Wrong summary type in scan results. Expected {}, got {}.",
                        Map.class.getName(), rawSummary.getClass().getName());
            }
        } else {
            log.warn("Additional details are missing in scan results.");
        }
        return result;
    }
}
