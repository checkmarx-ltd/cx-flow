package com.checkmarx.flow.cucumber.integration.multiScmInstances;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/jira/publish-processing.feature",
        tags = "@Scm_Optional_Instance and not @Skip")
public class RunPublishTicketsWithOptionalScmConfSteps {
}