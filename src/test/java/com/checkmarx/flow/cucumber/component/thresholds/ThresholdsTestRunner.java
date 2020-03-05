package com.checkmarx.flow.cucumber.component.thresholds;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/thresholds.feature",
        tags = "@ThresholdsFeature and not @Skip")
public class ThresholdsTestRunner {
}
