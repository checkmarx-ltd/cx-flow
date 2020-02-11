package com.checkmarx.flow.cucumber.component.parse;

import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.custom.JsonProperties;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * State that is passed between different step implementation classes for the 'parse' feature.
 */
@Component
public class TestContext {
    public static final String SAST_RESULT_EXTENSION = "xml";
    public static final String CXFLOW_REPORT_EXTENSION = "json";
    public static final String SAMPLE_SAST_RESULTS_DIR = "sample-sast-results";
    public static final String CXFLOW_REPORTS_DIR = "cxflow-reference-reports";
    public static final String CUCUMBER_DATA_DIR = "cucumber/data";

    private final CxFlowRunner cxFlowRunner;

    /**
     * The same instance of JsonProperties is referenced deep inside cxFlowRunner.
     * Changing this instance here allows to affect cxFlowRunner behavior.
     */
    private final JsonProperties jsonProperties;

    private final String workDir;

    /**
     * Using StringBuilder to simplify dynamic filename generation in several steps.
     */
    private final StringBuilder inputFilenameBuilder = new StringBuilder();

    /**
     * Used in scenarios that check error handling.
     */
    private Throwable cxFlowExecutionException;

    /**
     * Used in the 'Match' scenario (verify that generated CxFlow reports match reference reports).
     * Base filenames (i.e. without extension) of sample SAST results that have corresponding reference CxFlow reports.
     */
    private List<String> baseFilenames;


    public TestContext(CxFlowRunner cxFlowRunner, JsonProperties jsonProperties) {
        this.cxFlowRunner = cxFlowRunner;
        this.jsonProperties = jsonProperties;

        String systemTempDir = FileUtils.getTempDirectoryPath();
        String scenarioSubdir = String.format("cxflow-parse-tests-%s", UUID.randomUUID());

        this.workDir = Paths.get(systemTempDir, scenarioSubdir).toString();
        this.jsonProperties.setDataFolder(workDir);
    }

    public void reset() {
        cxFlowExecutionException = null;
        baseFilenames = null;
        inputFilenameBuilder.setLength(0);
    }

    public String getWorkDir() {
        return workDir;
    }

    public String getInputFilename() {
        return inputFilenameBuilder.toString();
    }

    public void setInputFilename(String inputFilename) {
        inputFilenameBuilder.setLength(0);
        inputFilenameBuilder.append(inputFilename);
    }

    public void appendToInputFilename(String value) {
        inputFilenameBuilder.append(value);
    }

    public String getOutputFilename() {
        return jsonProperties.getFileNameFormat();
    }

    public void setOutputFilename(String value) {
        jsonProperties.setFileNameFormat(value);
    }

    public CxFlowRunner getCxFlowRunner() {
        return cxFlowRunner;
    }

    public void setCxFlowExecutionException(Throwable cxFlowExecutionException) {
        this.cxFlowExecutionException = cxFlowExecutionException;
    }

    public Throwable getCxFlowExecutionException() {
        return cxFlowExecutionException;
    }

    public void setBaseFilenames(List<String> baseFilenames) {
        this.baseFilenames = baseFilenames;
    }

    public List<String> getBaseFilenames() {
        return baseFilenames;
    }
}
