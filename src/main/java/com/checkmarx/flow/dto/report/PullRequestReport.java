package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;

import java.util.HashMap;
import java.util.Map;

import static net.logstash.logback.marker.Markers.append;
import static net.logstash.logback.marker.Markers.appendEntries;


public class PullRequestReport extends AnalyticsReport {

    public static final String OPERATION = "Pull Request";

    private String pullRequestStatus;

    private Map<FindingSeverity, Integer> findingsPerSeverity = null;
    private Map<FindingSeverity, Integer> thresholds = null;

    
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
        this.findingsPerSeverity = findingsPerSeverity;
    }

    public void setThresholds(Map<FindingSeverity, Integer> thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public void log()  {
        jsonlogger.info(append(_getOperation() , this), "");

        if (findingsPerSeverity != null) {
            jsonlogger.info(appendEntries(findingsPerSeverity), "");
        }

        if (thresholds != null) {
            jsonlogger.info(appendEntries(thresholds), "");
        }
    }
}
