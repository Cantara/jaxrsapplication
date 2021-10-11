package no.cantara.jaxrsapp.security;

import no.cantara.security.authentication.Authentication;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class JaxRsAppSecurityContext implements SecurityContext {

    private final Authentication authentication;

    public JaxRsAppSecurityContext(Authentication authentication) {
        this.authentication = authentication;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public Principal getUserPrincipal() {
        return new JaxRsAppPrincipal(authentication);
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }
}
