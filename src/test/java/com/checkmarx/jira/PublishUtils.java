package com.checkmarx.jira;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.sdk.config.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

@TestComponent
public class PublishUtils implements IPublishUtils {

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private FlowProperties flowProperties;

    @Autowired
    private FlowService flowService;

    @Override
    public File getFileFromResourcePath(String path) throws IOException {
        return new ClassPathResource(path).getFile();
    }

    @Override
    public BugTracker createJiraBugTracker() {
        return BugTracker.builder()
                .issueType(jiraProperties.getIssueType())
                .projectKey(jiraProperties.getProject())
                .type(BugTracker.Type.JIRA)
                .closedStatus(jiraProperties.getClosedStatus())
                .openTransition(jiraProperties.getOpenTransition())
                .priorities(jiraProperties.getPriorities())
                .openStatus(jiraProperties.getOpenStatus())
                .closeTransition(jiraProperties.getCloseTransition())
                .build();
    }

    @Override
    public void publishRequest(ScanRequest request, File file, BugTracker.Type bugTrackerType) throws ExitThrowable {
        request.setBugTracker(createJiraBugTracker());
        flowProperties.setBugTracker(bugTrackerType.name());
        flowService.cxParseResults(request, file);
    }

    @Override
    public ScanRequest getScanRequestWithDefaults() {
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
