package com.checkmarx.flow.custom;

import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service("Json")
@RequiredArgsConstructor
public class JsonIssueTracker implements IssueTracker {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JsonIssueTracker.class);
    private final JsonProperties properties;
    private final FilenameFormatter filenameFormatter;

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        if (properties != null) {
            if(request != null) {
                String filename = filenameFormatter.formatPath(request, properties.getFileNameFormat(), properties.getDataFolder());

                request.setFilename(filename);
                log.info("Creating file {}, Deleting if already exists", filename);
                try {
                    Files.deleteIfExists(Paths.get(filename));
                    Files.createDirectories(Paths.get(properties.getDataFolder()));
                    Files.createFile(Paths.get(filename));
                } catch (IOException e) {
                    log.error("Issue deleting or creating file {}", filename,e);
                }
            } else {
                log.error("Filename or Request is not set");
                throw new MachinaException();
            }
        } else {
            log.error("Properties are not set");
            throw new MachinaException();
        }
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            if(request != null && results != null) {
                mapper.writeValue(new File(request.getFilename()), results);
            } else {
                log.error("No request or results provided");
                throw new MachinaException();
            }

        } catch (IOException e) {
            log.error("Issue occurred while writing file {}", request.getFilename(), e);
            throw new MachinaException();
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
        return issue != null ? issue.getId() : "";
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        return issue != null ? issue.getFilename(): "";
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