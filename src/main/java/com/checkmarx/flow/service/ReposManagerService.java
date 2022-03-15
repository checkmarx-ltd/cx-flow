package com.checkmarx.flow.service;

import com.checkmarx.flow.config.properties.CxIntegrationsProperties;
import com.checkmarx.flow.config.external.CxGoConfigFromWebService;
import com.checkmarx.flow.exception.ReposManagerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class ReposManagerService {
    private static final String CXFLOW_CONFIG_TEMPLATE = "/cxFlowConfig?repoUrl={repoUrl}&orgId={orgId}";

    private final RestTemplate restTemplate;
    private final CxIntegrationsProperties cxIntegrationsProperties;

    public ReposManagerService(@Qualifier("flowRestTemplate") RestTemplate restTemplate,
                               CxIntegrationsProperties cxIntegrationsProperties) {
        this.restTemplate = restTemplate;
        this.cxIntegrationsProperties = cxIntegrationsProperties;
    }

    public CxGoConfigFromWebService getCxGoDynamicConfig(String repoGitUrl, String orgId) {
        String apiBaseUrl = cxIntegrationsProperties.getUrl();
        if (StringUtils.isNotEmpty(apiBaseUrl)) {
            String fullUrlTemplate = apiBaseUrl + CXFLOW_CONFIG_TEMPLATE;
            log.info("Overriding CxGo configuration for the '{}' organization. Repo URL: {}", orgId, repoGitUrl);
            return getConfigResponse(repoGitUrl, orgId, fullUrlTemplate);
        } else {
            throw new ReposManagerException("ReposManager cannot be reached. URL is blank or empty");
        }
    }

    private CxGoConfigFromWebService getConfigResponse(String repoGitUrl, String orgId, String urlTemplate) {
        ResponseEntity<CxGoConfigFromWebService> responseEntity;
        try {
            responseEntity = restTemplate.getForEntity(urlTemplate, CxGoConfigFromWebService.class, repoGitUrl, orgId);
        } catch (HttpClientErrorException e) {
            throw new ReposManagerException("HttpClientErrorException: " + e.getMessage());
        }
        return responseEntity.getBody();
    }
}