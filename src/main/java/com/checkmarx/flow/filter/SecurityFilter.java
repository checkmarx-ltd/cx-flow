package com.checkmarx.flow.filter;

import org.slf4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityFilter implements Filter {

    private static final String AUTH_HEADER = "Authorization";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SecurityFilter.class);

    public SecurityFilter() {
    }

    @Override

    public void init(FilterConfig filterConfig) throws ServletException {
        log.debug("Initiating WebFilter >> ");
    }

    /**
     * All requests pass through this Filter.  If a valid token is not present, HTTP Forbidden is returned
     *
     * @param request
     * @param response
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        //TODO Per-Request Validation as required (white list)
        HttpServletRequest req =  ((HttpServletRequest)request);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.debug("Destroying WebFilter >> ");
    }
}