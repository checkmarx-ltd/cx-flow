package com.checkmarx.flow.custom;

import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class ImmutableIssueTracker implements IssueTracker {
    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {

    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return null;
    }

    @Override
    public List<Issue> getIssues(ScanRequest request) throws MachinaException {
        return new ArrayList<>();
    }

    @Override
    public Issue createIssue(ScanResults.XIssue issue, ScanRequest request) throws MachinaException {
        return null;
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
        return ScanUtils.isSAST(issue)
                ? issue.getFilename()
                : ScanUtils.getScaSummaryIssueKey(request, issue);
    }

    @Override
    public boolean isIssueClosed(Issue issue, ScanRequest request) {
        return false;
    }

    @Override
    public boolean isIssueOpened(Issue issue, ScanRequest request) {
        return false;
    }

    /**
     * Common function for initializing file based bug trackers
     */
    public void fileInit(ScanRequest request, ScanResults results, String filePath, FilenameFormatter filenameFormatter, Logger log) throws MachinaException {
        Path fullPath = Paths.get(filePath);
        Path parentDir = fullPath.getParent();
        Path filename = fullPath.getFileName();
        if(request == null || results == null){
            throw new MachinaException("Request or Results object is missing");
        }
        String formattedPath = filenameFormatter.formatPath(request, filename.toString(), parentDir.toString());
        request.setFilename(formattedPath);
        log.info("Creating file {}", formattedPath);
        try {
            Files.deleteIfExists(Paths.get(formattedPath));
            Files.createFile(Paths.get(formattedPath));
        } catch (IOException e) {
            log.error("Issue deleting existing file or writing initial {}", filename, e);
        }
    }

    /**
     * Common function for writing POJO to Json file output using mapper
     */
    public void writeJsonOutput(ScanRequest request, Object report, Logger log) throws MachinaException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writeValue(new File(request.getFilename()), report);
        } catch (IOException e) {
            log.error("Issue occurred while writing file {}", request.getFilename(), e);
            throw new MachinaException();
        }
    }

}
