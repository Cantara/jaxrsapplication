package no.cantara.security.authentication.whydah;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.authentication.AuthenticationManagerFactory;

import java.util.Arrays;

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
        String filteredRoleNames = applicationProperties.get(WhydahSecurityProperties.WHYDAH_FILTERED_ROLENAMES, "");
        String[] roleNames = filteredRoleNames.split("[, ]");
        return new WhydahAuthenticationManager(Arrays.asList(roleNames), oauth2Uri, WhydahApplicationSessionConfigurator.from(applicationProperties));
    }
}
