package com.checkmarx.flow.cucumber.integration.sca_scanner.zip_scan;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/sca/scanResultsProcessing.feature",
        tags = "@SCA_Zip_Scan and not @Skip")
public class ScaZipScanRunner {
}