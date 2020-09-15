package com.checkmarx.flow.cucumber.component.commentscript;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/configure-sast-comment-script.feature",
        tags = "not @Skip")
public class CommentScriptRunner {

}
