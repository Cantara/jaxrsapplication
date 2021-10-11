package no.cantara.jaxrsapp.security;

import no.cantara.security.authentication.Authentication;

import java.security.Principal;

public class JaxRsAppPrincipal implements Principal {

    private final Authentication authentication;

    public JaxRsAppPrincipal(Authentication authentication) {
        this.authentication = authentication;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public String getName() {
        return authentication.ssoId();
    }
}
