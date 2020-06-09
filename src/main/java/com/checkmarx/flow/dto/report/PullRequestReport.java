package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private OperationResult pullRequestResult;

    public PullRequestReport(ScanDetails scanDetails, ScanRequest request) {
        super(scanDetails.getScanId(), request);

        setEncryptedRepoUrl(request.getRepoUrl());
        if(scanDetails.isOsaScan()){
            scanId = scanDetails.getOsaScanId();
            scanInitiator = OSA;
        }
    }

    @Override
    protected String _getOperation() {
        return OPERATION;
    }
}
