package com.checkmarx.flow.handlers.config;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.service.ConfigurationOverrider;
import com.checkmarx.flow.service.CxScannerService;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;

public interface ConfigContextProvider {

    public FlowProperties getFlowProperties();
    public CxScannerService getCxScannerService();
    public JiraProperties getJiraProperties();
    public FlowService getFlowService();
    public HelperService getHelperService();
    public FilterFactory getFilterFactory();
    public ConfigurationOverrider getConfigOverrider();
    
}
