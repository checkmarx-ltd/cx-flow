package com.checkmarx.flow.cucumber.integration.sast.config;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/sast/overwriting-project-config.feature",
        tags = "not @Skip")
public class OverwritingProjectConfigRunner {
}
