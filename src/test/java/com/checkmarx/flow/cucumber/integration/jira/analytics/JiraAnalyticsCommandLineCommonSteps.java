package com.checkmarx.flow.cucumber.integration.jira.analytics;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.JsonLoggerTestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.JiraService;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

class JiraAnalyticsCommandLineCommonSteps {

    static final String PROJECT_KEY = "AT1";
    static final String JIRA_URL = "https://cxflow.atlassian.net/";

    @Autowired
    protected CxProperties cxProperties;

    @Autowired
    protected JiraProperties jiraProperties;

    @Autowired
    protected JiraService jiraService;

    @Autowired
    protected FlowProperties flowProperties;

    @Autowired
    protected FlowService flowService;

    @Autowired
    protected IJiraTestUtils jiraUtils;

    @Autowired
    protected JsonLoggerTestUtils jsonLoggerTestUtils;

    protected Filter filter;
    protected BugTracker bugTracker;

    protected void setFilter(String filterValue) {
        filter = Filter.builder()
                .type(Filter.Type.SEVERITY)
                .value(filterValue)
                .build();
    }

    protected File getFileFromResourcePath(String path) throws IOException {
        return new ClassPathResource(path).getFile();
    }

    ScanRequest getBasicScanRequest() {
        return ScanRequest.builder()
                .application("TestApp")
                .product(ScanRequest.Product.CX)
                .project("TestProject")
                .team("CxServer")
                .namespace("Test")
                .repoName("TestRepo")
                .repoUrl("http://localhost/repo.git")
                .repoUrlWithAuth("http://localhost/repo.git")
                .repoType(ScanRequest.Repository.GITHUB)
                .bugTracker(bugTracker)
                .branch("master")
                .refs(Constants.CX_BRANCH_PREFIX.concat("master"))
                .email(null)
                .incremental(false)
                .scanPreset("Checkmarx Default")
                .filters(Collections.singletonList(filter))
                .build();
    }
}