package no.cantara.security.authentication;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.whydah.WhydahApplicationCredentialStore;
import no.cantara.security.whydah.WhydahSecurityProperties;

public class WhydahAuthenticationManagerFactory implements AuthenticationManagerFactory {

    public WhydahAuthenticationManagerFactory() {
    }

    @Override
    public Class<?> providerClass() {
        return WhydahAuthenticationManager.class;
    }

    @Override
    public String alias() {
        return "whydah";
    }

    @Override
    public WhydahAuthenticationManager create(ApplicationProperties applicationProperties) {
        String oauth2Uri = applicationProperties.get(WhydahSecurityProperties.WHYDAH_OAUTH2_URI);
        return new WhydahAuthenticationManager(oauth2Uri, new WhydahApplicationCredentialStore(applicationProperties));
    }
}
