package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.ScaConfig;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.Sca;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScaConfigurationOverrider {
    private static final ModelMapper modelMapper = new ModelMapper();

    private final ScaProperties scaProperties;
    private final ScaFilterFactory scaFilterFactory;

    public void initScaConfig(ScanRequest request) {
        log.debug("Initializing SCA configuration in scan request using default configuration properties.");
        ScaConfig scaConfig = modelMapper.map(scaProperties, ScaConfig.class);
        request.setScaConfig(scaConfig);
        scaFilterFactory.initScaFilter(request);
    }

    public void overrideScanRequestProperties(ScaConfig scaConfig, ScanRequest request, Map<String, String> overrideReport) {
        log.debug("Overriding SCA config in scan request.");
        ScaConfig existingScaConfig = request.getScaConfig();
        if (existingScaConfig == null) {
            log.debug("SCA config doesn't exist yet. Using the override as is.");
            request.setScaConfig(scaConfig);
        } else {
            log.debug("SCA config exists, merging.");
            modelMapper.map(scaConfig, existingScaConfig);
        }
        addToReport(scaConfig, overrideReport);
    }

    public void overrideScanRequestProperties(Sca override, ScanRequest request, Map<String, String> overrideReport) {
        Optional<Sca> sca = Optional.ofNullable(override);
        if (!sca.isPresent()) {
            return;
        }

        ScaConfig scaConfig = ScaConfig.builder()
                .build();

        sca.map(Sca::getAccessControlUrl).ifPresent(accessControlUrl -> {
            scaConfig.setAccessControlUrl(accessControlUrl);
            overrideReport.put("accessControlUrl", accessControlUrl);
        });

        sca.map(Sca::getApiUrl).ifPresent(apiUrl -> {
            scaConfig.setApiUrl(apiUrl);
            overrideReport.put("apiUrl", apiUrl);
        });

        sca.map(Sca::getAppUrl).ifPresent(appUrl -> {
            scaConfig.setAppUrl(appUrl);
            overrideReport.put("appUrl", appUrl);
        });

        sca.map(Sca::getTenant).ifPresent(tenant -> {
            scaConfig.setTenant(tenant);
            overrideReport.put("tenant", tenant);
        });

        sca.map(Sca::getThresholdsSeverity).ifPresent(thresholdsSeverity -> {
            scaConfig.setThresholdsSeverity(thresholdsSeverity);
            overrideReport.put("thresholdsSeverity", ScanUtils.convertMapToString(thresholdsSeverity));
        });

        sca.map(Sca::getThresholdsScore).ifPresent(thresholdsScore -> {
            scaConfig.setThresholdsScore(thresholdsScore);
            overrideReport.put("thresholdsScore", String.valueOf(thresholdsScore));
        });

        request.setScaConfig(scaConfig);
    }

    private static void addToReport(ScaConfig config, Map<String, String> report) {
        report.put("accessControlUrl", config.getAccessControlUrl());
        report.put("apiUrl", config.getApiUrl());
        report.put("appUrl", config.getAppUrl());
        report.put("tenant", config.getTenant());
        report.put("thresholdsSeverity", ScanUtils.convertMapToString(config.getThresholdsSeverity()));
        report.put("thresholdsScore", String.valueOf(config.getThresholdsScore()));
    }
}
