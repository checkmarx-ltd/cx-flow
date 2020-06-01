package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;

import java.util.ArrayList;
import java.util.List;

public class FilterFactory {
    public static FilterConfiguration getFilter(List<String> severity,
                                                List<String> cwe,
                                                List<String> category,
                                                List<String> status,
                                                FlowProperties flowProperties) {
        List<Filter> filters;
        if (!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status)) {
            filters = getFilters(severity, cwe, category, status);
        } else {
            filters = getFilters(flowProperties);
        }
        return FilterConfiguration.fromSimpleFilters(filters);
    }

    /**
     * Create List of filters based on String lists of severity, cwe, category
     * @param flowProperties
     * @return
     */
    private static List<Filter> getFilters(FlowProperties flowProperties) {
        return getFilters(flowProperties.getFilterSeverity(), flowProperties.getFilterCwe(), flowProperties.getFilterCategory(), flowProperties.getFilterStatus());
    }

    /**
     * Create List of filters based on String lists of severity, cwe, category
     * @param severity
     * @param cwe
     * @param category
     * @return
     */
    public static List<Filter> getFilters(List<String> severity, List<String> cwe, List<String> category, List<String> status) {
        List<Filter> filters = new ArrayList<>();
        filters.addAll(getListByFilterType(severity, Filter.Type.SEVERITY));
        filters.addAll(getListByFilterType(cwe, Filter.Type.CWE));
        filters.addAll(getListByFilterType(category, Filter.Type.TYPE));
        filters.addAll(getListByFilterType(status, Filter.Type.STATUS));
        return filters;
    }

    private static List<Filter> getListByFilterType(List<String> stringFilters, Filter.Type type){
        List<Filter> filterList = new ArrayList<>();
        if(stringFilters != null) {
            for (String s : stringFilters) {
                filterList.add(Filter.builder()
                        .type(type)
                        .value(s)
                        .build());
            }
        }
        return filterList;
    }
}
