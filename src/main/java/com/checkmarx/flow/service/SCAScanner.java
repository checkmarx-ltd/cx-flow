package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.custom.PDFProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.report.AnalyticsReport;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;

import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.PDFPropertiesSCA;
import com.checkmarx.sdk.config.RestClientConfig;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.AstScaResults;
import com.checkmarx.sdk.dto.ast.ASTResults;
import com.checkmarx.sdk.dto.ast.ScanParams;

import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.exception.CheckmarxException;

import com.checkmarx.sdk.service.scanner.AbstractScanner;
import com.checkmarx.sdk.service.scanner.ScaScanner;
import com.checkmarx.sdk.utils.CxRepoFileHelper;
import com.checkmarx.sdk.utils.scanner.client.IScanClientHelper;
import com.checkmarx.sdk.utils.scanner.client.ScaClientHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.checkmarx.flow.exception.ExitThrowable.exit;


@Service
@Slf4j
@Order(2)
public class SCAScanner extends AbstractASTScanner {

    private final ScaProperties scaProperties;
    private final PDFProperties pdfProperties;
    private final CxRepoFileHelper cxRepoFileHelper;
    private final CxProperties cxProperties;
    private final  BugTrackerEventTrigger bugTrackerEventTrigger;
    private final  AbstractScanner client;

    private String scanType;



    @Autowired
    ScaScanner scaScannerClient;

    public SCAScanner(ScaScanner scaClient, FlowProperties flowProperties, BugTrackerEventTrigger bugTrackerEventTrigger,
                      ScaProperties scaProperties, ResultsService resultsService, PDFProperties pdfProperties, CxProperties cxProperties, BugTrackerEventTrigger bugTrackerEventTrigger1, @Qualifier("scaScanner") AbstractScanner client,ProjectNameGenerator projectNameGenerator) {
        super(scaClient, flowProperties,scaProperties, ScaProperties.CONFIG_PREFIX, bugTrackerEventTrigger,projectNameGenerator,resultsService);



        this.scaProperties = scaProperties;
        this.pdfProperties = pdfProperties;
        this.cxProperties = cxProperties;
        this.bugTrackerEventTrigger = bugTrackerEventTrigger1;
        this.client = client;
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
            ScanParams sdkScanParams;
            if(scaProperties.getProjectName()!=null)
            {
                sdkScanParams = ScanParams.builder()
                    .projectName(scaProperties.getProjectName())
                    .scaConfig(scanRequest.getScaConfig())
                    .filterConfiguration(scanRequest.getFilter())
                    .build();
            }
            else {
                sdkScanParams = ScanParams.builder()
                        .projectName(scanRequest.getProject())
                        .scaConfig(scanRequest.getScaConfig())
                        .filterConfiguration(scanRequest.getFilter())
                        .build();
            }
            restClientConfig=scaScannerClient.getScanConfig(sdkScanParams);

            iScanClientHelper=new ScaClientHelper(restClientConfig,log,scaProperties,cxProperties);
            ScanResults results =iScanClientHelper.getReportContent(file, scanRequest.getFilter());
            resultsService.processResults(scanRequest, results, scanDetails);
            if (flowProperties.isBreakBuild() && results != null && results.getXIssues() != null && !results.getXIssues().isEmpty()) {
                log.error(ERROR_BREAK_MSG);
                exit(ExitCode.BUILD_INTERRUPTED);
            }
        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred while processing results file", e);
            exit(ExitCode.CHECKMARX_EXCEPTION);
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
            }else if(scaProperties.isEnableScaResolver())
            {
                log.info("CxAST-SCA sca resolver is enabled");
                if(scanParams.getRemoteRepoUrl()!=null)
                {
                    String scaClonedFolderPath = cxRepoFileHelper.getScaClonedRepoFolderPath(scanRequest.getRepoUrlWithAuth(), scanRequest.getExcludeFiles(), scanRequest.getBranch());
                    scanParams.setSourceDir(scaClonedFolderPath);
                }
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

    @Override
    public ScanResults scanCliToGeneratePDF(ScanRequest scanRequest, String scanType, File... files) {
        BugTracker.Type bugTrackerType = bugTrackerEventTrigger.triggerScanStartedEvent(scanRequest);
        ScanResults result = null;
        this.scanType=scanType;

        String effectiveProjectName = normalize(scanRequest.getProject(),flowProperties.isPreserveProjectName());
        scanRequest.setProject(effectiveProjectName);
        if (bugTrackerType.equals(BugTracker.Type.NONE)) {
            log.info("Not waiting for scan completion as Bug Tracker type is NONE");
            ScanParams sdkScanParams = toSdkScanParams(scanRequest, files[0].getPath());
            client.scanWithNoWaitingToResults(sdkScanParams);
        } else {
            actualPDFScan(scanRequest, files[0].getPath());
        }
        return result;
    }

    private void actualPDFScan(ScanRequest scanRequest, String path) {
        ScanResults result = null;
        log.info("--------------------- Initiating new {} scan ---------------------", scanType);
        try {


            PDFPropertiesSCA pdfSCAprop= new PDFPropertiesSCA();
            pdfSCAprop.setFileNameFormat(pdfProperties.getFileNameFormat());
            pdfSCAprop.setDataFolder(pdfProperties.getDataFolder());

            ScanParams sdkScanParams = toSdkScanParams(scanRequest, path);
            client.scanForPDF(sdkScanParams,pdfSCAprop);

        } catch (Exception e) {
        }
    }

    private ScanParams toSdkScanParams(ScanRequest scanRequest, String pathToScan) {
        ScanParams scanParams = ScanParams.builder()
                .projectName(scanRequest.getProject())
                .sourceDir(pathToScan)
                .scaConfig(scanRequest.getScaConfig())
                .filterConfiguration(scanRequest.getFilter())
                .disableCertificateValidation(scanRequest.isDisableCertificateValidation())
                .build();
        setScannerSpecificProperties(scanRequest,scanParams);
        return scanParams;
    }

    @Override
    public ScanResults DownloadPDF(ScanResults scanResults, PDFProperties pdfProperties) {
        return null;
    }
    private static String normalize(String rawProjectName, boolean preserveProjectName) {
        String result = null;
        if (rawProjectName != null) {
            if (!preserveProjectName) {
                if(!rawProjectName.contains("#")) {
                    result = rawProjectName.replaceAll("[^a-zA-Z0-9-_.]+", "-");
                }
                else {
                    result = rawProjectName;
                }
                if (!result.equals(rawProjectName)) {
                    log.debug("Project name ({}) has been normalized to allow only valid characters.", rawProjectName);
                }
            } else {
                result = rawProjectName;
                log.info("Project name ({}) has not been normalized.", rawProjectName);
            }
            log.info("Project name being used: {}", result);
        } else {
            log.warn("Project name returned NULL");
        }
        return result;
    }
    private void logRequest(ScanRequest request, AstScaResults internalResults, OperationResult scanCreationResult) {
        String scanId = getScanId(internalResults);
        ScanReport report = new ScanReport(scanId, request, request.getRepoUrl(), scanCreationResult, AnalyticsReport.SCA);
        report.log();
    }
}