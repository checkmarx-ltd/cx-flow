package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.OperationStatus;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;



import com.cx.restclient.dto.scansummary.Severity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ThresholdValidatorImpl implements ThresholdValidator {
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private static final String MERGE_SUCCESS_DESCRIPTION = "Checkmarx Scan Completed";
    private static final String MERGE_FAILURE_DESCRIPTION = "Checkmarx Scan completed. Vulnerability scan failed";
    
    private final FlowProperties flowProperties;
    private final ScaProperties scaProperties;
    private final SastScanner sastScanner;
    private final SCAScanner scaScanner;

    @Autowired
    public ThresholdValidatorImpl(@Lazy SastScanner sastScanner, @Lazy SCAScanner scaScanner,
                                  FlowProperties flowProperties, ScaProperties scaProperties) {
        this.sastScanner = sastScanner;
        this.scaScanner = scaScanner;
        this.flowProperties = flowProperties;
        this.scaProperties = scaProperties;
    }

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
        if (isSast()) {
            isAllowed = isAllowedSast(scanResults, pullRequestReport);
        }

        if (isAllowed && scaScanner.isEnabled()) {
            isAllowed = isAllowedSca(scanResults, pullRequestReport);
        }

        return isAllowed;
    }

    private boolean isSast() {
        return sastScanner.isEnabled();
    }

    private boolean isAllowedSca(ScanResults scanResults, PullRequestReport pullRequestReport) {
        log.debug("Checking if Cx-SCA pull request merge is allowed.");
        Map<Severity, Integer> scaThresholdsSeverity = getScaEffectiveThresholdsSeverity();
        Double scaThresholdsScore = getScaEffectiveThresholdsScore();

        writeMapToLog(scaThresholdsSeverity, "Using Cx-SCA thresholds severity");
        writeMapToLog(scaThresholdsScore, "Using Cx-SCA thresholds score");
        pullRequestReport.setScaThresholdsSeverity(scaThresholdsSeverity);
        pullRequestReport.setScaThresholdsScore(scaThresholdsScore);

        boolean isAllowedSca = !isAnyScaThresholdsExceeded(scanResults, scaThresholdsSeverity, scaThresholdsScore, pullRequestReport);
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

    private Map<Severity, Integer> getScaEffectiveThresholdsSeverity() {
        Map<Severity, Integer> defult = isScaThresholdsScoreDefined()
        ? passScaPrForAnyFindings()
        : failScaPrIfResultHasAnyFindings();
        return (areScaThresholdsSeverityDefined())
                ? scaProperties.getThresholdsSeverity()
                : defult;
    }

    private Double getScaEffectiveThresholdsScore() {
        double defult = areScaThresholdsSeverityDefined()
        ? 10.0
        : 0.0;
        return (isScaThresholdsScoreDefined())
                ? scaProperties.getThresholdsScore()
                : defult;
    }

    private boolean areSastThresholdsDefined() {
        Map<FindingSeverity, Integer> thresholds = flowProperties.getThresholds();
        return thresholds != null
                && !thresholds.isEmpty()
                && thresholds.values().stream().anyMatch(Objects::nonNull);
    }

    private boolean isScaThresholdsScoreDefined() {
        return scaProperties.getThresholdsScore() != null;
    }

    private boolean areScaThresholdsSeverityDefined() {
        Map<Severity, Integer> thresholdsSeverity = scaProperties.getThresholdsSeverity();
        return thresholdsSeverity != null
                && !thresholdsSeverity.isEmpty()
                && thresholdsSeverity.values().stream().anyMatch(Objects::nonNull);
    }

    private static boolean isAnyScaThresholdsExceeded(ScanResults scanResults,  Map<Severity, Integer> scaThresholds,
                                                      Double scaThresholdsScore, PullRequestReport pullRequestReport) {
        boolean isExceeded = isExceedsScaThresholdsScore(scanResults, scaThresholdsScore);

        EnumMap<Filter.Severity, Integer> scaFindingsCountsPerSeverity = getScaFindingsCountsPerSeverity(scanResults);
        pullRequestReport.setScaFindingsSeverityCount(scaFindingsCountsPerSeverity);

        for (Map.Entry<Filter.Severity, Integer> entry : scaFindingsCountsPerSeverity.entrySet()) {
            Filter.Severity severity = entry.getKey();
            Integer thresholdCount = scaThresholds.get(severity);
            if (thresholdCount == null) {
                continue;
            }
            Integer findingsCount = entry.getValue();
            if (findingsCount > thresholdCount) {
                isExceeded = true;
                logScaThresholdExceedsCounts(true, severity, thresholdCount, findingsCount);
                // Don't break here, because we want to log validation for all the thresholds.
            } else {
                logScaThresholdExceedsCounts(false, severity, thresholdCount, findingsCount);
            }
        }

        return isExceeded;
    }


    private static boolean isExceedsScaThresholdsScore(ScanResults scanResults, Double scaThresholdsScore) {
        double summaryRiskScore = scanResults.getScaResults().getSummary().getRiskScore();

        boolean isExceeded = scaThresholdsScore != null && (summaryRiskScore > scaThresholdsScore);
        logScaThresholdsScoreCheck(isExceeded, scaThresholdsScore, summaryRiskScore);
        return isExceeded;
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

    private static EnumMap<Filter.Severity, Integer> getScaFindingsCountsPerSeverity(ScanResults scanResults) {
        log.debug("Calculating CxSCA finding counts per severity, after the filters were applied.");

        EnumMap<Filter.Severity, Integer> countsSeverityMap = new EnumMap<com.checkmarx.sdk.dto.Filter.Severity, Integer>(com.checkmarx.sdk.dto.Filter.Severity.class);
        List<com.checkmarx.sdk.dto.ast.Finding> findings = scanResults.getScaResults().getFindings();

        for (com.checkmarx.sdk.dto.ast.Finding finding : findings) {
            Filter.Severity severity = finding.getSeverity();
            if(countsSeverityMap.containsKey(severity)){
                Integer severityCount = countsSeverityMap.get(severity);
                countsSeverityMap.put(severity, ++severityCount);
            }else{
                countsSeverityMap.put(severity, 1);
            }
        }
        return countsSeverityMap;
    }

    public static Map<FindingSeverity, Integer> getSastFindingCountPerSeverity(ScanResults scanResults) {
        log.debug("Calculating CxSAST finding counts per severity, after the filters were applied.");
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

    private static Map<Severity, Integer> failScaPrIfResultHasAnyFindings() {
        EnumMap<Severity, Integer> result = new EnumMap<>(Severity.class);
        for (Severity any : Severity.values()) {
            result.put(any, 0);
        }
        return result;
    }

    private static Map<Severity, Integer> passScaPrForAnyFindings() {
        EnumMap<Severity, Integer> result = new EnumMap<>(Severity.class);
        for (Severity any : Severity.values()) {
            result.put(any, Integer.MAX_VALUE);
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

    private static void logScaThresholdsScoreCheck(boolean exceedsThreshold, Double thresholdsScore,
                                                   Double scaSummaryScore) {
        if (thresholdsScore == null) {
            log.info("SCA thresholds score is not defined, skipping.");
        } else {
            String message;
            if (exceedsThreshold) {
                message = "CxSCA findings summary's score: ({}) is exceeded the thresholds score defined: ({})";
            } else {
                message = "CxSCA findings summary's score: ({}) doesn't exceeded the thresholds score defined: ({})";
            }
            log.info(message, scaSummaryScore, thresholdsScore);
        }
    }

    private static void logScaThresholdExceedsCounts(boolean exceedsThreshold, Filter.Severity severity,
                                                     Integer thresholdCount, Integer findingsCount) {
        String message;
        if (exceedsThreshold) {
            message = "CxSCA findings count: ({}) is exceeded the thresholds's severity counts: ({}) for severity: ({})";
        } else {
            message = "CxSCA findings count: ({}) doesn't exceeded the thresholds's severity counts: ({}) for severity: ({})";
        }
        log.info(message, findingsCount, thresholdCount, severity);
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
