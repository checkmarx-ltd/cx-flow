package com.checkmarx.flow.cucumber.integration.azure.publishing.github2ado;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = { "pretty", "summary", "html:build/cucumber/integration/azure/publishing/github2ado/"},
        features = "src/test/resources/cucumber/features/integrationTests/azure/github2ado/github2ado.feature",
        glue = { "com.checkmarx.flow.cucumber.integration.azure.publishing.github2ado" },
        tags = "@Github2AdoFeature and not @Skip")
public class Github2AdoRunner {
}
