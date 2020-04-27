package com.checkmarx.flow.cucumber.integration.jira.analytics;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.cucumber.common.JsonLoggerTestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.report.JiraTicketsReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.exception.CheckmarxException;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@SpringBootTest(classes = { CxFlowApplication.class, JiraTestUtils.class, JsonLoggerTestUtils.class})
public class JiraAnalyticsOpenIssueViaCommandLineSteps extends JiraAnalyticsCommandLineCommonSteps {

    private static final String FINDING_PATH = "cucumber/data/sample-sast-results/1-finding.xml";
    private static final String JIRA_NEW_STATE = "new";

    @Before("@Jira_Analytics_Open_Issue_Command_Line")
    public void init() {
        cxProperties.setOffline(true);
        jiraProperties.setUrl(JIRA_URL);
        jiraService.init();

        setFilter("High");
    }

    @After("@Jira_Analytics_Open_Issue_Command_Line")
    public void tearDown() throws IOException {
        jiraUtils.cleanProject(PROJECT_KEY);
        jsonLoggerTestUtils.clearLogContents();
    }

    @Given("bug tracker is Jira")
    public void setTargetToJira() {
        bugTracker = getBasicBugTrackerToJira();
        flowProperties.setBugTracker(bugTracker.getType().name());
    }

    @When("opening a new Jira issue via the command line")
    public void openNewIssueViaCommandLine() throws IOException, ExitThrowable {
        sastScannerService.cxParseResults(getBasicScanRequest(), getFileFromResourcePath(FINDING_PATH));
    }

    @Then("a matching ticket creation data should be recorded in the analytics json file")
    public void validateAnalyticsJsonFile() throws CheckmarxException {
        List<String> currentNewIssuesList = jiraService.getCurrentNewIssuesList();
        JiraTicketsReport jiraTicketsReport = (JiraTicketsReport) jsonLoggerTestUtils.getReportNode(JiraTicketsReport.OPERATION, JiraTicketsReport.class);
        HashMap<String, List<String>> jiraTickets = jiraTicketsReport.getJiraTickets();

        int totalNewTickets = jiraTickets.get(JIRA_NEW_STATE).size();
        Assert.assertEquals("Expected to find 1 new JIRA opened ticket record on analytics logs , but found different count: " + totalNewTickets, 1, totalNewTickets);
        Assert.assertEquals("Actual JIRA new ticket's project key on analytics logs is different from expected",
                jiraService.getCurrentNewIssuesList(), jiraTickets.get(JIRA_NEW_STATE));
    }

    private BugTracker getBasicBugTrackerToJira() {
        return BugTracker.builder()
                .projectKey(PROJECT_KEY)
                .type(BugTracker.Type.JIRA)
                .issueType("Bug")
                .build();
    }
}