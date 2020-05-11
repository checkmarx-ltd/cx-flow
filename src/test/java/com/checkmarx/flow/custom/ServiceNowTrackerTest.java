package com.checkmarx.flow.custom;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.sdk.dto.ScanResults;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = "github")
@SpringBootTest(classes = {CxFlowApplication.class})
public class ServiceNowTrackerTest {
    private static final Logger log = LoggerFactory.getLogger(ServiceNowTrackerTest.class);

    @Autowired
    @Qualifier("ServiceNow")
    private IssueTracker issueTracker;

    @Test
    public void createAndSearchIssues_WithAllParams_Success(){
        try {
            log.info("Start createAndSearchIssues_WithAllParams_Success");

            ScanResults.XIssue issue = getIssue("create test incident");
            ScanRequest request = getRequest();

            issueTracker.init(request, null);
            Issue newIssue = issueTracker.createIssue(issue, request);
            assertNotNull(newIssue);
        } catch (MachinaException e) {
            log.error(e.getMessage());
        }
    }

    @Test
    public void searchAndUpdateIssues_WithAllParams_Success(){
        try {
            log.info("Start searchAndUpdateIssues_WithAllParams_Success");
            // find an existing issue
            ScanRequest request = getRequest();

            List<Issue> issues = issueTracker.getIssues(request);
            assertNotNull(issues);
            assertTrue(issues.size() > 0);
            assertNotNull(issues.get(0).getId());

            ScanResults.XIssue resultIssue = getIssue("update test incident");
            request.setId(issues.get(0).getId());

            // update existing issue
            Issue upatedIssue = issueTracker.updateIssue(issues.get(0), resultIssue, request);
            assertNotNull(upatedIssue);
        } catch (MachinaException e) {
            log.error(e.getMessage());
        }
    }

    @Test
    public void searchAndCloseIssues_WithAllParams_Success(){
        try {
            log.info("Start searchAndCloseIssues_WithAllParams_Success");
            // find an existing issue
            ScanRequest request = getRequest();

            List<Issue> issues = issueTracker.getIssues(request);
            assertNotNull(issues);
            assertTrue(issues.size() > 0);
            assertNotNull(issues.get(0).getId());

            request.setId(issues.get(0).getId());
            // close an existing issue
            issueTracker.closeIssue(issues.get(0), request);

            // check closed issue
            issues = issueTracker.getIssues(request);
            assertNotNull(issues);
            assertTrue(issues.size() > 0);
            assertTrue(issues.get(0).getState().equals("7"));
        } catch (MachinaException e) {
            log.error(e.getMessage());
        }
    }

    private ScanRequest getRequest() {
        return ScanRequest.builder()
                .application("test_app")
                .repoName("test_repo")
                .namespace("checkmarx")
                .product(ScanRequest.Product.CX)
                .branch("develop").build();
    }

    private ScanResults.XIssue getIssue(String description) {
        return ScanResults.XIssue.builder()
                .description(description)
                .vulnerability("test vulnerability")
                .file("test file")
                .severity("3").build();
    }
}
