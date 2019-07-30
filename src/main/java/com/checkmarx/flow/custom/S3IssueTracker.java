package com.checkmarx.flow.custom;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.ScanResults;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Profile("s3")
@Service("S3")
public class S3IssueTracker implements IssueTracker {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(S3IssueTracker.class);
    private final S3Properties properties;
    private final AmazonS3 s3Client;

    public S3IssueTracker(S3Properties properties, AmazonS3 s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        if(request != null) {
            String folder = properties.getDataFolder();
            String filename = "cx.".concat(UUID.randomUUID().toString());
            if (!ScanUtils.empty(folder) && folder.endsWith("/")) {
                filename = folder.concat(filename);
            } else if (!ScanUtils.empty(folder) && !folder.endsWith("/")) {
                filename = folder.concat("/").concat(filename);
            }
            request.setFilename(filename);
            log.info("Creating file {}", filename);
            log.info("Deleting if already exists");
            try {
                Files.deleteIfExists(Paths.get(filename));
                Files.createFile(Paths.get(filename));
            } catch (IOException e) {
                log.error("Issue deleting existing file {}", filename);
                log.error(ExceptionUtils.getStackTrace(e));
            }
        } else {
            log.error("Filename or Request is not set");
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
                String bucket = request.getAdditionalMetadata("result_bucket");
                String key = request.getAdditionalMetadata("result_key");
                String filename = request.getFilename();
                if(ScanUtils.anyEmpty(bucket, key, filename)){
                    log.error("result_bucket | result_key | temporary file was massing from the ScanRequest metadata");
                    throw new MachinaException();
                }
                File resultFile = new File(filename);
                log.info("Saving {} to S3 bucket {} with key {}", filename, bucket, key);
                s3Client.putObject(bucket, key, resultFile);
                log.info("Save successful");
            } else {
                log.error("No request or results provided");
                throw new MachinaException();
            }

        } catch (IOException e) {
            log.error("Issue occurred while writing file {}", request.getFilename());
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException();
        } catch (AmazonClientException e){
            log.error("AWS Exception occurred: {}", ExceptionUtils.getMessage(e));
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
        log.debug("Noop");
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