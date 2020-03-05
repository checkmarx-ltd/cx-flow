package com.checkmarx.flow.cucumber.component.parse;

import com.checkmarx.flow.cucumber.common.Constants;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.utils.TestsParseUtils;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.util.Strings;
import org.junit.Assert;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation for steps that run CxFlow with different arguments.
 */
public class RunningCxFlowSteps {
    private final TestContext testContext;

    public RunningCxFlowSteps(TestContext testContext) {
        this.testContext = testContext;
    }

    @When("parsing the input")
    public void runParse() throws InvocationTargetException, IOException {
        runCxFlow(testContext.getInputFilename(), null, null);
    }

    @When("parsing the input with severity filter: {}")
    public void parsingTheResultsWithSeverityFilter(String severities)
            throws InvocationTargetException, IOException {

        String filterArgs = getCommandLineFilterArgs(severities);
        runCxFlow(testContext.getInputFilename(), null, filterArgs);
    }

    @When("parsing each of these inputs")
    public void parsingEachOfTheseResults() throws InvocationTargetException, IOException {
        for (String baseFilename : testContext.getBaseFilenames()) {
            String inputFilename = String.format("%s.%s", baseFilename, TestContext.SAST_RESULT_EXTENSION);
            String outputFilename = String.format("%s.%s", baseFilename, TestContext.CXFLOW_REPORT_EXTENSION);
            runCxFlow(inputFilename, outputFilename, null);
        }
    }

    @When("running CxFlow with command line: {string}")
    public void runningCxFlowWithCommandLineCommandLine(String commandLine) {
        Throwable exception = null;
        try {
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), commandLine);
        } catch (Throwable e) {
            exception = e;
        }
        testContext.setCxFlowExecutionException(exception);
    }

    @Then("CxFlow exits with exit code {}")
    public void cxflowExitsWithStatusCodeStatusCode(int expectedExitCode) {
        Throwable exception = testContext.getCxFlowExecutionException();

        Assert.assertNotNull("Expected an exception to be thrown.", exception);
        Assert.assertEquals(InvocationTargetException.class, exception.getClass());

        Throwable targetException = ((InvocationTargetException) exception).getTargetException();
        Assert.assertTrue(targetException instanceof ExitThrowable);

        int actualExitCode = ((ExitThrowable) targetException).getExitCode();
        Assert.assertEquals(expectedExitCode, actualExitCode);
    }

    private static String getCommandLineFilterArgs(String severities) {
        List<String> additionalArgs = TestsParseUtils.parseCSV(severities)
                .map(severity -> "--severity=" + severity)
                .collect(Collectors.toList());
        return Strings.join(additionalArgs).with(" ");
    }

    private Path copyInputResourceToWorkDir(String inputFilename) throws IOException {
        String srcResourcePath = Paths.get(Constants.SAMPLE_SAST_RESULTS_DIR, inputFilename).toString();
        try (InputStream srcStream = TestUtils.getResourceAsStream(srcResourcePath)) {
            if (srcStream == null) {
                throw new IOException("Unable to get resource: " + srcResourcePath);
            }

            Path targetPath = Paths.get(testContext.getWorkDir(), inputFilename);
            Files.copy(srcStream, targetPath);
            return targetPath;
        }
    }

    private void runCxFlow(String inputFilename, @Nullable String outputFilename, @Nullable String customCommandLineArgs)
            throws InvocationTargetException, IOException {
        Path inputFilePath = copyInputResourceToWorkDir(inputFilename);

        if (outputFilename == null) {
            outputFilename = String.format("report-%s.%s", UUID.randomUUID(), TestContext.CXFLOW_REPORT_EXTENSION);
        }
        testContext.setOutputFilename(outputFilename);

        String commandLineArgs = String.format("--parse --offline --app=ABC --f=%s %s",
                inputFilePath,
                customCommandLineArgs);

        TestUtils.runCxFlow(testContext.getCxFlowRunner(), commandLineArgs);
    }
}
