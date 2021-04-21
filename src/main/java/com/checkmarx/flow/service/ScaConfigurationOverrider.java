package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.ScaConfig;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.dto.sca.Sca;
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

    private static final String ACCESS_CONTROL_URL = "accessControlUrl";
    private static final String API_URL = "apiUrl";
    private static final String APP_URL = "appUrl";
    private static final String TENANT = "tenant";
    private static final String TEAM = "team";
    private static final String THRESHOLDS_SEVERITY = "thresholdsSeverity";
    private static final String THRESHOLDS_SCORE = "thresholdsScore";
    private static final String INCLUDE_SOURCES = "includeSources";

    private static final String FILTER_SCORE = "filterScore";
    private static final String FILTER_SEVERITY = "filterSeverity";

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

        ScaConfig scaConfig = request.getScaConfig();

        sca.map(Sca::getAccessControlUrl).ifPresent(accessControlUrl -> {
            scaConfig.setAccessControlUrl(accessControlUrl);
            overrideReport.put(ACCESS_CONTROL_URL, accessControlUrl);
        });

        sca.map(Sca::getApiUrl).ifPresent(apiUrl -> {
            scaConfig.setApiUrl(apiUrl);
            overrideReport.put(API_URL, apiUrl);
        });

        sca.map(Sca::getAppUrl).ifPresent(appUrl -> {
            scaConfig.setAppUrl(appUrl);
            overrideReport.put(APP_URL, appUrl);
        });

        sca.map(Sca::getTenant).ifPresent(tenant -> {
            scaConfig.setTenant(tenant);
            overrideReport.put(TENANT, tenant);
        });

        sca.map(Sca::getThresholdsSeverity).ifPresent(thresholdsSeverity -> {
            scaConfig.setThresholdsSeverityDirectly(thresholdsSeverity);
            overrideReport.put(THRESHOLDS_SEVERITY, ScanUtils.convertMapToString(thresholdsSeverity));
        });

        sca.map(Sca::getThresholdsScore).ifPresent(thresholdsScore -> {
            scaConfig.setThresholdsScore(thresholdsScore);
            overrideReport.put(THRESHOLDS_SCORE, String.valueOf(thresholdsScore));
        });

        sca.map(Sca::isIncludeSources).ifPresent(includeSources -> {
            scaConfig.setIncludeSources(includeSources);
            overrideReport.put(INCLUDE_SOURCES, String.valueOf(includeSources));
        });

        sca.map(Sca::getTeam).ifPresent(team -> {
            scaConfig.setTeam(team);
            overrideReport.put(TEAM, team);
        });

        overrideSeverityFilters(request, sca, overrideReport);

        overrideScoreFilter(request, sca, overrideReport);
    }

    private void overrideScoreFilter(ScanRequest request, Optional<Sca> override, Map<String, String> overrideReport) {
        override.map(Sca::getFilterScore).ifPresent(score -> {
            Filter filterFromOverride = scaFilterFactory.getScoreFilter(score);
            if (replaceFiltersOfType(request, Collections.singletonList(filterFromOverride), Filter.Type.SCORE)) {
                overrideReport.put(FILTER_SCORE, String.valueOf(filterFromOverride));
            }
        });
    }

    private void overrideSeverityFilters(ScanRequest request, Optional<Sca> override, Map<String, String> overrideReport) {
        override.map(Sca::getFilterSeverity).ifPresent(severities -> {
            List<Filter> filtersFromOverride = scaFilterFactory.getSeverityFilters(severities);
            if (replaceFiltersOfType(request, filtersFromOverride, Filter.Type.SEVERITY)) {
                overrideReport.put(FILTER_SEVERITY, severities.toString());
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
        report.put(ACCESS_CONTROL_URL, config.getAccessControlUrl());
        report.put(API_URL, config.getApiUrl());
        report.put(APP_URL, config.getAppUrl());
        report.put(TENANT, config.getTenant());
        report.put(THRESHOLDS_SEVERITY, ScanUtils.convertMapToString(config.getThresholdsSeverity()));
        report.put(THRESHOLDS_SCORE, String.valueOf(config.getThresholdsScore()));
    }
}
