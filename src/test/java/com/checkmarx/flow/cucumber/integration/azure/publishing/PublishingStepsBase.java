package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Properties;

@Getter(AccessLevel.PROTECTED)
public abstract class PublishingStepsBase {
    protected static final String ISSUE_TRACKER_NAME = "Azure";
    private static final String PROPERTIES_FILE_PATH = "cucumber/features/integrationTests/azure/publishing.properties";

    private String projectName;
    private String organizationName;

    @Autowired
    private AzureDevopsClient adoClient;

    @Autowired
    private ADOProperties adoProperties;

    @PostConstruct
    private void initAdoClient() throws IOException {
        Properties testProperties = TestUtils.getPropertiesFromResource(PROPERTIES_FILE_PATH);
        projectName = testProperties.getProperty("projectName");
        organizationName = testProperties.getProperty("organization");
        adoClient.init(organizationName, projectName);

        adoProperties.setProjectName(projectName);
    }
}
