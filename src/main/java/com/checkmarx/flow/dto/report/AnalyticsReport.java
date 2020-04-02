package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.AesEncryptionUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static net.logstash.logback.marker.Markers.append;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Slf4j
public abstract class AnalyticsReport {

    protected static final Logger jsonlogger = LoggerFactory.getLogger("jsonLogger");

    protected static final String NOT_APPLICABLE = "NA";
    public static final String SAST = "SAST";
    public static final String OSA = "OSA";

    protected String projectName = NOT_APPLICABLE;
    protected String repoUrl = null;
    protected String scanInitiator;
    protected String scanId;


    public AnalyticsReport(String scanId, ScanRequest request) {
        this.scanId = scanId;
        scanInitiator = OSA;
        if (scanId == null) {
            this.scanId = NOT_APPLICABLE;
        }
        this.projectName = request.getProject();
    }

    public AnalyticsReport(Integer scanId, ScanRequest request) {
        if (scanId != null) {
            this.scanId = scanId.toString();
        } else {
            this.scanId = NOT_APPLICABLE;
        }
        this.projectName = request.getProject();
        scanInitiator = SAST;
    }

    public void log() {
        jsonlogger.info(append(_getOperation(), this), "");
    }


    /**
     * adding underscore to prevent getOperation() to be called during logging of this object in log()
     * since we don't want the OPERATION to be a part of the logged object
     */
    protected abstract String _getOperation();


    protected void setEncryptedRepoUrl(String repoUrl) {
        try {
            if (repoUrl != null) {
                this.repoUrl = AesEncryptionUtils.encrypt(repoUrl);
            } else {
                this.repoUrl = NOT_APPLICABLE;
            }

        } catch (IOException e) {
            this.repoUrl = NOT_APPLICABLE;

            log.error("Unable to encrypt repoUrl " + e.getMessage());
        }
    }
}
