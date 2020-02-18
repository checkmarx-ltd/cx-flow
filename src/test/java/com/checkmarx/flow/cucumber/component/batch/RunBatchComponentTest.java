package com.checkmarx.flow.cucumber.component.batch;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/component/batch", "json:build/cucumber/component/batch/cucumber.json" },
        features = "src/test/resources/cucumber/features",
        glue = { "com.checkmarx.flow.cucumber.common.steps", "com.checkmarx.flow.cucumber.component.batch" },
        tags = "@BatchFeature and @ComponentTest and not @Skip")
public class RunBatchComponentTest {
}
