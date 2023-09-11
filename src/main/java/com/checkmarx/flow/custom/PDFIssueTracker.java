package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.service.CxAuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service("PDF")
@RequiredArgsConstructor
public class PDFIssueTracker implements IssueTracker{

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PDFIssueTracker.class);
    private final PDFProperties properties;
    private final FilenameFormatter filenameFormatter;
    private final FlowProperties flowProperties;
    private final CxAuthService authClient;
    private final RestTemplate restTemplate;
    private final CxProperties cxProperties;



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
                    log.error("Issue while deleting or creating file {}", filename,e);
                }
            } else {
                log.error("Filename or Request is not set.");
                throw new MachinaException();
            }
        } else {
            log.error("Properties are not set");
            throw new MachinaException();
        }
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        List<String> vulnerabilityScannerList = flowProperties.getEnabledVulnerabilityScanners();
        for (String vulnerabilityScanner: vulnerabilityScannerList) {
            if(vulnerabilityScanner.equalsIgnoreCase("sca")){
                log.info("Downloading PDF Report for {}",vulnerabilityScanner);
                downloadSCAReport();
            }
            if(vulnerabilityScanner.equalsIgnoreCase("sast")){
                log.info("Downloading PDF Report for {}",vulnerabilityScanner);
                downloadSASTReport(results);
            }
        }
    }

    public void downloadSASTReport(ScanResults results){
//        String strJSON = "{'reportType':'%s','scanId':'%s'}";
//        strJSON = String.format(strJSON, "PDF", results.getSastScanId());
//        HttpEntity requestEntity = new HttpEntity<>(strJSON, authClient.createAuthHeaders());
//
//        try {
//            log.info("Updating details for project {} with id {}", cxProject.getName(), cxProject.getId());
//            restTemplate.exchange(cxProperties.getUrl().concat(PROJECT), HttpMethod.PATCH, requestEntity, String.class, results.getSastScanId());
//        } catch (HttpStatusCodeException e) {
//            log.debug(ExceptionUtils.getStackTrace(e));
//            log.error("Error occurred while updating details for project.", e.printStackTrace());
//        }


    }
    public void downloadSCAReport(){

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
        return null;
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        return null;
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
