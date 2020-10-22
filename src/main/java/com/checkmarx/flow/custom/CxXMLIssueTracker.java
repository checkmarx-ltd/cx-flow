package com.checkmarx.flow.custom;

import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.flow.utils.JAXBHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.SCAResults;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service("CxXml")
@RequiredArgsConstructor
public class CxXMLIssueTracker extends ImmutableIssueTracker {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CxXMLIssueTracker.class);
    private final CxXMLProperties properties;
    private final FilenameFormatter filenameFormatter;

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        String filename = filenameFormatter.formatPath(request, properties.getFileNameFormat(), properties.getDataFolder());
        request.setFilename(filename);
        log.info("Creating file {}, Deleting if already exists", filename);
        try {
            Files.deleteIfExists(Paths.get(filename));
            Files.createDirectories(Paths.get(properties.getDataFolder()));
            Files.createFile(Paths.get(filename));
        } catch (IOException e){
            log.error("Issue deleting or creating file {}", filename,e);
        }
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        try {
            if (!ScanUtils.empty(results.getOutput())) {
                Files.write(Paths.get(request.getFilename()), results.getOutput().getBytes());
            }

            marshalScaResults(request, results);

        } catch (IOException | JAXBException e) {
            log.error("Issue occurred while writing file: {} with error message: {}", request.getFilename(), e.getMessage());
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

    private void marshalScaResults(ScanRequest request, ScanResults results) throws JAXBException {
        SCAResults scaResults = results.getScaResults();
        if (scaResults != null) {
            JAXBHelper.convertObjectIntoXml(scaResults, request.getFilename());
        }
    }
}