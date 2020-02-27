package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import io.cucumber.java.Before;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {CxFlowApplication.class})
public class PublishingSteps {

    private final FlowProperties flowProperties;

    public PublishingSteps(FlowProperties flowProperties) {
        this.flowProperties = flowProperties;
    }

    @Before
    public void setIssueTracker() {
        flowProperties.setBugTracker("Azure");
    }

}
