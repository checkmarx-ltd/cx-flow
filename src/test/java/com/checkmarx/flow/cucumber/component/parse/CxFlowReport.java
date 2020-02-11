package com.checkmarx.flow.cucumber.component.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CxFlowReport {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    private final JsonNode report;

    private CxFlowReport(JsonNode report) {
        this.report = report;
    }

    public static CxFlowReport parse(TestContext testContext) throws IOException {
        Path reportAbsolutePath = Paths.get(testContext.getWorkDir(), testContext.getOutputFilename());
        File reportFile = reportAbsolutePath.toFile();
        JsonNode reportJson = objectMapper.readTree(reportFile);
        return new CxFlowReport(reportJson);
    }

    public static int getDetailCount(JsonNode issue) {
        return issue.get("details").size();
    }

    public static int getResultCount(JsonNode issue) {
        return issue.at("/additionalDetails/results").size();

    }

    public JsonNode getIssues() {
        return report.get("xissues");
    }

    public JsonNode getSummary() {
        return report.at("/additionalDetails/flow-summary");
    }

    public Map<String, JsonNode> getResultMapByFilename() {
        JsonNode issues = getIssues();

        return StreamSupport.stream(issues.spliterator(), false)
                .collect(Collectors.toMap(
                        issue -> issue.get("filename").asText(),
                        issue -> issue.at("/additionalDetails/results").get(0)));
    }

    public String[] getSeverities() {
        return report.findValues("severity")
                .stream()
                .map(JsonNode::textValue)
                .toArray(String[]::new);
    }

    public Integer getSeverityCounterFromSummary(String severity) {
        JsonNode summary = getSummary();
        Integer result =null;
        if (summary.has(severity)) {
            result = summary.get(severity).asInt();
        }
        return result;
    }
}
