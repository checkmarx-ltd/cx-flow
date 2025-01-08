package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.custom.PDFProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.sdk.config.AstProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.AstScaResults;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResults;
import com.checkmarx.sdk.dto.ast.ScanParams;
import com.checkmarx.sdk.service.scanner.AstScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;


@Service
@Order(3)
@Slf4j
public class ASTScanner extends AbstractASTScanner {

    public ASTScanner(AstScanner astClient, FlowProperties flowProperties, BugTrackerEventTrigger bugTrackerEventTrigger,ProjectNameGenerator projectNameGenerator,ResultsService resultsService) {
        super(astClient, flowProperties,null, AstProperties.CONFIG_PREFIX, bugTrackerEventTrigger,projectNameGenerator,resultsService);
    }

    @Override
    protected ScanResults toScanResults(AstScaResults internalResults) {
        return ScanResults.builder()
                .astResults(internalResults.getAstResults())
                .build();
    }

    @Override
    protected void cxParseResults(ScanRequest request, File file) throws ExitThrowable {

    }

    @Override
    protected void setScannerSpecificProperties(ScanRequest scanRequest, ScanParams scanParams) {
        // Currently only gets used in SCAScanner class
    }

    @Override
    protected String getScanId(AstScaResults internalResults) {

        return Optional.ofNullable(internalResults.getAstResults())
                .map(ASTResults::getScanId)
                .orElse("");

    }


    @Override
    public ScanResults scanCliToGeneratePDF(ScanRequest request, String scanType, File... files) {
        return null;
    }

    @Override
    public ScanResults DownloadPDF(ScanResults scanResults, PDFProperties pdfProperties) {
        return null;
    }
}