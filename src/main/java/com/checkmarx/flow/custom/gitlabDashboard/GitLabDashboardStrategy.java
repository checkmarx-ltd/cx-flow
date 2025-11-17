package com.checkmarx.flow.custom.gitlabDashboard;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.flow.exception.MachinaException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

public interface GitLabDashboardStrategy {
    void generateSastDashboard(ScanRequest request, ScanResults results) throws MachinaException;
    void generateScaDashboard(ScanRequest request, ScanResults results) throws MachinaException;

    default void writeJsonOutput(ScanRequest request, Object report, Logger log) throws MachinaException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writeValue(new File(request.getFilename()).getCanonicalFile(), report);
        } catch (IOException e) {
            log.error("Issue occurred while writing file {}", request.getFilename(), e);
            throw new MachinaException();
        }
    }
}

