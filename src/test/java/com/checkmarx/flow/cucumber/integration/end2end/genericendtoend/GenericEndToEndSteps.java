package com.checkmarx.flow.cucumber.integration.end2end.genericendtoend;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@SpringBootTest(classes = {CxFlowApplication.class})
public class GenericEndToEndSteps {
    @Autowired
    private FlowProperties flowProperties;
    /*
     * repositories
     */
    @Autowired GitHubProperties gitHubProperties;
    @Autowired ADOProperties adoProperties;

    /*
     * bug-trackers
     */
    @Autowired JiraProperties jiraProperties;

    private Repository repository;
    private BugTracker bugTracker;
    private ConfigurableApplicationContext appContext;

    @Given("repository is {word}")
    public void setRepository(String repository) {
        this.repository = Repository.setTo(repository, this);
    }

    @And("bug-tracker is {word}")
    public void setBugTracker(String bugTracker) {
        this.bugTracker = BugTracker.setTo(bugTracker, this);
        flowProperties.setBugTracker(bugTracker); //TO DO: ?!?!?
    }

    @And("Scan engine is {word}")
    public void setScanEngine() {
        //TO DO: set the engine
    }

    @And("CxFlow is running as a service")
    public void runAsService() {
        log.info("runnning cx-flow as a service");
        appContext = TestUtils.runCxFlowAsService();
    }

    @And("webhook is configured for push event")
    public void generatePushWebHook() {
        repository.generateWebHook(HookType.PUSH);
    }

    @And("webhook is configured for pull-request")
    public void generatePRWebHook() {
        repository.generateWebHook(HookType.PULL_REQUEST);
    }

    @When("pushing a change")
    public void pushChange() {
        String content = null;
        try {
            content = getFileInBase64();
        } catch (IOException e) {
            fail("can not read source file");
        }
        repository.pushFile(content);
    }

    @When("creating pull-request")
    public void createPR() {
        repository.createPR();
    }

    @Then("bug-tracker issues are updated")
    public void validateIssueOnBugTracker() {
        String severities = "(" + flowProperties.getFilterSeverity().stream().collect(Collectors.joining(",")) + ")";
        bugTracker.verifyIssueCreated(severities);
    }

    @Then("pull-request is updated")
    public void checkPRUpdate() {
        // TO DO: get data from pr
        // TO DO: validate data
    }

    @After
    public void cleanUp() {
        repository.cleanup();
        bugTracker.deleteIssues(); //TO DO: move to bt
        SpringApplication.exit(appContext);
    }

    private String getFileInBase64() throws IOException {
        String path = new StringJoiner(File.separator)
                .add("cucumber")
                .add("data")
                .add("input-files-toscan")
                .add("e2e.src")
                .toString();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            try (
                    InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
                    BufferedReader reader = new BufferedReader(isr)
            ) {
                String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                String encodedString = Base64.getEncoder().encodeToString(content.getBytes());
                return encodedString;
            }
        }
    }
}