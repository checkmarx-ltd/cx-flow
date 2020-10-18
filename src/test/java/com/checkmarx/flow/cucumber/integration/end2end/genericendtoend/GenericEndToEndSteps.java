package com.checkmarx.flow.cucumber.integration.end2end.genericendtoend;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.*;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;


import java.io.*;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@SpringBootTest(classes = {CxFlowApplication.class})
public class GenericEndToEndSteps {
    static final String E2E_CONFIG = "cx.config";
    static final String BRANCH_MASTER = "master";
    static final String BRANCH_DEVELOP = "develop";

    @Autowired
    private FlowProperties flowProperties;
    /*
     * repositories
     */
    @Autowired GitHubProperties gitHubProperties;
    @Autowired ADOProperties adoProperties;
    @Autowired GitLabProperties gitLabProperties;

    /*
     * bug-trackers
     */
    @Autowired JiraProperties jiraProperties;

    private Repository repository;
    private BugTracker bugTracker;
    private ConfigurableApplicationContext appContext;
    private String engine;

    @And("CxFlow is running as a service")
    public void runAsService() {
        log.info("running cx-flow as a service (active profile: {})", engine);
        appContext = TestUtils.runCxFlowAsServiceWithAdditionalProfiles(engine);
    }

    @And("repository is {word}")
    public void setRepository(String repository) {
        this.repository = Repository.setTo(repository, this);
    }

    @And("bug-tracker is {word}")
    public void setBugTracker(String bugTracker) {
        this.bugTracker = BugTracker.setTo(bugTracker, this);
        FlowProperties flowProperties = (FlowProperties)appContext.getBean("flowProperties");
        flowProperties.setBugTracker(bugTracker);
        log.info("Active scanners are: {}", flowProperties.getEnabledVulnerabilityScanners().toString());
    }

    @Given("Scan engine is {word}")
    public void setScanEngine(String engine) {
        this.engine = engine;
        log.info("setting scan engine to {}" , engine);
    }

    @And("webhook is configured for push event")
    public void generatePushWebHook() {
        repository.setActiveBranch(BRANCH_MASTER);
        repository.generateConfigAsCode(this);
        repository.generateWebHook(HookType.PUSH);
    }

    @And("webhook is configured for pull-request")
    public void generatePRWebHook() {
        repository.setActiveBranch(BRANCH_DEVELOP);
        repository.generateConfigAsCode(this);
        pushChange();
        repository.generateWebHook(HookType.PULL_REQUEST);
    }

    @When("pushing a change")
    public void pushChange() {
        String base64content = null;
        String fileTextContent = null;
        try {
            base64content = readFileInBase64();
            fileTextContent = readFile();
        } catch (IOException e) {
            fail("can not read source file");
        }
        repository.pushFile(base64content, fileTextContent);
    }

    @When("creating pull-request")
    public void createPR() {
        repository.createPR();
    }

    @Then("bug-tracker issues are updated")
    public void validateIssueOnBugTracker() {
        String severities = "(" + flowProperties.getFilterSeverity().stream().collect(Collectors.joining(",")) + ")";
        bugTracker.verifyIssueCreated(severities, engine);
    }

    @Then("pull-request is updated")
    public void checkPRUpdate() {
        repository.verifyPRUpdated(engine);
    }

    @After
    public void cleanUp() {
        repository.cleanup();
        Optional.ofNullable(bugTracker).ifPresent(BugTracker::deleteIssues);
        TestUtils.exitCxFlowService(appContext);
        log.info("finished clean-up");
    }

    String getEngine() {
        return engine;
    }

    private String readFileInBase64() throws IOException {
        String path = new StringJoiner(File.separator)
                .add("cucumber")
                .add("data")
                .add("input-files-toscan")
                .add("e2e.src")
                .toString();
        return getFileContentAsBase64(path);
    }

    private String readFile() throws IOException {
        String path = new StringJoiner(File.separator)
                .add("cucumber")
                .add("data")
                .add("input-files-toscan")
                .add("e2e.src")
                .toString();
        return getFileContent(path);
    }

    String getConfigAsCodeInBase64() throws IOException {
        String path = new StringJoiner(File.separator)
                .add("cucumber")
                .add("data")
                .add("input-files-toscan")
                .add(E2E_CONFIG + ".src")
                .toString();
        return getFileContentAsBase64(path);
    }

    String getConfigAsCodeTextContent() throws IOException {
        String path = new StringJoiner(File.separator)
                .add("cucumber")
                .add("data")
                .add("input-files-toscan")
                .add(E2E_CONFIG + ".src")
                .toString();
        return getFileContent(path);
    }

    private String getFileContentAsBase64(String path) throws IOException {
        return Base64.getEncoder().encodeToString(getFileContent(path).getBytes());
    }

    private String getFileContent(String path)  throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            try (
                    InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
                    BufferedReader reader = new BufferedReader(isr)
            ) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    public ConfigurableApplicationContext getAppContext() {
        return appContext;
    }
}