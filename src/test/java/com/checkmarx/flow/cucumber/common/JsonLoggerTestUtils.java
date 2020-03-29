package com.checkmarx.flow.cucumber.common;

import com.checkmarx.sdk.exception.CheckmarxException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;

public class JsonLoggerTestUtils {
    private JsonNode node;
    private String logAbsolutePath;

    public JsonNode getNode() {
        return node;
    }

    public String getLogAbsolutePath() {
        return logAbsolutePath;
    }

    public JsonNode getOperationNode(String operation) throws CheckmarxException {
        FileInputStream inputStream = null;
        BufferedReader streamReader = null;
        File file = null;
        node = null;
        ObjectMapper objectMapper = new ObjectMapper();
        logAbsolutePath = System.getProperty("LOG_PATH") + File.separator + "CxFlowReport.json";
        try {

            inputStream = new FileInputStream(logAbsolutePath);
            streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

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
            node = objectMapper.readTree(scanRequest).get(operation);
            return node;

        } catch (IOException e) {
            throw new CheckmarxException(e.getMessage());
        } finally {
            try {
                inputStream.close();
                streamReader.close();
                objectMapper = null;
            } catch (Exception e) {
                throw new CheckmarxException(e.getMessage());
            }

        }
        
    }


    public void deleteLoggerContents() throws CheckmarxException {
        //delete file contents
        try {
            new FileOutputStream(logAbsolutePath).close();
        } catch (IOException e) {
            throw new CheckmarxException(e.getMessage());
        }
    }
}
