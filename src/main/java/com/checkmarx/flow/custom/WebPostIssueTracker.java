package com.checkmarx.flow.custom;

import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service("Web")
public class WebPostIssueTracker implements IssueTracker {
    private static final Logger log = LoggerFactory.getLogger(WebPostIssueTracker.class);
    private final WebPostProperties properties;
    private final RestTemplate restTemplate;
    private final FilenameFormatter filenameFormatter;

    public WebPostIssueTracker(WebPostProperties properties,
                               @Qualifier("flowRestTemplate") RestTemplate restTemplate,
                               FilenameFormatter filenameFormatter) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.filenameFormatter = filenameFormatter;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        if(request != null) {
            String initialFilename = "cx.".concat(UUID.randomUUID().toString());
            String filename = filenameFormatter.format(request, initialFilename, properties.getDataFolder());
            request.setFilename(filename);
            log.info("Creating file {}", filename);
            log.info("Deleting if already exists");
            try {
                Files.deleteIfExists(Paths.get(filename));
                Files.createFile(Paths.get(filename));
            } catch (IOException e) {
                log.error("Issue deleting existing file {}", filename, e);
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
                String resultUrl = request.getAdditionalMetadata("result_url");
                String filename = request.getFilename();
                if(ScanUtils.anyEmpty(resultUrl, filename)){
                    log.error("result_url | temporary file was massing from the ScanRequest metadata");
                    throw new MachinaException();
                }
                File resultFile = new File(filename);
                log.info("Saving file {} to signed web url", filename);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<byte[]> entity = new HttpEntity<>(Files.readAllBytes(Paths.get(resultFile.getCanonicalPath())), headers);
                URI uri = new URI(resultUrl);
                restTemplate.put(uri, entity);
                log.info("Save successful");
            } else {
                log.error("No request or results provided");
                throw new MachinaException();
            }

        } catch (IOException e) {
            log.error("Issue occurred while writing file {}", request.getFilename(), e);
            throw new MachinaException();
        }catch (URISyntaxException e){
            log.error("Error occurred: {}", ExceptionUtils.getMessage(e), e);
            throw new MachinaException();
        }catch (HttpClientErrorException e){
            log.error("HttpClientErrorException occurred: {}", ExceptionUtils.getMessage(e), e);
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