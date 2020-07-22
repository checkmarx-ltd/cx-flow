package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResultsWrapper;
import com.checkmarx.sdk.service.AstClient;
import com.cx.restclient.ScaClientImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;



@Service
@Slf4j
public class SCAScanner extends  AbstractASTScanner{

    public SCAScanner(ScaClientImpl astClient, FlowProperties flowProperties) {
        super(astClient, flowProperties, ScaProperties.CONFIG_PREFIX);
    }


    @Override
    protected ScanResults toScanResults(ASTResultsWrapper internalResults) {
        return ScanResults.builder()
                .scaResults(internalResults.getScaResults())
                .build();
    }


    @Override
    protected String getScanId(ASTResultsWrapper internalResults) {
        return internalResults.getScaResults().getScanId();
    }



}