package com.checkmarx.flow.cucumber.integration.github;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.utils.github.GitHubTestUtilsImpl;
import com.checkmarx.sdk.config.CxProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

public abstract class GitHubCommonSteps {

    @Autowired
    protected CxProperties cxProperties;

    @Autowired
    protected FlowProperties flowProperties;

    @Autowired
    protected FlowService flowService;

    @Autowired
    protected GitHubTestUtilsImpl gitHubTestUtils;

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