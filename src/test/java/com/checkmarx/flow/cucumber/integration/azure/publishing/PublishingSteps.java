package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {CxFlowApplication.class})
public class PublishingSteps {
    private static final String PROJECT_NAME = "CxFlowTest3";

    @Autowired
    private FlowProperties flowProperties;

    @Autowired
    private ADOProperties adoProperties;

    private AzureDevopsClient adoClient;

    @Before
    public void prepareEnvironment() {
        flowProperties.setBugTracker("Azure");
        adoProperties.setUrl("https://dev.azure.com/myorganization");

        adoClient = new AzureDevopsClient(adoProperties);
        adoClient.ensureProjectExists(PROJECT_NAME);
    }

    @Given("Azure DevOps doesn't contain any issues")
    public void azureDevOpsDoesnTContainAnyIssues() {
    }
}
