package no.cantara.security.authentication;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.authentication.whydah.WhydahAuthenticationManager;
import no.cantara.security.authentication.whydah.WhydahAuthenticationManagerFactory;

public class DefaultAuthenticationManagerFactory implements AuthenticationManagerFactory {

    public DefaultAuthenticationManagerFactory() {
    }

    @Override
    public Class<?> providerClass() {
        return WhydahAuthenticationManager.class;
    }

    @Override
    public String alias() {
        return "default";
    }

    @Override
    public WhydahAuthenticationManager create(ApplicationProperties applicationProperties) {
        return new WhydahAuthenticationManagerFactory().create(applicationProperties);
    }
}
