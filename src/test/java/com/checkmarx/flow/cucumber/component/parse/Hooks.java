package com.checkmarx.flow.cucumber.component.parse;

import com.checkmarx.flow.config.properties.FlowProperties;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;

@RequiredArgsConstructor
public class Hooks {
    private static final Logger log = LoggerFactory.getLogger(Hooks.class);

    private final TestContext testContext;
    private final FlowProperties flowProperties;

    @Before("@ParseFeature")
    public void beforeEachScenario() {
        // Other component tests may change flowProperties.
        // Reset these changes to the values as in application.yml to prevent the current test from failing.
        flowProperties.setFilterScript(null);
        flowProperties.setFilterSeverity(Collections.singletonList("High"));

        tryCreateWorkDir();
        testContext.reset();
    }

    @After("@ParseFeature")
    public void afterEachScenario() {
        tryDeleteWorkDir();
    }

    private void tryCreateWorkDir() {
        File workDir = new File(testContext.getWorkDir());
        if (!workDir.mkdir()) {
            log.warn("Failed to create work directory: {}", workDir);
        }
    }

    private void tryDeleteWorkDir() {
        try {
            File workDir = new File(testContext.getWorkDir());
            FileUtils.deleteDirectory(workDir);
        } catch (Exception e) {
            log.warn("Failed to delete work directory: {}", testContext.getWorkDir());
        }
    }
}
