package com.checkmarx.flow.cucumber.component.fileurl;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/file-url.feature",
        tags = "not @Skip")
public class FileUrlRunner {
}
