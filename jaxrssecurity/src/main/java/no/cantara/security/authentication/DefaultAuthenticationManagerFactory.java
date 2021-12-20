package no.cantara.security.authentication;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.authentication.whydah.DefaultWhydahService;
import no.cantara.security.authentication.whydah.JaxRsWhydahSession;
import no.cantara.security.authentication.whydah.WhydahApplicationSessionConfigurator;
import no.cantara.security.authentication.whydah.WhydahAuthenticationManager;
import no.cantara.security.authentication.whydah.WhydahSecurityProperties;

import java.util.Arrays;
import java.util.List;

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
        List<String> roleNamesFilter = Arrays.asList(roleNames);
        JaxRsWhydahSession jaxRsWhydahSession = WhydahApplicationSessionConfigurator.from(applicationProperties);
        DefaultWhydahService whydahService = new DefaultWhydahService(jaxRsWhydahSession.getApplicationSession());
        return new WhydahAuthenticationManager(roleNamesFilter, oauth2Uri, jaxRsWhydahSession, whydahService);
    }
}
