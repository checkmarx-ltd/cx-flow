package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.logstash.logback.marker.Markers.*;

@Data
public abstract class Writable {

    public static final Logger jsonlogger = LoggerFactory.getLogger("jsonLogger");

    public static final Logger log = org.slf4j.LoggerFactory.getLogger(Writable.class);
    public static final String SAST = "SAST";
    private static final String OSA = "OSA";

    protected String scanId;
    protected String repoUrl;
    protected String scanType;
    
    public Writable(String scanId, ScanRequest request) {
        this.scanId = scanId;
        this.repoUrl = request.getRepoUrl();
        scanType = OSA;
    }

    public Writable(Integer scanId, ScanRequest request) {
        this.scanId = scanId.toString();
        this.repoUrl = request.getRepoUrl();
        scanType = SAST;
    }
    
    public void write() throws MachinaException {
        jsonlogger.info(append(_getOperation(), this), "");
    }
    
    public abstract String _getOperation();
}
