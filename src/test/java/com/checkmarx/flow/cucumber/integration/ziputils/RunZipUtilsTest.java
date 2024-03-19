package com.checkmarx.flow.cucumber.integration.ziputils;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/integration/ziputils", "json:build/cucumber/features/integration/ziputils" },
        features = "src/test/resources/cucumber/features",
        glue = { "com.checkmarx.flow.cucumber.integration.ziputils"},
        tags = "@Ziputils and @Integration and not @Skip")
public class RunZipUtilsTest {
}
