package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = {CxFlowApplication.class})
public class PublishingSteps {
    private static final String PROPERTIES_FILE_PATH = "cucumber/features/integrationTests/azure/publishing.properties";

    @Autowired
    private FlowProperties flowProperties;

    @Autowired
    private ADOProperties adoProperties;

    private AzureDevopsClient adoClient;

    private String projectName;

    @Before
    public void prepareEnvironment() throws IOException {
        Properties testProperties = TestUtils.getPropertiesFromResource(PROPERTIES_FILE_PATH);

        projectName = testProperties.getProperty("projectName");

        flowProperties.setBugTracker("Azure");

        adoClient = new AzureDevopsClient(adoProperties);
        adoClient.ensureProjectExists(projectName);
        adoClient.deleteProjectIssues(projectName);
    }

    @Given("Azure DevOps doesn't contain any issues")
    public void azureDevOpsDoesnTContainAnyIssues() throws IOException {
        int actualIssueCount = adoClient.getIssueCount(projectName);
        assertEquals("Unexpected issue count.", 0, actualIssueCount);
    }
}
