package com.checkmarx.flow.cucumber.integration.cxconfigbugtracker;


import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = { "pretty", "summary", "html:build/cucumber/integration/cxconfig"},
        features = "src/test/resources/cucumber/features/integrationTests/cxConfigBugTracker.feature",
        glue = { "com.checkmarx.flow.cucumber.integration.cxconfigbugtracker" },
        tags = "@CxConfigBugTrackerFeature and not @Skip")
public class CxConfigBugTrackerRunner {
}
