package com.checkmarx.flow.aop;

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
            "execution(* com.checkmarx.flow.service.FlowService.executeCxScanFlow(com.checkmarx.flow.dto.ScanRequest, ..)) ||" +
            "execution(* com.checkmarx.flow.service.*.process(.., com.checkmarx.flow.dto.ScanRequest, ..))) && (args(.., request) || args(request, ..))")
    public void beforeAdvice(JoinPoint joinPoint, ScanRequest request) {
        String id = request.getId();
        MDC.put("cx", id);
    }
    @After(value = "execution(* com.checkmarx.flow.service.ResultsService.processScanResultsAsync(.., com.checkmarx.flow.dto.ScanRequest, ..)) &&  (args(.., request) || args(request, ..))")
    public void afterAdvice(JoinPoint joinPoint, ScanRequest request) {
        MDC.clear();
    }
}

