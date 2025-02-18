package com.checkmarx.flow.cucumber.component.commentscript;

import com.checkmarx.flow.utils.JasyptConfig;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

@RunWith(Cucumber.class)
@ContextConfiguration(classes = {JasyptConfig.class})
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/configure-sast-comment-script.feature",
        tags = "not @Skip")
public class CommentScriptRunner {

}
