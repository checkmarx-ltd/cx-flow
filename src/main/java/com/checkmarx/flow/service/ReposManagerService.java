package com.checkmarx.flow.service;

import com.checkmarx.flow.config.CxIntegrationsProperties;
import com.checkmarx.flow.config.external.CxGoDynamicConfig;
import com.checkmarx.flow.exception.ReposManagerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class ReposManagerService {

    private final RestTemplate restTemplate;
    private final CxIntegrationsProperties cxIntegrationsProperties;

    private String cxGoConfigUrlPattern;

    public ReposManagerService(@Qualifier("flowRestTemplate") RestTemplate restTemplate,
                               CxIntegrationsProperties cxIntegrationsProperties) {
        this.restTemplate = restTemplate;
        this.cxIntegrationsProperties = cxIntegrationsProperties;
    }

    @PostConstruct
    private void compositeUrlPaths() {
        cxGoConfigUrlPattern = cxIntegrationsProperties.getUrl() + "/%s/orgs/%s/tenantConfig";
    }

    CxGoDynamicConfig getCxGoDynamicConfig(String scmType, String orgName) {
        if (StringUtils.isNotEmpty(cxIntegrationsProperties.getUrl())) {
            String urlPath = String.format(cxGoConfigUrlPattern, scmType, orgName);
            log.info("Overriding Cx-Go configuration for SCM type: {} and organization name: {}", scmType, orgName);
            ResponseEntity<CxGoDynamicConfig> responseEntity;
            try {
                responseEntity = restTemplate.exchange
                        (urlPath, HttpMethod.GET, new HttpEntity<>(null, null), CxGoDynamicConfig.class);

            } catch (HttpClientErrorException e) {
                throw new ReposManagerException("HttpClientErrorException: " + e.getMessage());
            }
            return responseEntity.getBody();
        } else {
            throw new ReposManagerException("Repos-manager cannot be reached. URL is blank or empty");
        }
    }
}