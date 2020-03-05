package com.checkmarx.flow.cucumber.integration.jira;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.jira.PublishUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class, PublishUtils.class})
public class JiraLoadTestSteps {

    @Autowired
    private IJiraTestUtils jiraUtils;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private CxProperties cxProperties;

    @Autowired
    private PublishUtils publishUtils;

    private String filename;

    List<Long> durations = new ArrayList<>(100);

    int percentsThrshold;

    @Before("@JiraLoadTests")
    public void setOfflineMode() {
        cxProperties.setOffline(true);
    }


    @Before("@JiraLoadTests")
    public void cleanJiraProject() throws IOException {
        jiraUtils.ensureProjectExists(jiraProperties.getProject());
        jiraUtils.ensureIssueTypeExists(jiraProperties.getIssueType());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }


    @Given("SAST results with {int} findings, that should result in {int} JIRS issues")
    public void prepareFile(int findings, int issues) {
        if (findings != 1000 || issues != 1000) {
            throw new IllegalArgumentException(String.format("No XML files are defined for the paramters given. Findings: %d, Issues: %d", findings, issues));
        }
        filename = "cucumber/data/sample-sast-results/300-findings.xml";
    }

    @When("results are parsed and published {int} times, and time is recorded for each sample")
    public void publishResults(int numOfSamples) throws ExitThrowable, IOException {
        for (int i = 0; i < numOfSamples; i++) {
            internalPublishResults();
            verifyNumberOfIssues();
        }
    }

    @Then("{int} percents of publish request should take less than {int} seconds")
    public void validateDurations(int percentsThreshold, int durationThreshold) {
        log.info("Durations: ");
        durations.stream().forEach(l -> log.info(l + ", "));
        // We put in a member because we will need this number in a future step.
        this.percentsThrshold = percentsThreshold;
        validateDuration(durations, percentsThreshold, durationThreshold, "Primary validation failed.");
    }

    @Then("the other tests should take less than {int} seconds")
    public void validateSecondaryDurations(int durationThreshold) {
        validateDuration(durations, 100- this.percentsThrshold, durationThreshold, "Secondary validation failed.");
    }

    private void validateDuration(List<Long> duration, int percentageThrshold, long durationThreshold, String message) {
        Long passed = duration.stream().filter(l -> l <= durationThreshold).count();
        int passPercentage = (int) ((passed * 100 )/ duration.size() );
        log.info("&&&&&&&&&&&&&&&&&&&&&&&&&&& Passed: " + passed + " Total: " + duration.size());
        Assert.assertTrue(message + "Percentage Thrashold: " + percentageThrshold + ", Passed: " + passPercentage ,passPercentage >=percentageThrshold);
    }




    private void verifyNumberOfIssues() {
        int numOfIssues = jiraUtils.getNumberOfIssuesInProject(jiraProperties.getProject());
        Assert.assertEquals("The number of issues in JIRA is wrong", 385, numOfIssues);
    }

    private void internalPublishResults() throws ExitThrowable, IOException {
        ScanRequest request = getScanRequestWithDefaults();
        File file = publishUtils.getFileFromResourcePath(filename);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        publishUtils.publishRequest(request, file, BugTracker.Type.JIRA);
        stopWatch.stop();
        durations.add(stopWatch.getTime());
    }


    private ScanRequest getScanRequestWithDefaults() {
        return ScanRequest.builder()
                .application("App1")
                .product(ScanRequest.Product.CX)
                .project("CodeInjection1")
                .team("CxServer")
                .namespace("compTest")
                .repoName("repo")
                .repoUrl("http://localhost/repo.git")
                .repoUrlWithAuth("http://localhost/repo.git")
                .repoType(ScanRequest.Repository.NA)
                .branch("master")
                .refs(Constants.CX_BRANCH_PREFIX.concat("master"))
                .email(null)
                .incremental(false)
                .scanPreset("Checkmarx Default")
                .build();
    }


}
