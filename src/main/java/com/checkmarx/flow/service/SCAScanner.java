package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.AstScaResults;
import com.checkmarx.sdk.dto.ast.ScanParams;
import com.checkmarx.sdk.dto.cx.CxScanParams;
import com.checkmarx.sdk.exception.CheckmarxException;

import com.checkmarx.sdk.service.CxRepoFileService;

import com.checkmarx.sdk.service.scanner.ScaScanner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@Slf4j
public class SCAScanner extends AbstractASTScanner {

    private final ScaProperties scaProperties;
    private final CxRepoFileService cxRepoFileService;

    public SCAScanner(ScaScanner scaClient, FlowProperties flowProperties, BugTrackerEventTrigger bugTrackerEventTrigger,
                      ScaProperties scaProperties, CxRepoFileService cxRepoFileService) {
        super(scaClient, flowProperties, ScaProperties.CONFIG_PREFIX, bugTrackerEventTrigger);
        this.scaProperties = scaProperties;
        this.cxRepoFileService = cxRepoFileService;
    }

    @Override
    protected ScanResults toScanResults(AstScaResults internalResults) {
        return ScanResults.builder()
                .scaResults(internalResults.getScaResults())
                .build();
    }

    @Override
    protected String getScanId(AstScaResults internalResults) {
        return Optional.ofNullable(internalResults.getScaResults().getScanId()).orElse("");
    }

    @Override
    protected void setScannerSpecificProperties(ScanRequest scanRequest, ScanParams scanParams) {
        if (scaProperties.isEnabledZipScan()) {
            log.info("CxAST-SCA zip scan is enabled");
            String scaZipFolderPath = getScaZipFolderPath(scanRequest);
            scanParams.setZipPath(scaZipFolderPath);
        }
    }

    private String getScaZipFolderPath(ScanRequest scanRequest) {
        CxScanParams cxScanParams = prepareScanParamsToCloneRepo(scanRequest);
        try {
            return cxRepoFileService.prepareRepoFile(cxScanParams);
        } catch (CheckmarxException e) {
            throw new MachinaRuntimeException(e.getMessage());
        }
    }

    private CxScanParams prepareScanParamsToCloneRepo(ScanRequest scanRequest) {
        CxScanParams cxScanParams = new CxScanParams();
        cxScanParams.withGitUrl(scanRequest.getRepoUrlWithAuth());
        cxScanParams.withFileExclude(scanRequest.getExcludeFiles());

        if (StringUtils.isNotEmpty(scanRequest.getBranch())) {
            cxScanParams.withBranch(Constants.CX_BRANCH_PREFIX.concat(scanRequest.getBranch()));
        }
        return cxScanParams;
    }
}