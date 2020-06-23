package com.checkmarx.flow.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * Returns parameter values from a provided Map instead of taking them from request.
 */
class ParameterOverridingWrapper extends HttpServletRequestWrapper {
    private final Map<String, String[]> parameterOverride;

    public ParameterOverridingWrapper(Map<String, String[]> parameterOverride, HttpServletRequest request) {
        super(request);
        this.parameterOverride = parameterOverride;
    }

    @Override
    public String getParameter(String name) {
        return parameterOverride.containsKey(name) ? parameterOverride.get(name)[0] : null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterOverride.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterOverride.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterOverride;
    }
}
