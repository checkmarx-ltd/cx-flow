package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.ScaConfig;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.Sca;
import com.checkmarx.sdk.dto.filtering.EngineFilterConfiguration;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
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
        // Initializing something in 'overrider' is not very consistent. However, this is the only point
        // where we can initialize SCA config without having to change each controller.

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

        overrideSeverityFilters(request, sca, overrideReport);

        overrideScoreFilter(request, sca, overrideReport);

        request.setScaConfig(scaConfig);
    }

    private void overrideScoreFilter(ScanRequest request, Optional<Sca> override, Map<String, String> overrideReport) {
        override.map(Sca::getFilterScore).ifPresent(score -> {
            Filter filterFromOverride = scaFilterFactory.getScoreFilter(score);
            if (replaceFiltersOfType(request, Collections.singletonList(filterFromOverride), Filter.Type.SCORE)) {
                overrideReport.put("filterScore", String.valueOf(filterFromOverride));
            }
        });
    }

    private void overrideSeverityFilters(ScanRequest request, Optional<Sca> override, Map<String, String> overrideReport) {
        override.map(Sca::getFilterSeverity).ifPresent(severities -> {
            List<Filter> filtersFromOverride = scaFilterFactory.getSeverityFilters(severities);
            if (replaceFiltersOfType(request, filtersFromOverride, Filter.Type.SEVERITY)) {
                overrideReport.put("filterSeverity", severities.toString());
            }
        });
    }

    private boolean replaceFiltersOfType(ScanRequest request, List<Filter> override, Filter.Type type) {
        boolean canOverride = true;
        List<Filter> existingFilters = Optional.ofNullable(request.getFilter())
                .map(FilterConfiguration::getScaFilters)
                .map(EngineFilterConfiguration::getSimpleFilters)
                .orElse(null);

        if (existingFilters == null) {
            log.warn("Unable to apply {} filter override. " +
                    "Unexpected state: simple filter list in SCA filters is not initialized.",
                    type);
            canOverride = false;
        } else {
            existingFilters.removeIf(filter -> filter.getType() == type);
            existingFilters.addAll(override);
        }
        return canOverride;
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
