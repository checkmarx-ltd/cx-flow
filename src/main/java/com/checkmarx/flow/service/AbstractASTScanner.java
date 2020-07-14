package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.OperationStatus;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResults;
import com.checkmarx.sdk.dto.ast.ASTResultsWrapper;
import com.checkmarx.sdk.dto.ast.SCAResults;
import com.checkmarx.sdk.dto.ast.ScanParams;
import com.checkmarx.sdk.service.AstClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


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
            logRequest(scanRequest, internalResults.getScaResults().getScanId(),  OperationResult.successful());
            result = toScanResults(internalResults);
        } catch (Exception e) {
            final String message = "SCA scan failed.";
            log.error(message, e);
            OperationResult scanCreationFailure = new OperationResult(OperationStatus.FAILURE, e.getMessage());
            logRequest(scanRequest, internalResults.getScaResults().getScanId(),  scanCreationFailure);
            throw new MachinaRuntimeException(message);
        }
        return result;
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
        return enabledScanners != null && (StringUtils.containsIgnoreCase(enabledScanners.toString(), scanType));
    }
    
    private void logRequest(ScanRequest request, String scanId, OperationResult scanCreationResult) {
        ScanReport report = new ScanReport(scanId, request,request.getRepoUrl(), scanCreationResult, ScanReport.SCA);
        report.log();
    }



   
}
