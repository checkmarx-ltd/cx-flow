package com.checkmarx.flow.cucumber.component.csvissuetracker;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.custom.CsvIssueTracker;
import com.checkmarx.flow.custom.CsvProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.dto.ScanResults;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.checkmarx.flow.dto.ScanRequest.Product.CX;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(classes = {CxFlowApplication.class})
@CucumberContextConfiguration
public class CsvIssueTrackerSteps {


    private final IssueService issueService;
    private final FlowProperties flowProperties;
    private final BugTracker bugTracker = new BugTracker();
    private final ScanRequest scanRequest = new ScanRequest();
    private final ScanResults scanResults = new ScanResults();
    private final ApplicationContext applicationContext;
    private final CsvProperties properties;
    private final String team = "cxflow";
    private final String project = "csv-issue-tracker";

    private final String workDir = setWorkDir();

    public CsvIssueTrackerSteps(IssueService issueService, FlowProperties flowProperties, CodeBashingService codeBashingService, ApplicationContext applicationContext, CsvIssueTracker csvIssueTracker, CsvProperties properties) {
        this.issueService = issueService;
        this.flowProperties = flowProperties;
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    private static final Map<String, List<ScanResults.XIssue>> sastFilenamesByDescription;
    static {
        ScanResults.XIssue issueA = createXIssueWithFile("Admin.jsp", "Open_Redirect", "1");
        Map<String, List<ScanResults.XIssue>> temp = new HashMap<>();
        temp.put("2 findings with the same vulnerability type and in the same file", asList(issueA, issueA));
        temp.put("2 findings with the same vulnerability type and in different files", asList(issueA, createXIssueWithFile("Member.jsp", "Open_Redirect", "1")));
        temp.put("2 findings with different vulnerability types and in the same file", asList(issueA, createXIssueWithFile("Admin.jsp", "Close_Redirect", "2")));
        temp.put("2 findings with different vulnerability types and in different files", asList(issueA, createXIssueWithFile("Member.jsp", "Close_Redirect", "2")));
        sastFilenamesByDescription = Collections.unmodifiableMap(temp);
    }

    private static ScanResults.XIssue createXIssueWithFile(String fileName, String vulnerability, String cwe) {
        return ScanResults.XIssue.builder().file(fileName).vulnerability(vulnerability).cwe(cwe).severity("low").similarityId("1").description("desc").build();
    }

    @Given("Sast results having the following findings: {}")
    public void setSastScanResults(String description) {
        if (sastFilenamesByDescription.get(description) == null) {
            throw new PendingException("Cannot find scan result file for description: " + description);
        }
        scanResults.setXIssues(sastFilenamesByDescription.get(description));
    }

    @When("publish findings using Csv issue tracker")
    public void publishFindingsUsingCsvIssueTracker() throws MachinaException {
        issueService.setApplicationContext(applicationContext);
        setScanRequestWithCsvBugTracker();
        String stringFormat = "[TEAM]-[PROJECT]-123.csv";
        properties.setFileNameFormat(stringFormat);
        properties.setDataFolder(workDir);
        issueService.process(scanResults, scanRequest);
        flowProperties.setMitreUrl("url");
    }


    @Then("Csv result generated with {} issue\\(s)")
    public void checkCsvResult(long numberOfIssues) throws IOException {
        Path reportAbsolutePath = Paths.get(workDir, String.format("%s-%s-123.csv", team, project));
        long lines = Files.lines(reportAbsolutePath).count() - 1;
        assertThat(lines, equalTo(numberOfIssues));
    }

    private String setWorkDir() {
        String systemTempDir = FileUtils.getTempDirectoryPath();
        String scenarioSubDir = String.format("cxflow-csv-issue-tracker-%s", UUID.randomUUID());
        return Paths.get(systemTempDir, scenarioSubDir).toString();
    }

    private void setScanRequestWithCsvBugTracker() {
        bugTracker.setType(BugTracker.Type.CUSTOM);
        bugTracker.setCustomBean("Csv");
        scanRequest.setProduct(CX);
        scanRequest.setBugTracker(bugTracker);
        scanRequest.setTeam(team);
        scanRequest.setProject(project);
        scanRequest.setApplication("application");
    }

}
