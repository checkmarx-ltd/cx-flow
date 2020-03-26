package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static net.logstash.logback.marker.Markers.append;
import static net.logstash.logback.marker.Markers.appendEntries;


@Getter
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


    public void setPullRequestStatus(String status){
        this.pullRequestStatus = status;
    }

    //adding underscore to prevent getOperation() to be called during logging of this object in log()
    //since we don't want the OPERATION to be a part of the logged object 
    @Override
    public String _getOperation() {
        return OPERATION;
    }

    public void setFindingsPerSeverity(Iterable<Map.Entry<FindingSeverity, Integer>> findingsPerSeverity) {

        Map<FindingSeverity, Integer> findingsMap = new HashMap<>();

        for (Map.Entry<FindingSeverity, Integer> entry: findingsPerSeverity) {
            findingsMap.put(entry.getKey(),entry.getValue() );
        }
        this.findingsPerSeverity = findingsMap;
    }

    public void setThresholds(Map<FindingSeverity, Integer> thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public void log()  {
        jsonlogger.info(append(_getOperation() , this), "");

    }
}
