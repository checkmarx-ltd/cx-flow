package com.checkmarx.flow.service;

import com.checkmarx.flow.config.CxIntegrationsProperties;
import com.checkmarx.flow.config.external.CxGoConfigFromWebService;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.exception.ReposManagerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

        cxGoConfigUrlPattern = cxIntegrationsProperties.getUrl() + "/%s/orgs/%s/tenantConfig";
    }

    public CxGoConfigFromWebService getCxGoDynamicConfig(String scmType, String orgId) {
        if (StringUtils.isNotEmpty(cxIntegrationsProperties.getUrl())) {
            String urlPath = String.format(cxGoConfigUrlPattern, scmType, urlEncode(orgId));
            log.info("Overriding Cx-Go configuration for SCM type: {} and organization ID: {}", scmType, orgId);
            ResponseEntity<CxGoConfigFromWebService> responseEntity;
            try {
                responseEntity = restTemplate.getForEntity(urlPath, CxGoConfigFromWebService.class);
            } catch (HttpClientErrorException e) {
                throw new ReposManagerException("HttpClientErrorException: " + e.getMessage());
            }
            return responseEntity.getBody();
        } else {
            throw new ReposManagerException("Repos-manager cannot be reached. URL is blank or empty");
        }
    }

    private static String urlEncode(String orgName) {
        try {
            return URLEncoder.encode(orgName, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new MachinaRuntimeException("Error while trying to encode input: " + orgName + " with error message: " + e.getMessage());
        }
    }
}