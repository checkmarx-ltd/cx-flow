package com.checkmarx.flow.service;

import com.checkmarx.flow.config.SbomProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.RestClientConfig;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.AstScaResults;
import com.checkmarx.sdk.dto.RemoteRepositoryInfo;
import com.checkmarx.sdk.dto.ResultsBase;
import com.checkmarx.sdk.dto.ast.ScanParams;
import com.checkmarx.sdk.dto.sca.ScaConfig;
import com.checkmarx.sdk.service.scanner.AbstractScanner;
import com.checkmarx.sdk.utils.scanner.client.IScanClientHelper;
import com.checkmarx.sdk.utils.scanner.client.ScaClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.URL;


@Service
@Slf4j
@RequiredArgsConstructor
public class SbomService extends AbstractScanner {

    private final SbomProperties sbomProperties;
    private  final ScaProperties scaProperties;
    private final CxProperties cxProperties;

    private final FilenameFormatter filenameFormatter;


    public void SbomProcess(String scanId, ScanParams scanParams, ScanRequest scanRequest){
        log.info("#################INITIATING SBOM PROCESS#################");
        log.info("Report File Format :{}",sbomProperties.getReportFileFormat());
        String fileUrl= initiateSbom(scanId,scanParams,sbomProperties.getReportFileFormat(), sbomProperties.isHideDevAndTestDependencies(), sbomProperties.isShowOnlyEffectiveLicenses());
        String message=downloadFile(fileUrl,scanRequest);
        log.info(message);
    }

    private String downloadFile(String fileUrl,ScanRequest scanRequest) {
        try {
            String newFilename = null;
            if(sbomProperties.getReportFileFormat().equalsIgnoreCase("CycloneDxJson") || sbomProperties.getReportFileFormat().equalsIgnoreCase("SpdxJson"))
            {
                newFilename = filenameFormatter.formatPath(scanRequest, sbomProperties.getJsonFileNameFormat(), sbomProperties.getDataFolder());
            }
            else
            {
                newFilename = filenameFormatter.formatPath(scanRequest, sbomProperties.getXmlFileNameFormat(), sbomProperties.getDataFolder());
            }
            File newFile = new File(newFilename);
            log.info("SBOM File Path: {}",newFilename);
            BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(newFile);

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }

            fileOutputStream.close();
            in.close();

            return "File downloaded successfully!";
        } catch (IOException e) {
            e.printStackTrace();
            return "Error occurred while downloading the file.";
        }
    }


    @Override
    protected void applyFilterToResults(AstScaResults scaResults, ScanParams scanParams) {

    }

    @Override
    protected AstScaResults toResults(ResultsBase scanResults) {
        return null;
    }

    @Override
    protected IScanClientHelper allocateClient(RestClientConfig restClientConfig) {
        return new ScaClientHelper(restClientConfig, log, scaProperties, cxProperties);
    }

    @Override
    protected void setRemoteBranch(ScanParams scanParams, RemoteRepositoryInfo remoteRepoInfo) {

    }

    @Override
    protected RestClientConfig getScanConfig(ScanParams scanParams) {
        RestClientConfig restClientConfig = new RestClientConfig();
        restClientConfig.setProjectName(scanParams.getProjectName());
        restClientConfig.setDisableCertificateValidation(scanParams.isDisableCertificateValidation());

        ScaConfig scaConfig = getScaSpecificConfig(scanParams);
        setSourceLocation(scanParams, restClientConfig, scaConfig);
        if(scanParams.getRemoteRepoUrl() != null){
            restClientConfig.setClonedRepo(true);
        }
        restClientConfig.setScaConfig(scaConfig);

        return restClientConfig;
    }

    private ScaConfig getScaSpecificConfig(ScanParams scanParams) {
        ScaConfig scaConfig = new ScaConfig();
        com.checkmarx.sdk.config.ScaConfig sdkScaConfig = scanParams.getScaConfig();
        if (sdkScaConfig != null) {
            scaConfig.setWebAppUrl(sdkScaConfig.getAppUrl());
            scaConfig.setApiUrl(sdkScaConfig.getApiUrl());
            scaConfig.setAccessControlUrl(sdkScaConfig.getAccessControlUrl());
            scaConfig.setTenant(sdkScaConfig.getTenant());
            scaConfig.setIncludeSources(sdkScaConfig.isIncludeSources());
            scaConfig.setExcludeFiles(sdkScaConfig.getExcludeFiles());
            scaConfig.setUsername(scaProperties.getUsername());
            scaConfig.setPassword(scaProperties.getPassword());
            scaConfig.setFingerprintsIncludePattern(scaProperties.getFingerprintsIncludePattern());
            scaConfig.setManifestsIncludePattern(scaProperties.getManifestsIncludePattern());
            scaConfig.setTeam(sdkScaConfig.getTeam());
            scaConfig.setScanTimeout(sdkScaConfig.getScanTimeout());
            scaConfig.setExpPathSastProjectName(sdkScaConfig.getExpPathSastProjectName());
            String zipPath = scanParams.getZipPath();
            if (StringUtils.isNotEmpty(zipPath)) {
                scaConfig.setZipFilePath(zipPath);
            }

        } else {
            log.warn("Unable to map SCA configuration to an internal object.");
        }
        return scaConfig;
    }
    @Override
    protected void validateScanParams(ScanParams scaParams) {

    }

    @Override
    public AstScaResults getLatestScanResults(ScanParams scanParams) {
        return null;
    }
}
