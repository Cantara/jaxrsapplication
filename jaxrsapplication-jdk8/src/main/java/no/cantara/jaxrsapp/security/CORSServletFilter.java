package no.cantara.jaxrsapp.security;


import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CORSServletFilter implements Filter {

    private final String origin;
    private final String credentials;
    private final String headers;
    private final String methods;

    private CORSServletFilter(String origin, String credentials, String headers, String methods) {
        this.origin = origin;
        this.credentials = credentials;
        this.headers = headers;
        this.methods = methods;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        response.addHeader("Access-Control-Allow-Origin", origin);
        response.addHeader("Access-Control-Allow-Credentials", credentials);
        response.addHeader("Access-Control-Allow-Headers", headers);
        response.addHeader("Access-Control-Allow-Methods", methods);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String origin = "*";
        private String credentials = "true";
        private String headers = "origin, content-type, accept, authorization";
        private String methods = "GET, POST, PUT, DELETE, OPTIONS, HEAD";

        private Builder() {
        }

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder credentials(String credentials) {
            this.credentials = credentials;
            return this;
        }

        public Builder headers(String headers) {
            this.headers = headers;
            return this;
        }

        public Builder methods(String methods) {
            this.methods = methods;
            return this;
        }

        public CORSServletFilter build() {
            return new CORSServletFilter(origin, credentials, headers, methods);
        }
    }
}
