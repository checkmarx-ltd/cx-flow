package com.checkmarx.flow.filter;

import com.google.common.base.CaseFormat;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transforms kebab-case request params to lower camel case.
 */
public class CaseTransformingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        Map<String, String[]> camelCaseParams = new ConcurrentHashMap<>();

        for (String param : request.getParameterMap().keySet()) {
            String formattedParam = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, param);
            camelCaseParams.put(formattedParam, request.getParameterValues(param));
        }

        ServletRequest requestWrapper = new ParameterOverridingWrapper(camelCaseParams, request);
        filterChain.doFilter(requestWrapper, response);
    }
}
