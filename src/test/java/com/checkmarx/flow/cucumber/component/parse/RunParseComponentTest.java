package com.checkmarx.flow.cucumber.component.parse;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/component/parse", "json:build/cucumber/component/parse/cucumber.json" },
        features = "src/test/resources/cucumber/features",
        glue = { "com.checkmarx.flow.cucumber.component.parse" },
        tags = "@ParseFeature and @ComponentTest  and not @Skip")
public class RunParseComponentTest {
}
