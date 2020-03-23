package com.checkmarx.flow.cucumber.component.parse.matchscenario;

import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.component.parse.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

public class CxFlowReportComparer {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final TestContext testContext;

    public CxFlowReportComparer(TestContext testContext) {
        this.testContext = testContext;
    }

    public ComparisonResult compareActualReportsToReferenceReports(Iterable<String> baseFilenames) throws IOException {
        ComparisonResult result = new ComparisonResult();
        for (String baseName : baseFilenames) {
            Mismatch mismatch = compareWithReferenceReport(baseName);
            if (mismatch != null) {
                result.addMismatch(mismatch);
            }
        }
        return result;
    }

    private Mismatch compareWithReferenceReport(String baseName) throws IOException {
        String completeName = String.format("%s.%s", baseName, TestContext.CXFLOW_REPORT_EXTENSION);

        String pathRelativeToData = Paths.get(TestContext.CXFLOW_REPORTS_DIR, completeName).toString();
        JsonNode referenceReport = TestUtils.parseJsonFromResources(pathRelativeToData);
        JsonNode actualReport = parseJsonFromFile(completeName);
        JsonNode differences = JsonDiff.asJson(referenceReport, actualReport);
        Mismatch mismatch = null;
        if (!differences.isEmpty()) {
            mismatch = new Mismatch(baseName, differences);
        }

        return mismatch;
    }

    private JsonNode parseJsonFromFile(String filename) throws IOException {
        File file = Paths.get(testContext.getWorkDir(), filename).toFile();
        JsonNode result;
        result = objectMapper.readTree(file);
        return result;
    }


}
