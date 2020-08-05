package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.cx.restclient.dto.scansummary.Severity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Corresponds to an event when a pull request is failed or approved depending on scan results.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PullRequestReport extends AnalyticsReport {

    public static final String OPERATION = "Pull Request";

    private Map<FindingSeverity, Integer> findingsPerSeverity = null;
    private Map<FindingSeverity, Integer> thresholds = null;
    private Map<Severity, Integer> scaThresholdsSeverity = null;
    private Map<Severity, Integer> scaFindingsSeverityCount = null;
    private Double scaThresholdsScore = null;

    private OperationResult pullRequestResult;
    private ScanRequest scanRequest;

    public PullRequestReport(ScanDetails scanDetails, ScanRequest request) {
        super(scanDetails.getScanId(), request);
        this.scanRequest = request;

        setEncryptedRepoUrl(request.getRepoUrl());
        if(scanDetails.isOsaScan()){
            scanId = scanDetails.getOsaScanId();
            scanInitiator = OSA;
        }
    }

    public PullRequestReport(String scanId, ScanRequest request, String scanInitiator) {
        this.scanId = scanId;
        this.scanInitiator = scanInitiator;
        if (scanId == null) {
            this.scanId = NOT_APPLICABLE;
        }
        this.projectName = request.getProject();
        setEncryptedRepoUrl(request.getRepoUrl());
    }

    public void setFindingsPerSeveritySca(ScanResults results) {
        Map<Filter.Severity, Integer> findingsMap = results.getScaResults().getSummary().getFindingCounts();
        Map<FindingSeverity, Integer> findingMapReport = new HashMap<FindingSeverity, Integer>();
        findingMapReport.put(FindingSeverity.HIGH, findingsMap.get(Filter.Severity.HIGH));
        findingMapReport.put(FindingSeverity.MEDIUM, findingsMap.get(Filter.Severity.MEDIUM));
        findingMapReport.put(FindingSeverity.LOW, findingsMap.get(Filter.Severity.LOW));
        findingMapReport.put(FindingSeverity.INFO, findingsMap.get(Filter.Severity.INFO));
        setFindingsPerSeverity(findingMapReport);
    }

    public ScanRequest getScanRequest() {
        return scanRequest;
    }

    @Override
    protected String _getOperation() {
        return OPERATION;
    }
}
