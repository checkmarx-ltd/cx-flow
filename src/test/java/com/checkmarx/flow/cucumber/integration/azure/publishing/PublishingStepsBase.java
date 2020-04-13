package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.cucumber.common.utils.TestUtils;

import java.io.IOException;
import java.util.Properties;

public abstract class PublishingStepsBase {
    private static final String PROPERTIES_FILE_PATH = "cucumber/features/integrationTests/azure/publishing.properties";
    protected static final String ISSUE_TRACKER_NAME = "Azure";

    public static String getProjectName() throws IOException {
        Properties testProperties = TestUtils.getPropertiesFromResource(PROPERTIES_FILE_PATH);
        return testProperties.getProperty("projectName");
    }
}
