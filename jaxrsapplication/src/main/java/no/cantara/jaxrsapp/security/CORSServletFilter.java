package no.cantara.jaxrsapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class CORSServletFilter extends HttpFilter {

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
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        response.addHeader("Access-Control-Allow-Origin", origin);
        response.addHeader("Access-Control-Allow-Credentials", credentials);
        response.addHeader("Access-Control-Allow-Headers", headers);
        response.addHeader("Access-Control-Allow-Methods", methods);
        chain.doFilter(request, response);
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
