package com.checkmarx.flow.cucumber.integration.sast.scan;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features",
        tags = "@ScanFeature and @IntegrationTest and not @Skip")
public class RunScanSteps {
}
