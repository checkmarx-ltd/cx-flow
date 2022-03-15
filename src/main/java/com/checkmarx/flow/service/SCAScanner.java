package com.checkmarx.flow.service;

import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.dto.ExitCode;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;

import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.RestClientConfig;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.AstScaResults;
import com.checkmarx.sdk.dto.ast.ScanParams;

import com.checkmarx.sdk.exception.CheckmarxException;

import com.checkmarx.sdk.service.scanner.ScaScanner;
import com.checkmarx.sdk.utils.CxRepoFileHelper;
import com.checkmarx.sdk.utils.scanner.client.IScanClientHelper;
import com.checkmarx.sdk.utils.scanner.client.ScaClientHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.checkmarx.flow.exception.ExitThrowable.exit;


@Service
@Slf4j
public class SCAScanner extends AbstractASTScanner {

    private final ScaProperties scaProperties;
    private final CxRepoFileHelper cxRepoFileHelper;
    @Autowired
    ScaScanner scaScannerClient;

    public SCAScanner(ScaScanner scaClient, FlowProperties flowProperties, BugTrackerEventTrigger bugTrackerEventTrigger,
                      ScaProperties scaProperties,ResultsService resultsService) {
        super(scaClient, flowProperties, ScaProperties.CONFIG_PREFIX, bugTrackerEventTrigger,resultsService);
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
    protected void cxParseResults(ScanRequest scanRequest, File file) throws ExitThrowable {
        RestClientConfig restClientConfig;
        IScanClientHelper iScanClientHelper;

        try {
            ScanParams sdkScanParams = ScanParams.builder()
                    .projectName(scanRequest.getProject())
                    .scaConfig(scanRequest.getScaConfig())
                    .filterConfiguration(scanRequest.getFilter())
                    .build();

            restClientConfig=scaScannerClient.getScanConfig(sdkScanParams);

            iScanClientHelper=new ScaClientHelper(restClientConfig,log,scaProperties);
            ScanResults results =iScanClientHelper.getReportContent(file, scanRequest.getFilter());
            resultsService.processResults(scanRequest, results, scanDetails);
            if (flowProperties.isBreakBuild() && results != null && results.getXIssues() != null && !results.getXIssues().isEmpty()) {
                log.error(ERROR_BREAK_MSG);
                exit(ExitCode.BUILD_INTERRUPTED);
            }
        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred while processing results file", e);
            exit(3);
        }
    }

    @Override
    protected void setScannerSpecificProperties(ScanRequest scanRequest, ScanParams scanParams) {
        try {
//            If bugtracker is not empty and type is CxXML then set
//            preserveXml to true, as it is needed to retrieve sca
//            report in xml format
            if(!ScanUtils.empty(scanRequest.getBugTracker().getCustomBean()) && scanRequest.getBugTracker().getCustomBean().equalsIgnoreCase("CxXml")){
                scaProperties.setPreserveXml(true);
            }
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
            log.error("Error occurred while setting scanner properties", e);
            throw new MachinaRuntimeException(e.getMessage());
        }
    }

}