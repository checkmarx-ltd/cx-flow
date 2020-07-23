package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.OperationStatus;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ZipUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResults;
import com.checkmarx.sdk.dto.ast.ASTResultsWrapper;
import com.checkmarx.sdk.dto.ast.SCAResults;
import com.checkmarx.sdk.dto.ast.ScanParams;
import com.checkmarx.sdk.service.AstClient;
import lombok.extern.slf4j.Slf4j;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;


@Slf4j
public abstract class AbstractASTScanner  implements VulnerabilityScanner{
    
    private String scanType ;
    private com.checkmarx.sdk.service.AstClient client;
    private FlowProperties flowProperties;
    
    public AbstractASTScanner() {
    }

    public AbstractASTScanner(AstClient astClient, FlowProperties flowProperties, String scanType) {
        this.client = astClient;
        this.flowProperties = flowProperties;
        this.scanType = scanType;
    }

    public ScanResults scan(ScanRequest scanRequest) {
        ScanResults result = null;
        log.info("--------------------- Initiating new {} scan ---------------------", scanType);
        ScanParams internalScaParams = toParams(scanRequest);
        ASTResultsWrapper internalResults = new ASTResultsWrapper(new SCAResults(), new ASTResults());
        try {
            internalResults = client.scanRemoteRepo(internalScaParams);
            logRequest(scanRequest, getScanId(internalResults),  OperationResult.successful());
            result = toScanResults(internalResults);
        } catch (Exception e) {
            return treatError(scanRequest, internalResults, e);
        }
        return result;
    }

    private ScanResults treatError(ScanRequest scanRequest, ASTResultsWrapper internalResults, Exception e) {
        final String message = scanType + " scan failed.";
        log.error(message, e);
        OperationResult scanCreationFailure = new OperationResult(OperationStatus.FAILURE, e.getMessage());
        logRequest(scanRequest, getScanId(internalResults),  scanCreationFailure);
        throw new MachinaRuntimeException(message);
    }

    public ScanResults scan(ScanRequest scanRequest, String path) throws ExitThrowable {
        ScanResults result;
        log.info("--------------------- Initiating new {} scan ---------------------", scanType);
        ASTResultsWrapper internalResults = new ASTResultsWrapper(new SCAResults(), new ASTResults());

        try {
            String cxZipFile = FileSystems.getDefault().getPath("cx.".concat(UUID.randomUUID().toString()).concat(".zip")).toAbsolutePath().toString();
            ZipUtils.zipFile(path, cxZipFile, flowProperties.getZipExclude());
            File f = new File(cxZipFile);
            log.debug("Creating temp file {}", f.getPath());
            log.debug("free space {}", f.getFreeSpace());
            log.debug("total space {}", f.getTotalSpace());
            log.debug(f.getAbsolutePath());
            ScanParams internalScaParams = toParams(scanRequest, cxZipFile);

            internalResults = client.scanLocalSource(internalScaParams);
            logRequest(scanRequest, getScanId(internalResults),  OperationResult.successful());
            result = toScanResults(internalResults);

            log.debug("Deleting temp file {}", f.getPath());
            Files.deleteIfExists(Paths.get(cxZipFile));

        } catch (Exception e) {
            return treatError(scanRequest, internalResults, e);
        }
        return result;
    }

    protected abstract String getScanId(ASTResultsWrapper internalResults) ;

    private ScanParams toParams(ScanRequest scanRequest, String zipPath) {
        return ScanParams.builder()
                .projectName(scanRequest.getProject())
                .zipPath(zipPath)
                .build();
    }
    
    protected abstract ScanResults toScanResults(ASTResultsWrapper internalResults);


    protected ScanParams toParams(ScanRequest scanRequest) {
        URL parsedUrl = getRepoUrl(scanRequest);

        return ScanParams.builder()
                .projectName(scanRequest.getProject())
                .remoteRepoUrl(parsedUrl)
                .build();
    }




    private URL getRepoUrl(ScanRequest scanRequest) {
        URL parsedUrl;
        try {
            parsedUrl = new URL(scanRequest.getRepoUrlWithAuth());
        } catch (MalformedURLException e) {
            log.error("Failed to parse repository URL: '{}'", scanRequest.getRepoUrl());
            throw new MachinaRuntimeException("Invalid repository URL.");
        }
        return parsedUrl;
    }
    
    @Override
    public boolean isEnabled() {
        List<String> enabledScanners = flowProperties.getEnabledVulnerabilityScanners();
        
        return enabledScanners != null 
                && enabledScanners.stream().anyMatch(scanner -> scanner.equalsIgnoreCase(scanType));
 
    }
    
    private void logRequest(ScanRequest request, String scanId, OperationResult scanCreationResult) {
        ScanReport report = new ScanReport(scanId, request,request.getRepoUrl(), scanCreationResult, ScanReport.SCA);
        report.log();
    }



   
}
