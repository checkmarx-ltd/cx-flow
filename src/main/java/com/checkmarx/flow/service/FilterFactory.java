package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.dto.filtering.ScriptedFilter;
import com.checkmarx.sdk.exception.CheckmarxRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FilterFactory {
    public FilterConfiguration getFilter(ControllerRequest request,
                                         @Nullable FlowProperties flowProperties) {
        FilterConfiguration result;

        request = Optional.ofNullable(request)
                .orElse(ControllerRequest.builder().build());

        if (hasRequiredProperties(request)) {
            result = getFilter(request.getSeverity(),
                    request.getCwe(),
                    request.getCategory(),
                    request.getStatus(),
                    null,
                    null);
        } else if (flowProperties != null) {
            result = getFilter(flowProperties);
        } else {
            result = FilterConfiguration.builder().build();
        }
        return result;
    }

    /**
     * Create filter configuration based on CxFlow properties.
     */
    private static FilterConfiguration getFilter(FlowProperties flowProperties) {
        return getFilter(flowProperties.getFilterSeverity(),
                flowProperties.getFilterCwe(),
                flowProperties.getFilterCategory(),
                flowProperties.getFilterStatus(),
                flowProperties.getFilterState(),
                flowProperties.getFilterScript());
    }

    private boolean hasRequiredProperties(ControllerRequest request) {
        return CollectionUtils.isNotEmpty(request.getSeverity())
                || CollectionUtils.isNotEmpty(request.getCwe())
                || CollectionUtils.isNotEmpty(request.getCategory())
                || CollectionUtils.isNotEmpty(request.getStatus());
    }

    /**
     * Create filter configuration based on lists of severity, cwe, category and on the text of a filter script
     */
    private static FilterConfiguration getFilter(List<String> severity,
                                                 List<String> cwe,
                                                 List<String> category,
                                                 List<String> status,
                                                 List<String> state,
                                                 String filterScript) {
        List<Filter> simpleFilters = new ArrayList<>();
        simpleFilters.addAll(getListByFilterType(severity, Filter.Type.SEVERITY));
        simpleFilters.addAll(getListByFilterType(cwe, Filter.Type.CWE));
        simpleFilters.addAll(getListByFilterType(category, Filter.Type.TYPE));
        simpleFilters.addAll(getListByFilterType(status, Filter.Type.STATUS));
        simpleFilters.addAll(getListByFilterType(state, Filter.Type.STATE));

        Script parsedScript = parseScriptText(filterScript);

        return FilterConfiguration.builder()
                .simpleFilters(simpleFilters)
                .scriptedFilter(ScriptedFilter.builder()
                        .script(parsedScript)
                        .build())
                .build();
    }

    private static Script parseScriptText(String filterScript) {
        Script result = null;
        if (StringUtils.isNotEmpty(filterScript)) {
            GroovyShell groovyShell = new GroovyShell();
            try {
                result = groovyShell.parse(filterScript);
            } catch (CompilationFailedException e) {
                throw new CheckmarxRuntimeException("An error has occurred while parsing the filter script. " +
                        "Please make sure the script syntax is correct.", e);
            } catch (Exception e) {
                throw new CheckmarxRuntimeException("An unexpected error has occurred while parsing the filter script.", e);
            }
        }
        return result;
    }

    private static List<Filter> getListByFilterType(List<String> stringFilters, Filter.Type type) {
        List<Filter> filterList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(stringFilters)) {
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
