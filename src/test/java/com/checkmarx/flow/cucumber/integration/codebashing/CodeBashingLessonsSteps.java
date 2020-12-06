package com.checkmarx.flow.cucumber.integration.codebashing;


import com.atlassian.jira.rest.client.api.domain.Issue;
import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.config.CodebashingProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.utils.gitlab.GitLabTestUtils;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.Assert;

import java.io.IOException;
import java.util.*;

@CucumberContextConfiguration
@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@Slf4j
@RequiredArgsConstructor
public class CodeBashingLessonsSteps {

    private static final String TENANT_BASE_URL = "https://tenant.base.url.com";
    private static final String CX_PROJECT_NAME = "CxFlow-Github-CLI";
    private static final String JIRA_PROJECT = "COD";
    private static final String GITLAB_PROJECT = "CodeBashing-Integration-Tests ";
    private GitLabTestUtils gitLabTestUtils;
    private String issueDescription = null;
    private String codebashingApiUrl;
    private String apiSecret;
    private final FlowProperties flowProperties;
    @Autowired
    private final CodebashingProperties codebashingProperties;
    @Autowired
    private final CxFlowRunner cxFlowRunner;
    @Autowired
    private final JiraProperties jiraProperties;
    @Autowired
    private IJiraTestUtils jiraUtils;
    @Autowired
    GitLabProperties gitLabProperties;

