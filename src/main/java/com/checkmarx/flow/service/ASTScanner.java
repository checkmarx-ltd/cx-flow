package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;

import com.checkmarx.sdk.config.AstProperties;

import com.checkmarx.sdk.dto.ScanResults;

import com.checkmarx.sdk.dto.ast.ASTResultsWrapper;


import com.cx.restclient.AstClientImpl;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;



@Service

@Slf4j
public class ASTScanner extends AbstractASTScanner  {

    public ASTScanner(AstClientImpl astClient, FlowProperties flowProperties) {
        super(astClient, flowProperties, AstProperties.CONFIG_PREFIX);
    }

    @Override
    protected ScanResults toScanResults(ASTResultsWrapper internalResults) {
        return ScanResults.builder()
                .astResults(internalResults.getAstResults())
                .build();
    }

    @Override
    protected String getScanId(ASTResultsWrapper internalResults) {
        return internalResults.getAstResults().getResults().getScanId();
    }
    

}