package no.cantara.security.authentication;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.whydah.WhydahApplicationCredentialStore;

public class DefaultAuthenticationManagerFactory implements AuthenticationManagerFactory {

    public DefaultAuthenticationManagerFactory() {
    }

    @Override
    public Class<?> providerClass() {
        return DefaultAuthenticationManager.class;
    }

    @Override
    public String alias() {
        return "default";
    }

    @Override
    public DefaultAuthenticationManager create(ApplicationProperties applicationProperties) {
        return new DefaultAuthenticationManager(new WhydahApplicationCredentialStore(applicationProperties));
    }
}
