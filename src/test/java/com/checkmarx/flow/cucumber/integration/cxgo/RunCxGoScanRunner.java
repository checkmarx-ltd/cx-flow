package com.checkmarx.flow.cucumber.integration.cxgo;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/integration/cxgo", "json:build/cucumber/integration/cxgo/cucumber.json" },
        features = "src/test/resources/cucumber/features/integrationTests/cxgo/cxgoScanProcessing.feature",
        glue = {"com.checkmarx.flow.cucumber.integration.cxgo"},
        tags = "@CxGoIntegrationTests and not @Skip")
public class RunCxGoScanRunner {
}