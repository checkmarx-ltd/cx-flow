package com.checkmarx.flow.filter;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.*;
import java.io.IOException;

public class SecurityFilterTest {

    private static final SecurityFilter securityFilter = new SecurityFilter();
    private static final ServletRequest request = new MockHttpServletRequest();
    private static final ServletResponse response = new MockHttpServletResponse();
    private static final FilterChain chain = new MockFilterChain();
    private static final FilterConfig filterConfig = new MockFilterConfig();

    @Test
    public void initNullArgs() throws ServletException {
        securityFilter.init(null);
    }

    @Test
    public void initEmptyArgs() throws ServletException {
        securityFilter.init(filterConfig);
    }

    @Test
    public void doFilterNullArgs() throws IOException, ServletException {
        securityFilter.doFilter(null, null, null);
    }

    @Test
    public void doFilterRequestNullResponseNullChain() throws IOException, ServletException {
        securityFilter.doFilter(request, null, null);
    }

    @Test
    public void doFilterRequestResponseNullChain() throws IOException, ServletException {
        securityFilter.doFilter(request, response, null);
    }

    @Test
    public void doFilterRequestChainNullResponse() throws IOException, ServletException {
        securityFilter.doFilter(request, null, chain);
    }

    @Test
    public void doFilterResponseNullRequestNullChain() throws IOException, ServletException {
        securityFilter.doFilter(null, response, null);
    }

    @Test
    public void doFilterResponseChainNullRequest() throws IOException, ServletException {
        securityFilter.doFilter(null, response, chain);
    }

    @Test
    public void doFilterChainNullRequestNullResponse() throws IOException, ServletException {
        securityFilter.doFilter(null, null, chain);
    }
    @Test
    public void doFilterRequestResponseChain() throws IOException, ServletException {
        securityFilter.doFilter(request, response, chain);
    }

    @Test
    public void destroy() {
        securityFilter.destroy();
    }
}