package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class GetResultsReport extends AnalyticsReport {

    private static final String OPERATION = "Get Request";

    public GetResultsReport(Integer sastScanId, ScanRequest request) {
        super(sastScanId,request);
    }

    public GetResultsReport(String osaSanId, ScanRequest request) {
        super(osaSanId,request);
    }

    //adding underscore to prevent getOperation() to be called during logging of this object in log()
    //since we don't want the OPERATION to be a part of the logged object
    @Override
    public String _getOperation() {
        return OPERATION;
    }
    
}
