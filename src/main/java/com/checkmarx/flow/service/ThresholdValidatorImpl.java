package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.OperationStatus;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.ScaConfig;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.Sca;
import com.checkmarx.sdk.dto.ScanResults;
import com.cx.restclient.dto.scansummary.Severity;
import com.cx.restclient.ast.dto.sca.report.Finding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
        boolean isMergeAllowed = true;

        if (!repoProperties.isErrorMerge()) {
            log.info("Merge is allowed, because error-merge is set to false.");
            pullRequestReport.setPullRequestResult(requestResult);
        }
        else{
            isMergeAllowed = isAllowed(scanResults, pullRequestReport.getScanRequest());

            if (!isMergeAllowed) {
                log.info("Merge is not allowed, because some thresholds were exceeded.");
                requestResult = new OperationResult(OperationStatus.FAILURE, MERGE_FAILURE_DESCRIPTION);
            }
        }

        pullRequestReport.setPullRequestResult(requestResult);

        return isMergeAllowed;
    }

    @Override
    public boolean thresholdsExceeded(ScanRequest request, ScanResults results){
        return !isAllowed(results, request);
    }

    @Override
    public boolean isThresholdsConfigurationExist(ScanRequest scanRequest){
        boolean sastThresholds = false;
        boolean scaThresholds = false;

        sastThresholds = scanRequest.getThresholds() != null || flowProperties.getThresholds() != null;
        if(scaProperties != null){
            scaThresholds = scaProperties.getThresholdsSeverity() != null || scaProperties.getThresholdsScore() != null;
        }

        if (!scaThresholds && scanRequest.getScaConfig() != null) {
            scaThresholds = scanRequest.getScaConfig().getThresholdsSeverity() != null || scanRequest.getScaConfig().getThresholdsScore() != null;
        }

        return  sastThresholds || scaThresholds;
    }


    private boolean isAllowed(ScanResults scanResults, ScanRequest request) {

        boolean isAllowed = true;
        if (isSast()) {
            isAllowed = isAllowedSast(scanResults, request);
        }

        if (isAllowed && scaScanner.isEnabled()) {
            isAllowed = isAllowedSca(scanResults, request);
        }

        return isAllowed;
    }

    private boolean isSast() {
        return sastScanner.isEnabled();
    }

    private boolean isAllowedSca(ScanResults scanResults, ScanRequest request) {
        log.debug("Checking if CxSCA pull request merge is allowed.");
        Map<Severity, Integer> scaThresholdsSeverity = getScaEffectiveThresholdsSeverity(request);
        Double scaThresholdsScore = getScaEffectiveThresholdsScore(request);

        writeMapToLog(scaThresholdsSeverity, "Using CxSCA thresholds severity");
        writeMapToLog(scaThresholdsScore, "Using CxSCA thresholds score");

        boolean isAllowedSca = !isAnyScaThresholdsExceeded(scanResults, scaThresholdsSeverity, scaThresholdsScore);
        isAllowedScannerToLog(isAllowedSca);

        return isAllowedSca;
    }

    private boolean isAllowedSast(ScanResults scanResults, ScanRequest request) {
        log.debug("Checking if CxSAST pull request merge is allowed.");
        Map<FindingSeverity, Integer> thresholds = getSastEffectiveThresholds(request);
        writeMapToLog(thresholds, "Using CxSAST thresholds");

        boolean isAllowed = !isAnySastThresholdExceeded(scanResults, thresholds);
        isAllowedScannerToLog(isAllowed);

        return isAllowed;
    }

    private void isAllowedScannerToLog(boolean isAllowedScanner) {
        log.info(isAllowedScanner ? "No thresholds were exceeded." :
                "Thresholds were exceeded.");
    }

    private Map<FindingSeverity, Integer> getSastEffectiveThresholds(ScanRequest scanRequest) {
        Map<FindingSeverity, Integer> res;

        if (areSastThresholdsFromRequestDefined(scanRequest)) {
            res = scanRequest.getThresholds();
        } else if (areSastThresholdsDefined()) {
            res = flowProperties.getThresholds();
        } else {
            res = failSastPrIfResultHasAnyFindings();
        }

        return res;
    }

    private boolean areSastThresholdsFromRequestDefined(ScanRequest scanRequest) {
        return Optional.ofNullable(scanRequest)
                .map(ScanRequest::getThresholds)
                .map(map -> !map.isEmpty())
                .orElse(false);
    }

    private Map<Severity, Integer> getScaEffectiveThresholdsSeverity(ScanRequest scanRequest) {
        Map<Severity, Integer> res;

        if (areScaThresholdsSeverityFromRequestDefined(scanRequest)) {
            res = scanRequest.getScaConfig().getThresholdsSeverity();
        } else if(areScaThresholdsSeverityDefined(scanRequest)) {
            res = scaProperties.getThresholdsSeverity();
        } else {
            res = isScaThresholdsScoreDefined(scanRequest)
                    ? passScaPrForAnyFindings()
                    : failScaPrIfResultHasAnyFindings();
        }
        return res;
    }

    private boolean areScaThresholdsSeverityFromRequestDefined(ScanRequest scanRequest) {
        return Optional.ofNullable(scanRequest)
                .map(ScanRequest::getScaConfig)
                .map(ScaConfig::getThresholdsSeverity)
                .map(map -> !map.isEmpty())
                .orElse(false);
    }

    private Double getScaEffectiveThresholdsScore(ScanRequest scanRequest) {
        Double res;

        if (areScaThresholdScoreFromRequestDefined(scanRequest)) {
            res = scanRequest.getScaConfig().getThresholdsScore();
        } else if (isScaThresholdsScoreDefined(scanRequest)) {
            res = scaProperties.getThresholdsScore();
        } else {
            res = areScaThresholdsSeverityDefined(scanRequest)
                    ? 10.0
                    : 0.0;
        }

        return res;
    }

    private boolean areScaThresholdScoreFromRequestDefined(ScanRequest scanRequest) {
        return Optional.ofNullable(scanRequest)
                .map(ScanRequest::getScaConfig)
                .map(ScaConfig::getThresholdsScore).isPresent();
    }

    private boolean areSastThresholdsDefined() {
        Map<FindingSeverity, Integer> thresholds = flowProperties.getThresholds();
        return thresholds != null
                && !thresholds.isEmpty()
                && thresholds.values().stream().anyMatch(Objects::nonNull);
    }

    private boolean isScaThresholdsScoreDefined(ScanRequest scanRequest) {
        return Optional.ofNullable(scanRequest)
                .map(ScanRequest::getScaConfig)
                .map(ScaConfig::getThresholdsScore)
                .map(any -> true)
                .orElse(scaProperties.getThresholdsScore() != null);
    }

    private boolean areScaThresholdsSeverityDefined(ScanRequest scanRequest) {
        return Optional.ofNullable(scanRequest)
                .map(ScanRequest::getScaConfig)
                .map(ScaConfig::getThresholdsSeverity)
                .map(map -> !map.isEmpty())
                .orElse(areScaThresholdsSeverityDefinedFromProperties());
    }

    private boolean areScaThresholdsSeverityDefinedFromProperties() {
        Map<Severity, Integer> thresholdsSeverity = scaProperties.getThresholdsSeverity();
        return thresholdsSeverity != null
                && !thresholdsSeverity.isEmpty()
                && thresholdsSeverity.values().stream().anyMatch(Objects::nonNull);
    }

    private static boolean isAnyScaThresholdsExceeded(ScanResults scanResults,  Map<Severity, Integer> scaThresholds,
                                                      Double scaThresholdsScore) {
        boolean isExceeded = isExceedsScaThresholdsScore(scanResults, scaThresholdsScore);

        Map<Severity, Integer> scaFindingsCountsPerSeverity = getScaFindingsCountsPerSeverity(scanResults);

        for (Map.Entry<Severity, Integer> entry : scaFindingsCountsPerSeverity.entrySet()) {
            Severity severity = entry.getKey();
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

    private static boolean isAnySastThresholdExceeded(ScanResults scanResults, Map<FindingSeverity, Integer> sastThresholds) {
        boolean result = false;
        Map<FindingSeverity, Integer> findingsPerSeverity = getSastFindingCountPerSeverity(scanResults);

        for (Map.Entry<FindingSeverity, Integer> entry : findingsPerSeverity.entrySet()) {
            if (isExceedsSastThreshold(entry, sastThresholds)) {
                result = true;
                // Don't break here, because we want to log validation for all the thresholds.
            }
        }
        return result;
    }

    private static Map<Severity, Integer> getScaFindingsCountsPerSeverity(ScanResults scanResults) {
        log.debug("Calculating CxSCA finding counts per severity, after the filters were applied.");

        EnumMap<Severity, Integer> countsSeverityMap = new EnumMap<>(Severity.class);
        scanResults.getScaResults().getFindings().stream()
                .collect(Collectors.groupingBy(Finding::getSeverity))
                .forEach((k, v) ->
                        countsSeverityMap.put(k, v.size()));
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

    private static void logScaThresholdExceedsCounts(boolean exceedsThreshold, Severity severity,
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
