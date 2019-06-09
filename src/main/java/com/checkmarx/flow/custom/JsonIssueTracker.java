package com.checkmarx.flow.custom;

import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.ScanResults;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service("Json")
public class JsonIssueTracker implements IssueTracker {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JsonIssueTracker.class);
    private final JsonProperties properties;

    @ConstructorProperties({"properties"})
    public JsonIssueTracker(JsonProperties properties) {
        this.properties = properties;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        String filename = properties.getFileNameFormat();
        filename = ScanUtils.getFilename(request, filename);
        String folder = properties.getDataFolder();
        if(!ScanUtils.empty(folder) && folder.endsWith("/")){
            filename = folder.concat(filename);
        }
        else if(!ScanUtils.empty(folder) && !folder.endsWith("/")){
            filename = folder.concat("/").concat(filename);
        }
        request.setFilename(filename);
        log.info("Creating file {}", filename);
        log.info("Deleting if already exists");
        try {
            Files.deleteIfExists(Paths.get(filename));
            Files.createFile(Paths.get(filename));
        } catch (IOException e){
            log.error("Issue deleting existing file {}", filename);
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writeValue(new File(request.getFilename()), results);

        } catch (IOException e) {
            log.error("Issue occurred while writing file {}", request.getFilename());
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return null;
    }

    @Override
    public List<Issue> getIssues(ScanRequest request) throws MachinaException {
        return null;
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        return null;
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {

    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        return null;
    }

    @Override
    public String getIssueKey(Issue issue, ScanRequest request) {
        return issue.getId();
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        return issue.getFilename();
    }

    @Override
    public boolean isIssueClosed(Issue issue, ScanRequest request) {
        return false;
    }

    @Override
    public boolean isIssueOpened(Issue issue, ScanRequest request) {
        return false;
    }


}