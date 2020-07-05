package com.checkmarx.flow.cucumber.component.thresholds.sastPR;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/thresholds.feature",
        glue = {"com.checkmarx.flow.cucumber.component.thresholds.sastPR"},
        tags = "@ThresholdsFeature and not @Skip and not @CxSCA")
public class ThresholdsTestRunner {
}
