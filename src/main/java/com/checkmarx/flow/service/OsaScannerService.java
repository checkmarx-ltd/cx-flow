package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

import static com.checkmarx.flow.exception.ExitThrowable.exit;

@Service
@Slf4j
@RequiredArgsConstructor
public class OsaScannerService {

    private static final String ERROR_BREAK_MSG = "Exiting with Error code 10 due to issues present";

    private final CxClient cxService;
    private final ResultsService resultsService;
    private final FlowProperties flowProperties;

    private ScanDetails scanDetails = null;

    public void cxOsaParseResults(ScanRequest request, File file, File libs) throws ExitThrowable {
        try {
            ScanResults results = cxService.getOsaReportContent(file, libs, request.getFilters());
            resultsService.processResults(request, results, scanDetails);
            if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
                log.error(ERROR_BREAK_MSG);
                exit(10);
            }
        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred while processing results file(s)", e);
            exit(3);
        }
    }
}