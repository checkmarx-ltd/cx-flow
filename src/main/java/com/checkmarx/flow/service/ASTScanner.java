package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.OperationStatus;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.config.AstProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResults;
import com.checkmarx.sdk.dto.ast.ASTResultsWrapper;
import com.checkmarx.sdk.dto.ast.SCAResults;
import com.checkmarx.sdk.dto.ast.ScanParams;


import com.checkmarx.sdk.service.AstClient;
import com.cx.restclient.AstClientImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


import java.net.URL;


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
    

}