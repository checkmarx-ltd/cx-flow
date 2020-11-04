package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.ScaConfig;
import com.checkmarx.sdk.dto.Sca;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class ScaConfigurationOverrider {
    public void overrideScanRequestProperties(ScaConfig scaConfig, ScanRequest request, Map<String, String> overrideReport) {
        addToReport(scaConfig, overrideReport);
        request.setScaConfig(scaConfig);
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
            scaConfig.initThresholdsSeverity(thresholdsSeverity);
            overrideReport.put("thresholdsSeverity", ScanUtils.convertMapToString(thresholdsSeverity));
        });

        sca.map(Sca::getThresholdsScore).ifPresent(thresholdsScore -> {
            scaConfig.setThresholdsScore(thresholdsScore);
            overrideReport.put("thresholdsScore", String.valueOf(thresholdsScore));
        });

        sca.map(Sca::getFilterSeverity).ifPresent(filterSeverity -> {
            scaConfig.setFilterSeverity(filterSeverity);
            overrideReport.put("filterSeverity", filterSeverity.toString());
        });

        sca.map(Sca::getFilterScore).ifPresent(filterScore -> {
            scaConfig.setFilterScore(filterScore);
            overrideReport.put("filterScore", String.valueOf(filterScore));
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
        report.put("filterSeverity", config.getFilterSeverity().toString());
        report.put("filterScore", String.valueOf(config.getFilterScore()));
    }
}
