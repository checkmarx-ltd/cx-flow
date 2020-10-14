package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.sdk.config.CxGoProperties;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.CxPropertiesBase;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.sdk.service.CxService;
import com.cx.restclient.CxGoClientImpl;
import com.cx.restclient.ScannerClient;
import org.springframework.stereotype.Service;

@Service
public class CxScannerService {
    
    private final CxProperties cxProperties;
    private final CxGoProperties cxgoProperties;
    private final FlowProperties flowProperties;
    private final CxService cxService;
    private  final CxGoClientImpl cxGoClient;
    private final ScannerClient scannerClient;
    private final CxPropertiesBase cxPropertiesBase;

    public CxScannerService(CxProperties cxProperties, CxGoProperties cxgoProperties, FlowProperties flowProperties, CxService cxService, CxGoClientImpl cxGoClient) {
        this.cxProperties = cxProperties;
        this.cxgoProperties = cxgoProperties;
        this.flowProperties = flowProperties;
        this.cxService = cxService;
        this.cxGoClient = cxGoClient;
        
        this.scannerClient = getScannerClient(flowProperties, cxGoClient, cxService);
        this.cxPropertiesBase = getProperties(flowProperties, cxgoProperties, cxProperties);
    }

    private ScannerClient getScannerClient(FlowProperties flowProperties, CxGoClientImpl cxGoClient, CxClient cxService) {
        return flowProperties!=null && flowProperties.isCxGoEnabled() && cxGoClient!=null ? cxGoClient : cxService;
    }

    public CxPropertiesBase getProperties(FlowProperties flowProperties, CxGoProperties cxgoProperties, CxProperties cxProperties){
        return flowProperties!=null && flowProperties.isCxGoEnabled() && cxgoProperties!=null ? cxgoProperties : cxProperties;
    }
    
    public ScannerClient getScannerClient() {
        return scannerClient;
    }

    public CxPropertiesBase getProperties() {
        return cxPropertiesBase;
    }
    
    public static AbstractVulnerabilityScanner getScanner(CxGoScanner cxgoScanner, SastScanner sastScanner){
        if(cxgoScanner.isEnabled()){
            return cxgoScanner;
        }else {
            return sastScanner;
        }
    }
    
}
