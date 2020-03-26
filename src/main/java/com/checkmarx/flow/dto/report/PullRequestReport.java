package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

import static net.logstash.logback.marker.Markers.append;
import static net.logstash.logback.marker.Markers.appendEntries;

@Data
@EqualsAndHashCode(callSuper = true)
public class PullRequestReport extends AnalyticsReport {

    public static final String OPERATION = "Pull Request";

    private String pullRequestStatus;

    private Integer findingsHigh = 0;
    private Integer findingsMedium = 0;
    private Integer findingsLow = 0;
    private Integer findingsInfo = 0;
    
    private Integer thresholdsHigh = 0;
    private Integer thresholdsMedium = 0;
    private Integer thresholdsLow = 0;
    private Integer thresholdsInfo;


    public PullRequestReport(ScanDetails scanDetails, ScanRequest request) {
        super(scanDetails.getScanId(), request);
        
        repoUrl = request.getRepoUrl();
        if(scanDetails.isOsaScan()){
            scanId = scanDetails.getOsaScanId();
            scanInitiator = OSA;
        }
    }


    public String getPullRequestStatus() {
        return pullRequestStatus;
    }
    
    public void setPullRequestStatus(String status){
        this.pullRequestStatus = status;
    }

    //adding underscore to prevent getOperation() to be called during logging of this object in log()
    //since we don't want the OPERATION to be a part of the logged object 
    @Override
    public String _getOperation() {
        return OPERATION;
    }

    public void setFindingsPerSeverity(Map<FindingSeverity, Integer> findingsPerSeverity) {
        this.findingsHigh= findingsPerSeverity.get(FindingSeverity.HIGH);
        this.findingsMedium= findingsPerSeverity.get(FindingSeverity.MEDIUM);
        this.findingsLow= findingsPerSeverity.get(FindingSeverity.LOW);
        this.findingsInfo= findingsPerSeverity.get(FindingSeverity.INFO);
        
    }

    public void setThresholds(Map<FindingSeverity, Integer> thresholds) {
        this.thresholdsHigh = thresholds.get(FindingSeverity.HIGH);
        this.thresholdsMedium = thresholds.get(FindingSeverity.MEDIUM);
        this.thresholdsLow = thresholds.get(FindingSeverity.LOW);
        this.thresholdsInfo = thresholds.get(FindingSeverity.LOW);
        
    }
}
