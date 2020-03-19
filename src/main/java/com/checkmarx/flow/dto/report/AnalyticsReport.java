package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import groovy.util.logging.Slf4j;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.logstash.logback.marker.Markers.*;

@Data
@Slf4j
public abstract class AnalyticsReport {

    public static final Logger jsonlogger = LoggerFactory.getLogger("jsonLogger");

    //public static final Logger log = org.slf4j.LoggerFactory.getLogger(Writable.class);
    public static final String SAST = "SAST";
    public static final String OSA = "OSA";

    protected String scanId;
    protected String repoUrl;
    protected String scanType;
    
    public AnalyticsReport(String scanId, ScanRequest request) {
        this.scanId = scanId;
        this.repoUrl = request.getRepoUrl();
        scanType = OSA;
    }

    public AnalyticsReport(Integer scanId, ScanRequest request) {
        if(scanId!=null) {
            this.scanId = scanId.toString();
        }else{
            this.scanId = null;
        }
                
        this.repoUrl = request.getRepoUrl();
        scanType = SAST;
    }
    
    public void log() {
        jsonlogger.info(append(_getOperation(), this), "");
    }

    //adding underscore to prevent getOperation() to be called during logging of this object in log()
    //since we don't want the OPERATION to be a part of the logged object
    public abstract String _getOperation();
}
