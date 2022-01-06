package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "sarif")
@Validated
public class SarifProperties {
    private String filePath = "./cx.sarif";
    private String scaScannerName = "Checkmarx - SCA";
    private String sastScannerName = "Checkmarx - SAST";
    private String scaOrganization = "Checkmarx - SCA";
    private String sastOrganization = "Checkmarx - SAST";

    private String sarifSchema="https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json";
    private String sarifVersion = "2.1.0";
    private String semanticVersion = "1.0.0";
    private Map<String, String> severityMap = new HashMap<>();
    private Map<String, String> securitySeverityMap = new HashMap<>();

    @PostConstruct
    private void loadSeverityMap(){
        severityMap.put("High", "error");
        severityMap.put("Medium", "error");
        severityMap.put("Low", "warning");
        severityMap.put("Informational", "warning");
    }

    @PostConstruct
    private void loadSecuritySeverityMap() {
        securitySeverityMap.put("High", "10.0");
        securitySeverityMap.put("Medium", "8.0");
        securitySeverityMap.put("Low", "5.0");
        securitySeverityMap.put("Informational", "2.0");
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    public String getScaScannerName() {
        return scaScannerName;
    }

    public void setScaScannerName(String scaScannerName) {
        this.scaScannerName = scaScannerName;
    }

    public String getSastScannerName() {
        return sastScannerName;
    }

    public void setSastScannerName(String sastScannerName) {
        this.sastScannerName = sastScannerName;
    }

    public String getSarifSchema() {
        return sarifSchema;
    }

    public String getScaOrganization() {
        return scaOrganization;
    }

    public void setScaOrganization(String scaOrganization) {
        this.scaOrganization = scaOrganization;
    }

    public String getSastOrganization() {
        return sastOrganization;
    }

    public void setSastOrganization(String sastOrganization) {
        this.sastOrganization = sastOrganization;
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

    public Map<String, String> getSecuritySeverityMap() {
        return securitySeverityMap;
    }

    public void setSecuritySeverityMap(Map<String, String> securitySeverityMap) {
        this.securitySeverityMap = securitySeverityMap;
    }
}
