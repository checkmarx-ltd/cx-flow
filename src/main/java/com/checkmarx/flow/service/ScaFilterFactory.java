package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.dto.filtering.EngineFilterConfiguration;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.service.FilterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ScaFilterFactory {
    private final NumberFormat neutralFormat = NumberFormat.getInstance(FilterValidator.NUMERIC_CONVERSION_LOCALE);

    private final ScaProperties scaProperties;

    public void initScaFilter(ScanRequest request) {
        log.info("Initializing SCA filters.");

        List<Filter> severityFilters = getSeverityFilters(scaProperties.getFilterSeverity());
        Filter scoreFilter = getScoreFilter(scaProperties.getFilterScore());
        Filter policyViolationFilter = getPolicyViolationFilter(scaProperties.getFilterPolicyViolation());

        List<Filter> allFilters = combine(severityFilters, scoreFilter,policyViolationFilter);
        writeToLog(allFilters);

        setScaFilters(allFilters, request);
    }

    public Filter getScoreFilter(Double value) {
        return Optional.ofNullable(value)
                .map(numericScore -> Filter.builder()
                        .type(Filter.Type.SCORE)
                        .value(neutralFormat.format(numericScore))
                        .build())
                .orElse(null);
    }

    public Filter getPolicyViolationFilter(String value) {
        return Optional.ofNullable(value)
                .map(booleanString -> Filter.builder()
                        .type(Filter.Type.POLICYVIOLATION)
                        .value(booleanString)
                        .build())
                .orElse(null);
    }

    public List<Filter> getSeverityFilters(List<String> severities) {
        return Optional.ofNullable(severities)
                .orElse(Collections.emptyList())
                .stream()
                .filter(StringUtils::isNotEmpty)
                .map(toSeverityFilter())
                .collect(Collectors.toList());
    }

    private static List<Filter> combine(List<Filter> severityFilters, Filter scoreFilter,
            Filter policyViolationFilter) {
        List<Filter> allFilters = new ArrayList<>(severityFilters);
        if (scoreFilter != null) {
            allFilters.add(scoreFilter);
        }
        if (policyViolationFilter != null) {
            allFilters.add(policyViolationFilter);
        }
        return allFilters;
    }

    private static Function<String, Filter> toSeverityFilter() {
        return severity -> Filter.builder()
                .type(Filter.Type.SEVERITY)
                .value(severity)
                .build();
    }

    private static void writeToLog(List<Filter> filters) {
        if (log.isDebugEnabled()) {
            String formattedFilters = filters.stream()
                    .map(filter -> String.format("(%s: %s)", filter.getType(), filter.getValue()))
                    .collect(Collectors.joining(", "));

            log.debug("Using SCA filters: {}", formattedFilters);
        }
    }

    private static void setScaFilters(List<Filter> filters, ScanRequest target) {
        FilterConfiguration existingOrNewConfig = Optional.ofNullable(target.getFilter())
                .orElseGet(() -> FilterConfiguration.builder().build());

        existingOrNewConfig.setScaFilters(EngineFilterConfiguration.builder()
                .simpleFilters(filters)
                .build());

        target.setFilter(existingOrNewConfig);
    }
}
