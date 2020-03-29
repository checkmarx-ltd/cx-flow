package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.MergeResultEvaluatorImpl;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;


@Data
@EqualsAndHashCode(callSuper = true)
public class GetResultsReport extends AnalyticsReport {
    
    public static final String OPERATION = "Get Request";
    private Integer highSeveryResults = 0;
    private Integer mediumSeveryResults= 0;
    private Integer lowSeveryResults = 0;
    private Integer highSeveryCxFlowResults = 0;
    private Integer infoSeveryResults = 0;
    private Integer mediumSeveryCxFlowResults = 0;
    private Integer lowSeveryCxFlowResults = 0;
    private Integer infoSeveryCxFlowResults = 0;

    public GetResultsReport(Integer sastScanId, ScanRequest request, ScanResults results) {
        super(sastScanId,request);
        setResults(results);
        setEncodedRepoUrl(request.getRepoUrl());
    }

    public GetResultsReport(String osaScanId, ScanRequest request, ScanResults results) {
        super(osaScanId,request);
        setResults(results);
        setEncodedRepoUrl(request.getRepoUrl());
    }

    private void setResults(ScanResults results) {
        this.highSeveryResults = results.getScanSummary().getHighSeverity();
        this.mediumSeveryResults = results.getScanSummary().getHighSeverity();
        this.lowSeveryResults = results.getScanSummary().getLowSeverity();
        this.infoSeveryResults = results.getScanSummary().getInfoSeverity();
        
        Map<FindingSeverity, Integer> cxFlowResults = MergeResultEvaluatorImpl.getFindingCountPerSeverity(results);

        if(cxFlowResults.get(FindingSeverity.HIGH) !=null) {
            this.highSeveryCxFlowResults = cxFlowResults.get(FindingSeverity.HIGH);
        }
        if(cxFlowResults.get(FindingSeverity.MEDIUM) != null) {
            this.mediumSeveryCxFlowResults = cxFlowResults.get(FindingSeverity.MEDIUM);
        }
        if(cxFlowResults.get(FindingSeverity.LOW) != null) {
            this.lowSeveryCxFlowResults = cxFlowResults.get(FindingSeverity.LOW);
        }
        if(cxFlowResults.get(FindingSeverity.INFO) != null) {
            this.infoSeveryCxFlowResults = cxFlowResults.get(FindingSeverity.INFO);
        }
    }
    
    //adding underscore to prevent getOperation() to be called during logging of this object in log()
    //since we don't want the OPERATION to be a part of the logged object
    @Override
    public String _getOperation() {
        return OPERATION;
    }
    
}
