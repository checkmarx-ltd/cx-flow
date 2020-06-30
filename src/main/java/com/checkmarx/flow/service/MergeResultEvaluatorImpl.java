package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.OperationStatus;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.cx.restclient.dto.scansummary.Severity;
import com.cx.restclient.sca.dto.report.Finding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MergeResultEvaluatorImpl implements MergeResultEvaluator {
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private static final String MERGE_SUCCESS_DESCRIPTION = "Checkmarx Scan Completed";
    private static final String MERGE_FAILURE_DESCRIPTION = "Checkmarx Scan completed. Vulnerability scan failed";
    
    private final FlowProperties flowProperties;
    private final ScaProperties scaProperties;

    @Override
    public boolean isMergeAllowed(ScanResults scanResults, RepoProperties repoProperties, PullRequestReport pullRequestReport) {

        OperationResult requestResult = new OperationResult(OperationStatus.SUCCESS, MERGE_SUCCESS_DESCRIPTION);
        boolean isMergeAllowed = isAllowed(scanResults, repoProperties, pullRequestReport);
        
        if (!isMergeAllowed) {
            requestResult = new OperationResult(OperationStatus.FAILURE, MERGE_FAILURE_DESCRIPTION);
        }

        pullRequestReport.setPullRequestResult(requestResult);
        
        return isMergeAllowed;
    }
    
    private boolean isAllowed(ScanResults scanResults, RepoProperties repoProperties, PullRequestReport pullRequestReport) {
        if (!repoProperties.isErrorMerge()) {
            log.info("Merge is allowed, because error-merge is set to false.");
            return true;
        }

        boolean isAllowed = true;
        if (StringUtils.containsIgnoreCase(flowProperties.getEnabledVulnerabilityScanners().toString(), CxProperties.CONFIG_PREFIX)) {
            isAllowed = isAllowedSast(scanResults, pullRequestReport);
        }

        if (StringUtils.containsIgnoreCase(flowProperties.getEnabledVulnerabilityScanners().toString(), ScaProperties.CONFIG_PREFIX)) {
            isAllowed = isAllowed && isAllowedSca(scanResults, pullRequestReport);
        }
        return isAllowed;
    }

    private boolean isAllowedSca(ScanResults scanResults, PullRequestReport pullRequestReport) {
        log.debug("Checking if Cx-SCA pull request merge is allowed.");
        Map<Severity, Double> scaThresholds = getScaEffectiveThresholds();
        writeMapToLog(scaThresholds, "Using Cx-SCA thresholds");
        // TODO: 6/30/2020 ask Orly
//        pullRequestReport.setScaThresholdsMap(scaThresholds);
        boolean isAllowedSca = !isAnyScaThresholdsExceeded(scanResults, scaThresholds, pullRequestReport);
        isAllowedScannerToLog(isAllowedSca);

        return isAllowedSca;
    }

    private boolean isAllowedSast(ScanResults scanResults, PullRequestReport pullRequestReport) {
        log.debug("Checking if Cx-SAST pull request merge is allowed.");
        Map<FindingSeverity, Integer> thresholds = getSastEffectiveThresholds();
        writeMapToLog(thresholds, "Using Cx-SAST thresholds");
        pullRequestReport.setThresholds(thresholds);

        boolean isAllowed = !isAnySastThresholdExceeded(scanResults, thresholds, pullRequestReport);
        isAllowedScannerToLog(isAllowed);

        return isAllowed;
    }

    private void isAllowedScannerToLog(boolean isAllowedScanner) {
        log.info(isAllowedScanner ? "Merge is allowed, because no thresholds were exceeded." :
                "Merge is not allowed, because some of the thresholds were exceeded.");
    }

    private Map<FindingSeverity, Integer> getSastEffectiveThresholds() {
        if (areSastThresholdsDefined()) {
            return flowProperties.getThresholds();
        } else {
            return failSastPrIfResultHasAnyFindings();
        }
    }

    private Map<Severity, Double> getScaEffectiveThresholds() {
        return (areScaThresholdsDefined())
                ? scaProperties.getThresholds()
                : failScaPrIfResultHasAnyFindings();
    }

    private boolean areSastThresholdsDefined() {
        Map<FindingSeverity, Integer> thresholds = flowProperties.getThresholds();
        return thresholds != null
                && !thresholds.isEmpty()
                && thresholds.values().stream().anyMatch(Objects::nonNull);
    }

    private boolean areScaThresholdsDefined() {
        Map<Severity, Double> thresholds = scaProperties.getThresholds();
        return thresholds != null
                && !thresholds.isEmpty()
                && thresholds.values().stream().anyMatch(Objects::nonNull);
    }

    private static boolean isAnyScaThresholdsExceeded(ScanResults scanResults,  Map<Severity, Double> scaThresholds, PullRequestReport pullRequestReport) {
        boolean notExceeding = true;

        Map<Severity, Double> scaFindingsMaxPerSeverity = getScaFindingsMaxPerSeverity(scanResults);
        // TODO: 6/30/2020 ask Orly
//        pullRequestReport.setMaxFindingsPerSeverity(scaFindingsMaxPerSeverity);

        for (Severity severity : scaFindingsMaxPerSeverity.keySet()) {
            notExceeding &= isExceedsScaThreshold(scaThresholds, scaFindingsMaxPerSeverity, severity);
            // enable fallthru for logging and authoring
        }
        return !notExceeding;
    }

    private static boolean isExceedsScaThreshold(Map<Severity, Double> scaThresholds, Map<Severity, Double> scaFindingsMaxPerSeverity, Severity severity) {
        Double findingsMaxSeverity = scaFindingsMaxPerSeverity.get(severity);
        Double thresholdScore = scaThresholds.get(severity);
        boolean exceedsThreshold = findingsMaxSeverity <= thresholdScore;

        logScaThresholdCheck(exceedsThreshold, severity, thresholdScore, findingsMaxSeverity);

        return exceedsThreshold;
    }

    private static boolean isAnySastThresholdExceeded(ScanResults scanResults, Map<FindingSeverity, Integer> sastThresholds, PullRequestReport pullRequestReport) {
        boolean result = false;
        Map<FindingSeverity, Integer> findingsPerSeverity = getSastFindingCountPerSeverity(scanResults);
        pullRequestReport.setFindingsPerSeverity(findingsPerSeverity);
        for (Map.Entry<FindingSeverity, Integer> entry : findingsPerSeverity.entrySet()) {
            if (isExceedsSastThreshold(entry, sastThresholds)) {
                result = true;
                // Don't break here, because we want to log validation for all the thresholds.
            }
        }
        return result;
    }

    private static Map<Severity, Double> getScaFindingsMaxPerSeverity(ScanResults scanResults) {
        log.debug("Calculating Cx-SCA finding MAX per severity, after the filters were applied.");

        EnumMap<Severity, Double> maxSeverityMap = new EnumMap<>(Severity.class);
        scanResults.getScaResults().getFindings().stream()
                .collect(Collectors.groupingBy(Finding::getSeverity))
                .forEach((k, v) ->
                    maxSeverityMap.put(k, Collections.max(v, Comparator.comparingDouble(Finding::getScore)).getScore()));
        return maxSeverityMap;
    }

    public static Map<FindingSeverity, Integer> getSastFindingCountPerSeverity(ScanResults scanResults) {
        log.debug("Calculating Cx-SAST finding counts per severity, after the filters were applied.");
        Map<FindingSeverity, Integer> result = new EnumMap<>(FindingSeverity.class);

        Map<?,?> cxFlowSummary = extractSummary(scanResults);
        if (cxFlowSummary != null) {
            for (Map.Entry<?, ?> entry : cxFlowSummary.entrySet()) {
                setFindingCount(result, entry);
            }
        }

        writeMapToLog(result, "Finding counts");
        return result;
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

    private static boolean isExceedsSastThreshold(Map.Entry<FindingSeverity, Integer> findingCountEntry,
                                                  Map<FindingSeverity, Integer> thresholds) {
        boolean exceedsThreshold = false;
        if (findingCountEntry != null && thresholds != null) {
            FindingSeverity severity = findingCountEntry.getKey();
            Integer threshold = thresholds.get(severity);
            int findingCount = Optional.ofNullable(findingCountEntry.getValue()).orElse(0);

            exceedsThreshold = threshold != null && findingCount > threshold;
            logSastThresholdCheck(exceedsThreshold, severity, threshold, findingCount);
        }
        return exceedsThreshold;
    }

    private static Map<FindingSeverity, Integer> failSastPrIfResultHasAnyFindings() {
        Map<FindingSeverity, Integer> result = new EnumMap<>(FindingSeverity.class);
        result.put(FindingSeverity.HIGH, 0);
        result.put(FindingSeverity.MEDIUM, 0);
        result.put(FindingSeverity.LOW, 0);
        return result;
    }

    private static Map<Severity, Double> failScaPrIfResultHasAnyFindings() {
        EnumMap<Severity, Double> result = new EnumMap<>(Severity.class);
        for (Severity any : Severity.values()) {
            result.put(any, 0.0);
        }
        return result;
    }

    private static void writeMapToLog(Object map, String message) {
        String json;
        try {
            json = jsonMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            json = "<error>";
            log.error("JSON serialization error.", e);
        }
        log.info("{}: {}", message, json);
    }

    private static void logScaThresholdCheck(boolean exceedsThreshold, Severity severity,
                                             Double threshold, Double findingsMaxSeverity) {
        if (threshold == null) {
            log.info("Threshold for the {} severity is not defined, skipping.", severity.name());
        } else {
            String message;
            if (exceedsThreshold) {
                message = "Findings max severity score ({}) is above the threshold ({}) for the {} severity.";
            } else {
                message = "Finding max severity score ({}) does not exceed the threshold ({}) for the {} severity.";
            }
            log.info(message, findingsMaxSeverity, threshold, severity.name());
        }
    }

    private static void logSastThresholdCheck(
            boolean exceedsThreshold, FindingSeverity severity, Integer threshold, int findingCount) {

        if (threshold == null) {
            log.info("Threshold for the {} severity is not defined, skipping.", severity.name());
        } else {
            String message;
            if (exceedsThreshold) {
                message = "Finding count ({}) is above the threshold ({}) for the {} severity.";
            } else {
                message = "Finding count ({}) does not exceed the threshold ({}) for the {} severity.";
            }
            log.info(message, findingCount, threshold, severity.name());
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
                String summaryClass = rawSummary != null ? rawSummary.getClass().getName() : null;
                log.warn("Wrong summary type in scan results. Expected {}, got {}.",
                        Map.class.getName(), summaryClass);
            }
        } else {
            log.warn("Additional details are missing in scan results.");
        }
        return result;
    }
}
