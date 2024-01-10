package com.checkmarx.flow.custom;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "json")
@Validated
public class JsonProperties {
    //TEAM, PROJECT, APP, BRANCH, REPO, NAMESPACE, TIME (YYYYMMDD.HHMMSS
    private String fileNameFormat = "[APP]-[BRANCH]-[TIME]";
    private String dataFolder = "/tmp";
    private boolean latestCommitterEmail = false;

    public String getFileNameFormat() {
        return fileNameFormat;
    }

    public void setFileNameFormat(String fileNameFormat) {
        this.fileNameFormat = fileNameFormat;
    }

    public String getDataFolder() {
        return dataFolder;
    }

    public void setDataFolder(String dataFolder) {
        this.dataFolder = dataFolder;
    }
    public boolean isLatestCommitterEmail() {
        return latestCommitterEmail;
    }
    public void setLatestCommitterEmail(boolean latestCommitterEmail) {
        this.latestCommitterEmail = latestCommitterEmail;
    }
}
