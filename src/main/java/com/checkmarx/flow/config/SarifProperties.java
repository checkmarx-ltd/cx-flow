package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "sarif")
@Validated
public class SarifProperties {
    private String filePath = "./cx.json";
    private String scannerName = "Checkmarx";
    private String scannerFullName = "Checkmarx SAST";

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getScannerName() {
        return scannerName;
    }

    public void setScannerName(String scannerName) {
        this.scannerName = scannerName;
    }

    public String getScannerFullName() {
        return scannerFullName;
    }

    public void setScannerFullName(String scannerFullName) {
        this.scannerFullName = scannerFullName;
    }
}
