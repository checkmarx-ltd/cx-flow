package com.checkmarx.flow.cucumber.component.cxconfig;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = { "pretty", "summary", "html:build/cucumber/component/cxconfig"},
        features = "src/test/resources/cucumber/features/componentTests/cxconfig.feature",
        glue = { "com.checkmarx.flow.cucumber.common.steps", "com.checkmarx.flow.cucumber.component.cxconfig" },
        tags = "@CxConfigFeature and not @Skip")
public class CxConfigTestRunner {
}
