package com.checkmarx.flow.cucumber.component.parse;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Hooks {
    private static final Logger log = LoggerFactory.getLogger(Hooks.class);

    private final TestContext testContext;

    public Hooks(TestContext context) {
        testContext = context;
    }

    @Before("@ParseFeature")
    public void beforeEachScenario() {
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
