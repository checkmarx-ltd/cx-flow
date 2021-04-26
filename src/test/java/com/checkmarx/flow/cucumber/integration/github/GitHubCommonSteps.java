package com.checkmarx.flow.cucumber.integration.github;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.SastScanner;
import com.checkmarx.flow.utils.github.GitHubTestUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

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
    protected GitHubTestUtils gitHubTestUtils;

    @Autowired
    protected SastScanner sastScanner;

    protected File getFileFromResourcePath(String path) throws IOException {
        return new ClassPathResource(path).getFile();
    }

    protected BugTracker getCustomBugTrackerToGit() {
        return BugTracker.builder()
                .type(BugTracker.Type.CUSTOM)
                .customBean("GitHub")
                .build();
    }

    protected FilterConfiguration getFilterConfiguration() {
        FilterConfiguration result;
        if (filter != null) {
            result = FilterConfiguration.fromSimpleFilters(Collections.singletonList(filter));
        } else {
            result = FilterConfiguration.builder().build();
        }
        return result;
    }
}