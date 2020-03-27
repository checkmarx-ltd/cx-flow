package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Status;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScanReport extends AnalyticsReport {

    private static final String OPERATION = "Scan Request";
    private static final String INCREMENTAL = "Inc";
    private static final String FULL = "Full";
    private String scanStatus;
    private String branch;
    private String repoType;
    protected String scanType;

    public ScanReport(Integer sastScanId, ScanRequest request, String sourcesPath, Status status) {
        super(sastScanId,request);
        setFields(request, sourcesPath, status);
    }

    public ScanReport(String osaScanId, ScanRequest request, String sourcesPath, Status status) {
        super(osaScanId,request);
        setFields(request, sourcesPath, status);
    }
    
    private void setFields(ScanRequest request, String sourcesPath, Status status) {
        this.branch = request.getBranch();
        this.repoType = request.getRepoType().getRepository();
        this.scanStatus = setEncodedRepoUrl(sourcesPath, status.getMessage());

        if(request.isIncremental()){
            this.scanType = INCREMENTAL;
        }else{
            this.scanType = FULL;
        }
    }



    //adding underscore to prevent getOperation() to be called during logging of this object in log()
    //since we don't want the OPERATION to be a part of the logged object
    @Override
    public String _getOperation() {
        return OPERATION;
    }
    
}
