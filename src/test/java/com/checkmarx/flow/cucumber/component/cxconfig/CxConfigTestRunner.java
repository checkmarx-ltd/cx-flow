package com.checkmarx.flow.cucumber.component.cxconfig;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/cxconfig.feature",
        tags = "@CxConfigFeature and not @Skip")
public class CxConfigTestRunner {
}
