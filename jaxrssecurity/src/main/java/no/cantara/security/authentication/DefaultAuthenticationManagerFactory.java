package no.cantara.security.authentication;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.whydah.WhydahApplicationCredentialStore;
import no.cantara.security.whydah.WhydahSecurityProperties;

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
        String oauth2Uri = applicationProperties.get(WhydahSecurityProperties.WHYDAH_OAUTH2_URI);
        return new DefaultAuthenticationManager(oauth2Uri, new WhydahApplicationCredentialStore(applicationProperties));
    }
}
