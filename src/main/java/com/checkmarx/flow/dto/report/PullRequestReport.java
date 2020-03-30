package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

import static net.logstash.logback.marker.Markers.append;
import static net.logstash.logback.marker.Markers.appendEntries;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PullRequestReport extends AnalyticsReport {

    public static final String OPERATION = "Pull Request";

    private String pullRequestStatus;

    private Map<FindingSeverity, Integer> findingsMap = null;
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

    public void setFindingsPerSeverity(Map<FindingSeverity, Integer> findingsMap) {
        this.findingsMap = findingsMap;
    }

    public void setThresholds(Map<FindingSeverity, Integer> thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public void log()  {
        jsonlogger.info(append(_getOperation() , this), "");

    }
}
