package com.checkmarx.flow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "sbom")
@Validated
public class SbomProperties {

    @Getter @Setter
    private  String jsonFileNameFormat = "[APP]-[BRANCH]-[TIME].json";
    @Getter @Setter
    private  String xmlFileNameFormat = "[APP]-[BRANCH]-[TIME].xml";
    private String dataFolder = "/tmp";
    @Getter @Setter
    private boolean hideDevAndTestDependencies = false;
    @Getter @Setter
    private boolean showOnlyEffectiveLicenses = false;

    private String reportFileFormat="CycloneDxJson";





    public String getDataFolder() {
        return dataFolder;
    }

    public void setDataFolder(String dataFolder) {
        this.dataFolder = dataFolder;
    }

    public String getReportFileFormat() {
        return reportFileFormat;
    }

    public void setReportFileFormat(String reportFileFormat) {
        this.reportFileFormat = reportFileFormat;
    }
}
