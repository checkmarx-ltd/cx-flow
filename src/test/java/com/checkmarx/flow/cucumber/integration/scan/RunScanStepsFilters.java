package com.checkmarx.flow.cucumber.integration.scan;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/integration/scan"},
        features = "src/test/resources/cucumber/features",
        glue = { "com.checkmarx.flow.cucumber.common.steps", "com.checkmarx.flow.cucumber.integration.scan" },
        tags = "@ScanFeature and @IntegrationTest and @Filters and not @Skip")
public class RunScanStepsFilters {
}
