package com.checkmarx.flow.cucumber.common;

import com.checkmarx.flow.dto.report.*;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Paths;

import static org.springframework.boot.logging.LoggingSystemProperties.LOG_PATH;

public class JsonLoggerTestUtils {

    public static final String CX_FLOW_REPORT_JSON = "CxFlowReport.json";
    private String logPath;
    private String logAbsolutePath;

    public JsonLoggerTestUtils(){
        logPath = LOG_PATH;
        logAbsolutePath = Paths.get(System.getProperty("LOG_PATH"), CX_FLOW_REPORT_JSON).toString();
    }

    public JsonLoggerTestUtils(String logPath){
        this.logPath = logPath;
        logAbsolutePath = Paths.get(logPath, CX_FLOW_REPORT_JSON).toString();
    }

    public AnalyticsReport getReportNode(String nodeName , Class reportClass) throws CheckmarxException {
        
        ObjectMapper objectMapper = new ObjectMapper(); 
        JsonNode node = getReportNode(nodeName, objectMapper);
        
        return getAnalyticsReport(reportClass, objectMapper, node);

    }

    private AnalyticsReport getAnalyticsReport(Class reportClass, ObjectMapper objectMapper, JsonNode node) throws CheckmarxException {

        objectMapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false);

        try {
            return (AnalyticsReport)objectMapper.readValue(node.toString(), reportClass);
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
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        ){
            boolean moreLines = true;
            String lastLine = streamReader.readLine();
            String nextScanRequest = null;
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

    

    public void deleteLoggerContents() throws CheckmarxException {
        //delete file contents
        try (
                FileOutputStream file = new FileOutputStream(logAbsolutePath);
        )
        {}
        catch (IOException e) {
            throw new CheckmarxException(e.getMessage());
        } 
    }
    
    public static void main(String[] args){

        JsonLoggerTestUtils utils;
        AnalyticsReport reportObject = null;
        
        if(args != null && args.length > 0){
             utils = new JsonLoggerTestUtils(args[0]);
        }else {
             utils = new JsonLoggerTestUtils();
        }
        
        try {

            String lastLine = utils.getLastLine();
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode jsonNode = null;
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
                jsonNode = objectMapper.readTree(lastLine).get(GetResultsReport.OPERATION);
                if (jsonNode != null) {
                     reportObject = utils.getAnalyticsReport(GetResultsReport.class, objectMapper, jsonNode);
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
