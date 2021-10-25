package no.cantara.security.authentication;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.authentication.whydah.WhydahApplicationCredentialStore;
import no.cantara.security.authentication.whydah.WhydahAuthenticationManager;
import no.cantara.security.authentication.whydah.WhydahSecurityProperties;

import java.util.Arrays;

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
        String oauth2Uri = applicationProperties.get(WhydahSecurityProperties.WHYDAH_OAUTH2_URI);
        String filteredRoleNames = applicationProperties.get(WhydahSecurityProperties.WHYDAH_FILTERED_ROLENAMES, "");
        String[] roleNames = filteredRoleNames.split("[, ]");
        return new WhydahAuthenticationManager(Arrays.asList(roleNames), oauth2Uri, new WhydahApplicationCredentialStore(applicationProperties));
    }
}
