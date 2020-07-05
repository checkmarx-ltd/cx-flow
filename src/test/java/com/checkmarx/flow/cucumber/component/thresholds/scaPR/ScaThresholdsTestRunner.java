package com.checkmarx.flow.cucumber.component.thresholds.scaPR;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/sca-thresholds.feature",
        glue = {"com.checkmarx.flow.cucumber.component.thresholds.scaPR"},
        tags = "@CxSCA and @ThresholdsFeature and not @Skip")
public class ScaThresholdsTestRunner {

}