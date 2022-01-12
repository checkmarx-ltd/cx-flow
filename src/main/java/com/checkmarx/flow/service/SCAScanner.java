package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;

import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.AstScaResults;
import com.checkmarx.sdk.dto.ast.ScanParams;

import com.checkmarx.sdk.exception.CheckmarxException;

import com.checkmarx.sdk.service.scanner.ScaScanner;
import com.checkmarx.sdk.utils.CxRepoFileHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@Service
@Slf4j
public class SCAScanner extends AbstractASTScanner {

    private final ScaProperties scaProperties;
    private final CxRepoFileHelper cxRepoFileHelper;

    public SCAScanner(ScaScanner scaClient, FlowProperties flowProperties, BugTrackerEventTrigger bugTrackerEventTrigger,
                      ScaProperties scaProperties) {
        super(scaClient, flowProperties, ScaProperties.CONFIG_PREFIX, bugTrackerEventTrigger);
        this.scaProperties = scaProperties;
        this.cxRepoFileHelper = new CxRepoFileHelper();
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
        try {
            if (scaProperties.isEnabledZipScan()) {
                log.info("CxAST-SCA zip scan is enabled");
                String scaClonedFolderPath = cxRepoFileHelper.getScaClonedRepoFolderPath(scanRequest.getRepoUrlWithAuth(), scanRequest.getExcludeFiles(), scanRequest.getBranch());
                scanParams.setSourceDir(scaClonedFolderPath);
            }
            if(scanRequest.getExcludeFiles() != null) {
                scanParams.getScaConfig().setExcludeFiles(scanRequest.getExcludeFiles());
            } else if(scaProperties.getExcludeFiles() != null){
                List<String> excludeFiles = new ArrayList<String>(Arrays.asList(scaProperties.getExcludeFiles().split(",")));
                log.debug("Exclude Files list contains : {}", excludeFiles);
                scanParams.getScaConfig().setExcludeFiles(excludeFiles);
            }
        } catch (CheckmarxException e) {
            throw new MachinaRuntimeException(e.getMessage());
        }
    }

}