package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Status;
import lombok.Data;
import lombok.EqualsAndHashCode;



@Data
@EqualsAndHashCode(callSuper = true)
public class ScanReport extends Report {

    public static final String OPERATION = "Scan Request";
    public static final String INCREMENTAL = "Inc";
    public static final String FULL = "Full";
    private String scanStatus;
    private String branch;
    private String repoType;
    private String scanType;

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

        if(sourcesPath != null) {
            repoUrl = sourcesPath;
        }

        if(request.isIncremental()){
            this.scanType = INCREMENTAL;
        }else{
            this.scanType = FULL;
        }
        this.scanStatus = status.getMessage();
    }


    @Override
    public String _getOperation() {
        return OPERATION;
    }
}
