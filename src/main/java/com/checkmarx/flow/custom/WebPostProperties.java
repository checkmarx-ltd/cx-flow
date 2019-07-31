package com.checkmarx.flow.custom;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "web")
@Validated
public class WebPostProperties {
    private String fileNameFormat = "[APP]-[BRANCH]-[TIME]";
    private String dataFolder = "/tmp";


    public String getDataFolder() {
        return dataFolder;
    }

    public void setDataFolder(String dataFolder) {
        this.dataFolder = dataFolder;
    }
}
