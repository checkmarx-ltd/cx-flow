package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.AesEncodingUtils;
import com.checkmarx.sdk.exception.CheckmarxException;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.logstash.logback.marker.Markers.*;

@Data
public abstract class AnalyticsReport {

    protected static final Logger jsonlogger = LoggerFactory.getLogger("jsonLogger");
    protected static final Logger log = org.slf4j.LoggerFactory.getLogger(AnalyticsReport.class);
    
    public static final String SAST = "SAST";
    public static final String OSA = "OSA";

    protected String repoUrl = null;
    protected String scanInitiator;
    protected String scanId;
    
    public AnalyticsReport(String scanId, ScanRequest request) {
        this.scanId = scanId;
        scanInitiator = OSA;
    }

    public AnalyticsReport(Integer scanId, ScanRequest request) {
        if(scanId!=null) {
            this.scanId = scanId.toString();
        }else{
            this.scanId = null;
        }
        scanInitiator = SAST;
    }
    
    public void log() {
        jsonlogger.info(append(_getOperation(), this), "");
    }

    //adding underscore to prevent getOperation() to be called during logging of this object in log()
    //since we don't want the OPERATION to be a part of the logged object
    protected abstract String _getOperation();

    protected String setEncodedRepoUrl(String sourcesPath, String outputMsg) {
        if(sourcesPath != null) {
            repoUrl = sourcesPath;
        }
        try {
            this.repoUrl = AesEncodingUtils.encode(repoUrl);
            return outputMsg;

        } catch (CheckmarxException e) {
            this.repoUrl = null;
            outputMsg = "Unable to encode repoUrl " + e.getMessage();
            log.error(outputMsg);
            return outputMsg;
        }
    }
}
