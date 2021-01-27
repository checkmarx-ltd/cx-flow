package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.sdk.config.CxGoProperties;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.CxPropertiesBase;
import com.checkmarx.sdk.service.scanner.CxClient;
import com.checkmarx.sdk.service.CxService;
import com.checkmarx.sdk.service.scanner.GoScanner;
import com.checkmarx.sdk.service.scanner.ILegacyClient;
import org.springframework.stereotype.Service;

@Service
public class CxScannerService {
    
    private final ILegacyClient scannerClient;
    private final CxPropertiesBase cxPropertiesBase;

    public CxScannerService(CxProperties cxProperties, CxGoProperties cxgoProperties, FlowProperties flowProperties, CxService cxService, GoScanner cxGoClient) {
        
        this.scannerClient = getScannerClient(flowProperties, cxGoClient, cxService);
        this.cxPropertiesBase = getProperties(flowProperties, cxgoProperties, cxProperties);
    }

    private ILegacyClient getScannerClient(FlowProperties flowProperties, GoScanner cxGoClient, CxClient cxService) {
        return flowProperties!=null && flowProperties.isCxGoEnabled() && cxGoClient!=null ? cxGoClient : cxService;
    }

    public CxPropertiesBase getProperties(FlowProperties flowProperties, CxGoProperties cxgoProperties, CxProperties cxProperties){
        return flowProperties!=null && flowProperties.isCxGoEnabled() && cxgoProperties!=null ? cxgoProperties : cxProperties;
    }
    
    public ILegacyClient getScannerClient() {
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
