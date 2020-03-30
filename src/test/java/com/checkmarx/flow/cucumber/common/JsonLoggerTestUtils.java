package com.checkmarx.flow.cucumber.common;

import com.checkmarx.flow.dto.report.AnalyticsReport;
import com.checkmarx.sdk.exception.CheckmarxException;
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
        logAbsolutePath = System.getProperty(logPath) + File.separator + "CxFlowReport.json";
    }

    public AnalyticsReport getReportNode(String nodeName , Class reportClass) throws CheckmarxException {
        
        ObjectMapper objectMapper = new ObjectMapper(); 
        JsonNode node = getReportNode(nodeName, objectMapper);

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
        
         try (

            FileInputStream inputStream = new FileInputStream(logAbsolutePath);
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        ){
            boolean moreLines = true;
            String scanRequest = streamReader.readLine();
            String nextScanRequest = null;
            while (moreLines) {
                nextScanRequest = streamReader.readLine();
                if (nextScanRequest != null) {
                    scanRequest = nextScanRequest;
                } else {
                    moreLines = false;
                }
            }

            return objectMapper.readTree(scanRequest).get(nodeName);

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
    
}
