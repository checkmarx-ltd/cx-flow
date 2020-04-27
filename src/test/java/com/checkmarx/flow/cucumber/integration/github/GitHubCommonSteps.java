package com.checkmarx.flow.cucumber.integration.github;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.SastScannerService;
import com.checkmarx.flow.utils.github.GitHubTestUtilsImpl;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

public abstract class GitHubCommonSteps {

    static final String DEFAULT_TEAM_NAME = "CxServer";
    static final String MASTER_BRANCH_NAME = "master";
    static final String DEFAULT_TEST_NAMESPACE = "cxflowtestuser";

    ScanRequest scanRequest;
    Filter filter;

    @Autowired
    protected CxProperties cxProperties;

    @Autowired
    protected FlowProperties flowProperties;

    @Autowired
    protected FlowService flowService;

    @Autowired
    protected GitHubTestUtilsImpl gitHubTestUtils;

    @Autowired
    protected SastScannerService sastScannerService;

    protected File getFileFromResourcePath(String path) throws IOException {
        return new ClassPathResource(path).getFile();
    }

    protected BugTracker getCustomBugTrackerToGit() {
        return BugTracker.builder()
                .type(BugTracker.Type.CUSTOM)
                .customBean("GitHub")
                .build();
    }
}