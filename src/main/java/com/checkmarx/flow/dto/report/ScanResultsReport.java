package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.MergeResultEvaluatorImpl;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created after vulnerability scan has finished.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@EqualsAndHashCode(callSuper = true)
public class ScanResultsReport extends AnalyticsReport {

    public static final String OPERATION = "Scan Results";
    private Map<FindingSeverity, Integer> scanSummary = new EnumMap<>(FindingSeverity.class);
    private Map<FindingSeverity, Integer> cxFlowResults = new EnumMap<>(FindingSeverity.class);

    public ScanResultsReport(Integer sastScanId, ScanRequest request, ScanResults results) {
        super(sastScanId, request);
        setResults(results);
        setEncryptedRepoUrl(request.getRepoUrl());
    }

    public ScanResultsReport(String osaScanId, ScanRequest request, ScanResults results) {
        super(osaScanId, request);
        setResults(results);
        setEncryptedRepoUrl(request.getRepoUrl());
    }

    private void setResults(ScanResults results) {

        if(results.getScanSummary() !=null) {
            getSastResults(results);
        }
        else if(results.getScanSummary() !=null) {
            getSCAResults(results);
        }
    }

    private void getSCAResults(ScanResults results) {

        Map<Filter.Severity, Integer> findingsCount = results.getScaResults().getSummary().getFindingCounts();
        
        this.scanSummary.put(FindingSeverity.HIGH, findingsCount.get(Filter.Severity.HIGH));
        this.scanSummary.put(FindingSeverity.MEDIUM, findingsCount.get(Filter.Severity.HIGH));
        this.scanSummary.put(FindingSeverity.LOW, findingsCount.get(Filter.Severity.LOW));
        this.scanSummary.put(FindingSeverity.INFO, findingsCount.get(Filter.Severity.INFO));

        //TODO - the functionality of additionalDetails (results after filtering) is not developped in SCA yet
        //so we are not logging it
    }

    private void getSastResults(ScanResults results) {
        this.scanSummary.put(FindingSeverity.HIGH, results.getScanSummary().getHighSeverity());
        this.scanSummary.put(FindingSeverity.MEDIUM, results.getScanSummary().getMediumSeverity());
        this.scanSummary.put(FindingSeverity.LOW, results.getScanSummary().getLowSeverity());
        this.scanSummary.put(FindingSeverity.INFO, results.getScanSummary().getInfoSeverity());

        Map<FindingSeverity, Integer> cxFlowResultsIn = MergeResultEvaluatorImpl.getFindingCountPerSeverity(results);

        if (cxFlowResultsIn.get(FindingSeverity.HIGH) != null) {
            this.cxFlowResults.put(FindingSeverity.HIGH, cxFlowResultsIn.get(FindingSeverity.HIGH));
        }
        if (cxFlowResultsIn.get(FindingSeverity.MEDIUM) != null) {
            this.cxFlowResults.put(FindingSeverity.MEDIUM, cxFlowResultsIn.get(FindingSeverity.MEDIUM));
        }
        if (cxFlowResultsIn.get(FindingSeverity.LOW) != null) {
            this.cxFlowResults.put(FindingSeverity.LOW, cxFlowResultsIn.get(FindingSeverity.LOW));
        }
        if (cxFlowResultsIn.get(FindingSeverity.INFO) != null) {
            this.cxFlowResults.put(FindingSeverity.INFO, cxFlowResultsIn.get(FindingSeverity.INFO));
        }
    }

    @Override
    protected String _getOperation() {
        return OPERATION;
    }

}
