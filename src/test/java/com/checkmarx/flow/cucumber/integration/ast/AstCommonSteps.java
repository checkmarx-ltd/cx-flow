package com.checkmarx.flow.cucumber.integration.ast;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.ASTScanner;
import com.checkmarx.sdk.config.AstProperties;
import com.checkmarx.sdk.config.ScaProperties;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;

@RequiredArgsConstructor
public class AstCommonSteps {

    protected final FlowProperties flowProperties;
    protected final AstProperties astProperties;
    protected final ScaProperties scaProperties;

    protected void initAstConfig() {
        astProperties.setIncremental("false");
        //astProperties.setApiUrl("https://api.scacheckmarx.com");
        astProperties.setPreset("Default");
    }


    protected void initSCAConfig() {
        scaProperties.setAppUrl("https://sca.scacheckmarx.com");
        scaProperties.setApiUrl("https://api.scacheckmarx.com");
        scaProperties.setAccessControlUrl("https://platform.checkmarx.net");
    }


    protected ScanRequest getBasicScanRequest(String projectName, String repoWithAuth) {
        return ScanRequest.builder()
                .project(projectName)
                .repoUrlWithAuth(repoWithAuth)
                .branch("master")
                .repoType(ScanRequest.Repository.GITHUB)
                .build();
    }

}