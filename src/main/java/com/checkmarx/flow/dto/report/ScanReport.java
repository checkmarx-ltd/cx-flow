package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.ScanRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Corresponds to an event when a vulnerability scan has been started.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ScanReport extends AnalyticsReport {

    public static final String OPERATION = "Scan Request";
    private static final String INCREMENTAL = "Inc";
    private static final String FULL = "Full";
    private String branch;
    private String repoType;
    protected String scanType;

    private OperationResult scanResult;

    public ScanReport(Integer sastScanId, ScanRequest request, String sourcesPath, OperationResult result) {
        super(sastScanId, request);
        setFields(request, sourcesPath, result);
    }

    public ScanReport(String osaScanId, ScanRequest request, String sourcesPath, OperationResult result) {
        super(osaScanId, request);
        setFields(request, sourcesPath, result);
    }

    private void setFields(ScanRequest request, String repoUrl, OperationResult result) {
        this.branch = request.getBranch();
        this.repoType = request.getRepoType().getRepository();
        if (branch == null) {
            branch = NOT_APPLICABLE;
        }
        setEncryptedRepoUrl(repoUrl);

        if (request.isIncremental()) {
            this.scanType = INCREMENTAL;
        } else {
            this.scanType = FULL;
        }
        this.scanResult = result;
    }

    @Override
    protected String _getOperation() {
        return OPERATION;
    }
}
