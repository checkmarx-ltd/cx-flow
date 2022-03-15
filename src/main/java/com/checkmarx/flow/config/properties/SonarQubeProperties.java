package com.checkmarx.flow.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "sonarqube")
@Validated
public class SonarQubeProperties {
    private String filePath = "./cxSonarQube.json";
    private String scaScannerName = "Cx - SCA";
    private String sastScannerName = "Cx - SAST";
    private String scaOrganization = "Checkmarx";
    private String sastOrganization = "Checkmarx";

    private String sonarQubeSchema="";
    private String sonarQubeVersion = "2.1.0";
    private String semanticVersion = "1.0.0";
    private Map<String, String> severityMap = new HashMap<>();

    @PostConstruct
    private void loadSeverityMap(){
        severityMap.put("High", "CRITICAL");
        severityMap.put("Medium", "MAJOR");
        severityMap.put("Low", "MINOR");
        severityMap.put("Information", "INFO");
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

    public String getSonarQubeSchema() {
        return sonarQubeSchema;
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

    public void setSonarQubeSchema(String sonarQubeSchema) {
        this.sonarQubeSchema = sonarQubeSchema;
    }

    public String getSonarQubeVersion() {
        return sonarQubeVersion;
    }

    public void setSonarQubeVersion(String sonarQubeVersion) {
        this.sonarQubeVersion = sonarQubeVersion;
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