    @Before
    public void beforeEachScenario() {
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList("sast"));
        codebashingApiUrl = codebashingProperties.getCodebashingApiUrl();
        apiSecret = codebashingProperties.getApiSecret();
        gitLabTestUtils = new GitLabTestUtils(gitLabProperties);
    }

    @When("CxFlow parsing SAST results")
    public void getSastScanResults() {
        String commandLine = String.format("--project --cx-project=%s --app=MyApp --blocksysexit", CX_PROJECT_NAME);
        commandLine = String.format("--project --cx-project=%s --app=MyApp --namespace=cxflowtestuser --repo-name=codebashing-integration-tests --branch=master --blocksysexit", CX_PROJECT_NAME);
        tryRunCxFlow(commandLine);
    }


    @And("CodeBashing tenant base url {}")
    public void setCodeBashingConfiguration(boolean configurationExist) {
        if (configurationExist){
            codebashingProperties.setTenantBaseUrl(TENANT_BASE_URL);
            codebashingProperties.setCodebashingApiUrl(codebashingApiUrl);
            codebashingProperties.setApiSecret(apiSecret);
        }
        else {
            codebashingProperties.setTenantBaseUrl(null);
            codebashingProperties.setApiSecret(null);
            codebashingProperties.setCodebashingApiUrl(null);
        }
    }

    @Given("CxFlow uses Bug-Tracker type {}")
    public void setBugTracker(String bugTrackerType) throws IOException {
        flowProperties.setBugTracker(bugTrackerType);
        if(bugTrackerType.equals(BugTracker.Type.JIRA.toString())){
            initJiraBugTracker();
        }
        else{
            initGitLabBugTracker();
        }
    }

    @And("CxFlow finds a ticket with specific {} and {}")
    public void getissueByCwe(String issueType, int cweId) {

        if (flowProperties.getBugTracker().equals(BugTracker.Type.JIRA.toString())){
            Issue issueToValidate;
            Set<Issue> allIssues = jiraUtils.geAllIssuesInProject(JIRA_PROJECT);
            issueToValidate = findJiraIssueByVulnerabilityTypeAndCwe(allIssues, issueType, "*CWE:* " + cweId);
            issueDescription = issueToValidate.getDescription();
        }
        if (flowProperties.getBugTracker().equals("GitLab")){
            List<com.checkmarx.flow.dto.Issue> allIssues = gitLabTestUtils.getAllProjectIssues(GITLAB_PROJECT);
            com.checkmarx.flow.dto.Issue issueToValidate = findGitLabIssueByVulnerabilityTypeAndCwe(allIssues, issueType, "CWE:" + cweId);
            issueDescription = issueToValidate.getBody();
        }
    }

    @Then("CxFlow should add the correct {} to the ticket")
    public void verifyLessonPathInTicket(String lessonPath) {
        String expectedLessonPath;
        if (lessonPath.equals("default")){
            expectedLessonPath = flowProperties.getCodebashUrl();
        }
        else{
            expectedLessonPath= String.format("%s/%s", TENANT_BASE_URL, lessonPath);
        }
        String actualLessonPath = getLessonPathFromTicket();

        log.info("Comparing expected codebashing url: '{}' to actual '{}'", expectedLessonPath, actualLessonPath);
        Assert.assertEquals(expectedLessonPath, actualLessonPath);
    }

    @After("@CodeBashingIntegrationTests")
    public void CleanProjects() {
        log.info("Cleaning jira project after test: {}", jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
        gitLabTestUtils.deleteAllProjectIssues(GITLAB_PROJECT);
    }

    private void tryRunCxFlow(String commandLine) {
        try {
            TestUtils.runCxFlow(cxFlowRunner, commandLine);
        } catch (Throwable e) {
            log.info("Caught CxFlow execution exception: {}.", e.getClass().getSimpleName());
        }
    }

    private void initJiraBugTracker() throws IOException {
        log.info("Jira project key: {}", JIRA_PROJECT);
        jiraProperties.setProject(JIRA_PROJECT);
        log.info("Cleaning jira project before test: {}", jiraProperties.getProject());
        jiraUtils.ensureProjectExists(jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }

    private void initGitLabBugTracker() {
        gitLabTestUtils.deleteAllProjectIssues(GITLAB_PROJECT);
    }

    private Issue findJiraIssueByVulnerabilityTypeAndCwe(Set<Issue> allIssues, String vulnerabilityType, String cwe){
        Issue result = null;
        try {
            for (Issue issue: allIssues) {
                if (issue.getSummary().contains(vulnerabilityType) && issue.getDescription().contains(cwe)){
                    result = issue;
                    break;
                }
            }
        }
        catch (Exception e){
            log.error("could not find issue by type: {}", e.getMessage());
        }

        return  result;
    }

    private com.checkmarx.flow.dto.Issue findGitLabIssueByVulnerabilityTypeAndCwe(List<com.checkmarx.flow.dto.Issue> allIssues, String vulnerabilityType, String cwe){
        com.checkmarx.flow.dto.Issue result = null;
        try {
            for (com.checkmarx.flow.dto.Issue issue: allIssues) {
                if (issue.getTitle().contains(vulnerabilityType) && issue.getBody().contains(cwe)){
                    result = issue;
                    break;
                }
            }
        }
        catch (Exception e){
            log.error("could not find issue by type: {}", e.getMessage());
        }

        return  result;
    }

    private String getlessonPathFromJiraDescription(String description){
        int indexStart = description.indexOf("[Training");
        int indexEnd = description.indexOf("]", indexStart);

        String result = description.substring(indexStart + 10, indexEnd);

        log.info("found lesson path; {} in the ticket description", result);
        return result;
    }

    private String getlessonPathFromCustomTicketDescription(String description){
        int indexStart = description.indexOf("[Training");
        int indexEnd = description.indexOf(")", indexStart);

        String result = description.substring(indexStart + 11, indexEnd);

        log.info("found lesson path; {} in the ticket description", result);
        return result;
    }

    private String getLessonPathFromTicket(){
        String result = null;
        if (flowProperties.getBugTracker().equals(BugTracker.Type.JIRA.toString())){
            result = getlessonPathFromJiraDescription(issueDescription);
        }
        if (flowProperties.getBugTracker().equals("GitLab")){
           result = getlessonPathFromCustomTicketDescription(issueDescription);
        }
        return result;
    }
}