package com.checkmarx.flow.cucumber.integration.sca_scanner.config_as_code;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/cxconfig.feature",
        glue = {"com.checkmarx.flow.cucumber.integration.sca_scanner.config_as_code"},
        tags = "@Sca_cx_config and not @Skip")
public class ScaConfigAsCodeRunner {
}