package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "sarif")
@Validated
public class SarifProperties {
    private String filePath = "./cx.sarif";
    private String scannerName = "Checkmarx";
    private String organization = "Checkmarx";
    private String sarifSchema="https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json";
    private String sarifVersion = "2.1.0";
    private String semanticVersion = "1.0.0";
    private Map<String, String> severityMap = new HashMap<>();

    @PostConstruct
    private void loadSeverityMap(){
        severityMap.put("High", "error");
        severityMap.put("Medium", "error");
        severityMap.put("Low", "warning");
        severityMap.put("Informational", "warning");
    }

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

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getSarifSchema() {
        return sarifSchema;
    }

    public void setSarifSchema(String sarifSchema) {
        this.sarifSchema = sarifSchema;
    }

    public String getSarifVersion() {
        return sarifVersion;
    }

    public void setSarifVersion(String sarifVersion) {
        this.sarifVersion = sarifVersion;
    }

    public String getSemanticVersion() {
        return semanticVersion;
    }

    public void setSemanticVersion(String semanticVersion) {
        this.semanticVersion = semanticVersion;
    }

    public Map<String, String> getSeverityMap() {
        return severityMap;
    }

    public void setSeverityMap(Map<String, String> severityMap) {
        this.severityMap = severityMap;
    }
}
