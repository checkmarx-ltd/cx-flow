package com.checkmarx.flow.aop;

import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.ScanRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAop {
    @Before(value = "(execution(* com.checkmarx.flow.service.FlowService.initiateAutomation(.., com.checkmarx.flow.dto.ScanRequest, ..)) ||" +
            "execution(* com.checkmarx.flow.service.ResultsService.processScanResultsAsync(.., com.checkmarx.flow.dto.ScanRequest, ..)) ||" +
            "execution(* com.checkmarx.flow.service.SastScanner.executeCxScan(com.checkmarx.flow.dto.ScanRequest, ..)) ||" +
            "execution(* com.checkmarx.flow.service.CxGoScanner.executeCxScan(com.checkmarx.flow.dto.ScanRequest, ..)) ||" +
            "execution(* com.checkmarx.flow.service.SCAScanner.scan(com.checkmarx.flow.dto.ScanRequest, ..)) ||" +
            "execution(* com.checkmarx.flow.service.ASTScanner.scan(com.checkmarx.flow.dto.ScanRequest, ..)) ||" +
            "execution(* com.checkmarx.flow.service.ResultsService.publishCombinedResults(com.checkmarx.flow.dto.ScanRequest, ..)) ||" +
            "execution(* com.checkmarx.flow.service.*.process(.., com.checkmarx.flow.dto.ScanRequest, ..))) && (args(.., request) || args(request, ..))")
    public void beforeAdvice(JoinPoint joinPoint, ScanRequest request) {
        if(request != null) {
            String id = request.getId();
            MDC.put(FlowConstants.MAIN_MDC_ENTRY, id);
        }
    }
    @After(value = "execution(* com.checkmarx.flow.service.ResultsService.processScanResultsAsync(.., com.checkmarx.flow.dto.ScanRequest, ..)) &&  (args(.., request) || args(request, ..))")
    public void afterAdvice(JoinPoint joinPoint, ScanRequest request) {
        MDC.clear();
    }
}

