package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.config.AstProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResults;
import com.checkmarx.sdk.dto.ast.ASTResultsWrapper;
import com.checkmarx.sdk.dto.ast.ScanParams;
import com.cx.restclient.AstClientImpl;
import com.cx.restclient.ast.dto.sast.AstSastResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service

@Slf4j
public class ASTScanner extends AbstractASTScanner {

    public ASTScanner(AstClientImpl astClient, FlowProperties flowProperties, BugTrackerEventTrigger bugTrackerEventTrigger) {
        super(astClient, flowProperties, AstProperties.CONFIG_PREFIX, bugTrackerEventTrigger);
    }

    @Override
    protected ScanResults toScanResults(ASTResultsWrapper internalResults) {
        return ScanResults.builder()
                .astResults(internalResults.getAstResults())
                .build();
    }

    @Override
    protected void setScannerSpecificProperties(ScanRequest scanRequest, ScanParams scanParams) {
        // Currently only gets used in SCAScanner class
    }

    @Override
    protected String getScanId(ASTResultsWrapper internalResults) {

        return Optional.ofNullable(internalResults.getAstResults())
                .map(ASTResults::getResults)
                .map(AstSastResults::getScanId)
                .orElse("");

    }


}