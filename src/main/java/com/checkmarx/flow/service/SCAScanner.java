package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.OperationStatus;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ZipUtils;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.SCAParams;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.service.ScaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SCAScanner implements VulnerabilityScanner {
    private static final String SCAN_TYPE = ScaProperties.CONFIG_PREFIX;

    private final ScaClient scaClient;
    private final FlowProperties flowProperties;

    @Override
    public ScanResults scan(ScanRequest scanRequest) {
        ScanResults result = null;
        log.info("--------------------- Initiating new {} scan ---------------------", SCAN_TYPE);
        SCAParams internalScaParams = toScaParams(scanRequest);
        SCAResults internalResults = new SCAResults();
        try {
            internalResults = scaClient.scanRemoteRepo(internalScaParams);
            logRequest(scanRequest, internalResults.getScanId(),  OperationResult.successful());
            result = toScanResults(internalResults);
        } catch (Exception e) {
            final String message = "SCA scan failed.";
            log.error(message, e);
            OperationResult scanCreationFailure = new OperationResult(OperationStatus.FAILURE, e.getMessage());
            logRequest(scanRequest, internalResults.getScanId(),  scanCreationFailure);
            throw new MachinaRuntimeException(message);
        }
        return result;
    }

    public ScanResults scan(ScanRequest scanRequest, String path) throws ExitThrowable {
        ScanResults result;
        log.info("--------------------- Initiating new {} scan ---------------------", SCAN_TYPE);
        SCAResults internalResults = new SCAResults();

        try {
            String cxZipFile = FileSystems.getDefault().getPath("cx.".concat(UUID.randomUUID().toString()).concat(".zip")).toAbsolutePath().toString();
            ZipUtils.zipFile(path, cxZipFile, flowProperties.getZipExclude());
            File f = new File(cxZipFile);
            log.debug("Creating temp file {}", f.getPath());
            log.debug("free space {}", f.getFreeSpace());
            log.debug("total space {}", f.getTotalSpace());
            log.debug(f.getAbsolutePath());
            SCAParams internalScaParams = toScaZipParams(scanRequest, cxZipFile);

            internalResults = scaClient.scanLocalSource(internalScaParams);
            logRequest(scanRequest, internalResults.getScanId(),  OperationResult.successful());
            result = toScanResults(internalResults);

            log.debug("Deleting temp file {}", f.getPath());
            Files.deleteIfExists(Paths.get(cxZipFile));

        } catch (Exception e) {
            final String message = "SCA scan failed.";
            log.error(message, e);
            OperationResult scanCreationFailure = new OperationResult(OperationStatus.FAILURE, e.getMessage());
            logRequest(scanRequest, internalResults.getScanId(),  scanCreationFailure);
            throw new MachinaRuntimeException(message);
        }
        return result;
    }

    private void logRequest(ScanRequest request, String scanId, OperationResult scanCreationResult) {
        ScanReport report = new ScanReport(scanId, request,request.getRepoUrl(), scanCreationResult, ScanReport.SCA);
        report.log();
    }    
    
    
    @Override
    public boolean isEnabled() {
        List<String> enabledScanners = flowProperties.getEnabledVulnerabilityScanners();
        return enabledScanners != null && (StringUtils.containsIgnoreCase(enabledScanners.toString(), ScaProperties.CONFIG_PREFIX));
    }

    private ScanResults toScanResults(SCAResults scaResults) {
        return ScanResults.builder()
                .scaResults(scaResults)
                .build();
    }

    private SCAParams toScaParams(ScanRequest scanRequest) {
        URL parsedUrl = getRepoUrl(scanRequest);

        return SCAParams.builder()
                .projectName(scanRequest.getProject())
                .remoteRepoUrl(parsedUrl)
                .build();
    }

    private SCAParams toScaZipParams(ScanRequest scanRequest, String zipPath) {
        return SCAParams.builder()
                .projectName(scanRequest.getProject())
                .zipPath(zipPath)
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
}