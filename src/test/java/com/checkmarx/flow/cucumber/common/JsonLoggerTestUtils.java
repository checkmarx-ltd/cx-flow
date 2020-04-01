package com.checkmarx.flow.cucumber.common;

import com.checkmarx.flow.dto.report.*;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestComponent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@TestComponent
public class JsonLoggerTestUtils {

    public static final String CX_FLOW_REPORT_JSON = "CxFlowReport.json";
    private String logAbsolutePath;

    public JsonLoggerTestUtils() {
        logAbsolutePath = Paths.get(System.getProperty("LOG_PATH"), CX_FLOW_REPORT_JSON).toString();
    }

    public JsonLoggerTestUtils(String logPath) {
        logAbsolutePath = Paths.get(logPath, CX_FLOW_REPORT_JSON).toString();
    }

    public AnalyticsReport getReportNode(String nodeName, Class reportClass) throws CheckmarxException {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = getReportNode(nodeName, objectMapper);

        return getAnalyticsReport(reportClass, objectMapper, node);

    }

    private AnalyticsReport getAnalyticsReport(Class reportClass, ObjectMapper objectMapper, JsonNode node) throws CheckmarxException {

        objectMapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false);

        try {
            return (AnalyticsReport) objectMapper.readValue(node.toString(), reportClass);
        } catch (IOException e) {
            throw new CheckmarxException(e.getMessage());
        }
    }


    public JsonNode getReportNode(String nodeName) throws CheckmarxException {
        return getReportNode(nodeName, new ObjectMapper());
    }

    private JsonNode getReportNode(String nodeName, ObjectMapper objectMapper) throws CheckmarxException {

        String lastLine = getLastLine();
        try {
            return objectMapper.readTree(lastLine).get(nodeName);
        } catch (JsonProcessingException e) {
            throw new CheckmarxException(e.getMessage());
        }

    }

    private String getLastLine() throws CheckmarxException {

        try (
                FileInputStream inputStream = new FileInputStream(logAbsolutePath);
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        ) {
            boolean moreLines = true;
            String lastLine = streamReader.readLine();
            String nextScanRequest;
            while (moreLines) {
                nextScanRequest = streamReader.readLine();
                if (nextScanRequest != null) {
                    lastLine = nextScanRequest;
                } else {
                    moreLines = false;
                }
            }

            return lastLine;

        } catch (IOException e) {
            throw new CheckmarxException(e.getMessage());
        }

    }


    public void clearLogContents() throws IOException {
        // Unable to delete the file itself, because it is locked by the logger.
        // To be able to delete the file, we'll need to use AsyncAppender to logger config.
        // But it would be an overkill to use it just because of tests.
        try (FileOutputStream ignored = new FileOutputStream(logAbsolutePath)) {
            // Nothing to do here.
        }
    }

    public static void main(String[] args) {

        JsonLoggerTestUtils utils;
        AnalyticsReport reportObject = null;

        if (args != null && args.length > 0) {
            utils = new JsonLoggerTestUtils(args[0]);
        } else {
            utils = new JsonLoggerTestUtils();
        }

        try {

            String lastLine = utils.getLastLine();
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode jsonNode;
            jsonNode = objectMapper.readTree(lastLine).get(JiraTicketsReport.OPERATION);

            if (jsonNode != null) {
                reportObject = utils.getAnalyticsReport(JiraTicketsReport.class, objectMapper, jsonNode);
            }
            if (reportObject == null) {
                jsonNode = objectMapper.readTree(lastLine).get(ScanReport.OPERATION);
                if (jsonNode != null) {
                    reportObject = utils.getAnalyticsReport(ScanReport.class, objectMapper, jsonNode);
                }
            }
            if (reportObject == null) {
                jsonNode = objectMapper.readTree(lastLine).get(ScanResultsReport.OPERATION);
                if (jsonNode != null) {
                    reportObject = utils.getAnalyticsReport(ScanResultsReport.class, objectMapper, jsonNode);
                }
            }
            if (reportObject == null) {
                jsonNode = objectMapper.readTree(lastLine).get(PullRequestReport.OPERATION);

                if (jsonNode != null) {
                    reportObject = utils.getAnalyticsReport(PullRequestReport.class, objectMapper, jsonNode);
                }
            }

            System.out.println(reportObject);

        } catch (CheckmarxException | JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
