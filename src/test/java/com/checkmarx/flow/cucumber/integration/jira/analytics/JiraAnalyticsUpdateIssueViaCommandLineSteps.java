package com.checkmarx.flow.cucumber.integration.jira.analytics;

import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.report.JiraTicketsReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.sdk.exception.CheckmarxException;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class JiraAnalyticsUpdateIssueViaCommandLineSteps extends JiraAnalyticsCommandLineCommonSteps {

    private static final String OPEN_FINDING_PATH = "cucumber/data/sample-sast-results/2-findings-different-vuln-type-different-files.xml";
    private static final String CLOSE_FINDING_PATH = "cucumber/data/sample-sast-results/1-finding-for-update-close.xml";
    private static final String JIRA_UPDATED_STATE = "updated";
    private static final String JIRA_CLOSED_STATE = "closed";

    @Before("@Jira_Analytics_Update_Issue_Command_Line")
    public void init() throws IOException, ExitThrowable {
        cxProperties.setOffline(true);
        jiraProperties.setUrl(JIRA_URL);
        jiraService.init();

        setFilter("High");

        bugTracker = getBasicBugTrackerToJira();
        flowProperties.setBugTracker(bugTracker.getType().name());
        sastScannerService.cxParseResults(getBasicScanRequest(), getFileFromResourcePath(OPEN_FINDING_PATH));
    }

    @After("@Jira_Analytics_Update_Issue_Command_Line")
    public void tearDown() throws IOException {
        jiraUtils.cleanProject(PROJECT_KEY);
        jsonLoggerTestUtils.clearLogContents();
    }

    @When("updating a new Jira issue via the command line")
    public void closeNewIssueViaCommandLine() throws IOException, ExitThrowable {
        sastScannerService.cxParseResults(getBasicScanRequest(), getFileFromResourcePath(CLOSE_FINDING_PATH));
    }

    @Then("a matching ticket updating data should be recorded in the analytics json file")
    public void validateAnalyticsJsonFile() throws CheckmarxException {
        JiraTicketsReport jiraTicketsReport = (JiraTicketsReport) jsonLoggerTestUtils.getReportNode(JiraTicketsReport.OPERATION, JiraTicketsReport.class);
        HashMap<String, List<String>> jiraTickets = jiraTicketsReport.getJiraTickets();

        int totalUpdatedTickets = jiraTickets.get(JIRA_UPDATED_STATE).size();
        int totalClosedTickets = jiraTickets.get(JIRA_CLOSED_STATE).size();

        Assert.assertEquals("Expected to find 1 new JIRA updated ticket record on analytics logs , but found different count: " + totalUpdatedTickets, 1, totalUpdatedTickets);
        Assert.assertEquals("Actual JIRA updated ticket's project key on analytics logs is different from expected",
                jiraService.getCurrentUpdatedIssuesList(), jiraTickets.get(JIRA_UPDATED_STATE));

        Assert.assertEquals("Expected to find 1 new JIRA closed ticket record on analytics logs , but found different count: " + totalClosedTickets, 1, totalClosedTickets);
        Assert.assertEquals("Actual JIRA closed ticket's project key on analytics logs is different from expected",
                jiraService.getCurrentClosedIssuesList(), jiraTickets.get(JIRA_CLOSED_STATE));
    }

    private BugTracker getBasicBugTrackerToJira() {
        return BugTracker.builder()
                .projectKey(PROJECT_KEY)
                .type(BugTracker.Type.JIRA)
                .issueType("Bug")
                .closedStatus(jiraProperties.getClosedStatus())
                .openStatus(jiraProperties.getOpenStatus())
                .priorities(jiraProperties.getPriorities())
                .closeTransition(jiraProperties.getCloseTransition())
                .build();
    }
}