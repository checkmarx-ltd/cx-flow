package com.checkmarx.flow.cucumber.component.projectkeyscript;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.JiraService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {CxFlowApplication.class})
@CucumberContextConfiguration
@Slf4j
public class JiraProjectKeyScriptSteps {

    private final JiraService jiraService;
    private final JiraProperties jiraProperties;
    private final ScanRequest request = new ScanRequest();

    private final static  String EMPTY_SCRIPT = "empty";
    private final static  String INVALID_SCRIPT = "invalid-syntax-project-key-script";
    private final static  String EMPTY_STRING = "";
    private final static  String DOESNT_EXISTS = "doesnt exists";

    public JiraProjectKeyScriptSteps(JiraService jiraService,
                                     JiraProperties jiraProperties) {
        this.jiraService = jiraService;
        this.jiraProperties = jiraProperties;
    }

    @Before("@ConfigureJiraProjectKey")
    public void setData() {
        request.setBugTracker(BugTracker.builder()
                                      .projectKey("Default Project Key")
                                      .build());
    }

    @Given("given 'jira-project-key' script name is {string}")
    public void setJiraProjectKeyScriptName(String scriptName) {
        if (scriptName.equals(EMPTY_SCRIPT)) {
            jiraProperties.setProjectKeyScript(EMPTY_STRING);
        } else {
            String fullName = scriptName + ".groovy";

            if (scriptName.equals(INVALID_SCRIPT)) {
                fullName = fullName + ".invalid";
            }

            String projectKeyScript = getScriptFullPath(fullName);
            jiraProperties.setProjectKeyScript(projectKeyScript);
        }
    }

    private String getScriptFullPath(String scriptName) {
        String result;
        String path = "input-scripts-sample/jira-project-key/" + scriptName;

        try {
            File fullPath = TestUtils.getFileFromResource(path);
            result = fullPath.getPath();
        }catch (IOException ex) {
            result = "script-not-exist";
        }

        return result;
    }

    @When("Determine JIRA project key")
    public void determineJIRAProjectKey() {
        String jiraProjectKey = jiraService.determineJiraProjectKey(request);
        request.getBugTracker().setProjectKey(jiraProjectKey);
    }

    @Then("JIRA project key is equal to {string}")
    public void jiraProjectKeyIsEqualTo(String expectedProjectKey) {
        log.info("Comparing expected project key '{}' to actual project key '{}'",
                 expectedProjectKey,
                 request.getBugTracker().getProjectKey());

        assertEquals(expectedProjectKey,  request.getBugTracker().getProjectKey(), "fail comparing expected JIRA" +
                " project key to actual project key in scan request");
    }

    @When("Scan request contain feature repo name {string}")
    public void scanRequestContainFeatureRepoName(String repoName) {
        request.setRepoName(repoName);
    }
}
