package no.cantara.jaxrsapp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DefaultExceptionServletFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionServletFilter.class);

    public DefaultExceptionServletFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        try {
            chain.doFilter(request, response);
        } catch (Throwable t) {
            log.error(String.format("While attempting to serve: %s %s", request.getMethod(), request.getRequestURI()), t);
            response.sendError(500);
        }
    }

    @Override
    public void destroy() {
    }
}
