package com.checkmarx.flow.cucumber.integration.end2end.genericendtoend;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/integration/end2end/genericendtoend", "json:build/cucumber/features/e2eTests/genericEndToEnd" },
        features = "src/test/resources/cucumber/features",
        glue = { "com.checkmarx.flow.cucumber.integration.end2end.genericendtoend"},
        tags = "@EndToEnd and @Integration and not @Skip")
public class RunGenericEndToEndTest {

}