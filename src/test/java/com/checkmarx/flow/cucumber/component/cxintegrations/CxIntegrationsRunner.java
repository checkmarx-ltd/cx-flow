package com.checkmarx.flow.cucumber.component.cxintegrations;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/cxIntegrations.feature",
        tags = "@Cx-integrations and not @Skip")
public class CxIntegrationsRunner {
}