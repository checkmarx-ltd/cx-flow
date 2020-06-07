package com.checkmarx.flow.cucumber.component.parse;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.cucumber.common.Constants;
import com.google.common.collect.Sets;

import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of steps that specify the location of SAST results.
 */
@SpringBootTest(classes = {CxFlowApplication.class})
public class LoadingSastResultsSteps {
    private static final Map<String, String> sastFilenamesByDescription;
    static {
        Map<String, String> temp = new HashMap<>();
        temp.put("2 findings with the same vulnerability type and in the same file", "2-findings-same-vuln-type-same-file.xml");
        temp.put("2 findings with the same vulnerability type and in different files", "2-findings-same-vuln-type-different-files.xml");
        temp.put("2 findings with different vulnerability types and in the same file", "2-findings-different-vuln-type-same-file.xml");
        temp.put("2 findings with different vulnerability types and in different files", "2-findings-different-vuln-type-different-files.xml");
        sastFilenamesByDescription = Collections.unmodifiableMap(temp);
    }

    private final TestContext testContext;

    public LoadingSastResultsSteps(TestContext testContext) {
        this.testContext = testContext;
    }

    @Given("input with no findings")
    public void setEmptySastScanResults() {
        testContext.setInputFilename("empty-results.xml");
    }

    @Given("input having the following findings: {string}")
    public void setSastScanResults(String description) {
        String sastResultFilename = sastFilenamesByDescription.get(description);
        if (sastResultFilename == null) {
            throw new PendingException("Cannot find scan result file for description: " + description);
        }
        testContext.setInputFilename(sastResultFilename);
    }

    @Given("input containing 3 findings with different severities: Medium, High, Critical")
    public void sastScanResultsContainingFindingsWithDifferentSeverities() {
        testContext.setInputFilename("3-findings-different-severity-medium-high-critical.xml");
    }

    @Given("input has {int} findings with {string} severity")
    public void inputHasFindingsWithSeverity(int findingCount, String severity) {
        String fragment = String.format("%d-%s-", findingCount,
                severity.toLowerCase(Locale.ROOT));

        testContext.appendToInputFilename(fragment);
    }

    @And("each finding has a unique combination of vulnerability type + filename")
    public void eachFindingHasAUniqueCombination() {
        // Assuming this is the last part of the filename.
        String postfix = String.format("unique.%s", TestContext.SAST_RESULT_EXTENSION);
        testContext.appendToInputFilename(postfix);
    }

    @Given("input with {} finding\\(s)")
    public void inputWithANumberOfFindings(int findingCount) {
        String beginning = String.format("%d-finding%s", findingCount, findingCount > 1 ? "s" : "");
        testContext.appendToInputFilename(beginning);
    }

    @And("the execution path of each finding contains {int} nodes")
    public void theExecutionPathOfEachFindingContainsNodes(int nodeCount) {
        String fragment = String.format("-%d-nodes", nodeCount);
        testContext.appendToInputFilename(fragment);
    }

    @And("each of the findings has a different filename")
    public void eachOfTheFindingsHasADifferentFilename() {
        // For 1 finding, the "different filename" statement is obviously always true,
        // but we still use this statement here for consistency.
        String ending = String.format("-different-files.%s", TestContext.SAST_RESULT_EXTENSION);
        testContext.appendToInputFilename(ending);
    }

    @Given("reference CxFlow reports are available for specific inputs")
    public void referenceCxFlowReportsAreAvailable() throws IOException, URISyntaxException {
        List<String> baseFilenames = getSastResultsHavingReferenceReports();
        Assert.assertFalse("No matching reference reports found, nothing to test.", baseFilenames.isEmpty());
        testContext.setBaseFilenames(baseFilenames);
    }

    private List<String> getSastResultsHavingReferenceReports() throws IOException, URISyntaxException {
        Set<String> sampleSastResults = getResourceFilenames(Constants.SAMPLE_SAST_RESULTS_DIR,
                TestContext.SAST_RESULT_EXTENSION);

        Set<String> referenceCxFlowReports = getResourceFilenames(Constants.CXFLOW_REPORTS_DIR,
                TestContext.CXFLOW_REPORT_EXTENSION);

        Sets.SetView<String> result = Sets.intersection(sampleSastResults, referenceCxFlowReports);
        return new ArrayList<>(result);
    }

    private static Set<String> getResourceFilenames(String directory, String extension) throws IOException, URISyntaxException {
        Path resourceDir = getResourceDir(directory);
        Set<String> result;
        if (resourceDir != null) {
            result = getBaseFilenames(extension, resourceDir);
        } else {
            result = new HashSet<>();
        }
        return result;
    }

    private static Set<String> getBaseFilenames(String extension, Path resourceDir) throws IOException {
        int DIRECTORY_SCAN_DEPTH = 1;
        try (Stream<Path> files = Files.find(resourceDir,
                DIRECTORY_SCAN_DEPTH,
                onlyFilesWithExtension(extension))
        ) {
            return files
                    .map(path -> FilenameUtils.getBaseName(path.getFileName().toString()))
                    .collect(Collectors.toSet());
        }
    }

    private static Path getResourceDir(String subdir) throws URISyntaxException {
        Path pathRelativeToResources = Paths.get(Constants.CUCUMBER_DATA_DIR, subdir);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resourceDir = classLoader.getResource(pathRelativeToResources.toString());
        return resourceDir != null ? Paths.get(resourceDir.toURI()) : null;
    }

    private static BiPredicate<Path, BasicFileAttributes> onlyFilesWithExtension(String extension) {
        return (filePath, fileAttr) -> fileAttr.isRegularFile() &&
                FilenameUtils.getExtension(filePath.toString()).equals(extension);
    }
}
