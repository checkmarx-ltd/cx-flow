package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import lombok.Data;


@Data
public class GetResultsReport extends Report {

    public static final String OPERATION = "Get Request";

    public GetResultsReport(Integer sastScanId, ScanRequest request) {
        super(sastScanId,request);
    }

    public GetResultsReport(String osaSanId, ScanRequest request) {
        super(osaSanId,request);
    }
    
    @Override
    public String _getOperation() {
        return OPERATION;
    }
    
}
