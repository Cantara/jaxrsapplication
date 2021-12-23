package no.cantara.security.authentication.whydah;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.authentication.AuthenticationManagerFactory;

import java.util.Arrays;
import java.util.List;

public class WhydahAuthenticationManagerFactory implements AuthenticationManagerFactory {

    public static final String WHYDAH_AUTH_GROUP_USER_ROLE_NAME = "whydah_auth_group_user_role_name";
    public static final String WHYDAH_AUTH_GROUP_APPLICATION_TAG_NAME = "whydah_auth_group_user_role_name";

    public static final String DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME = "access-groups";
    public static final String DEFAULT_AUTH_GROUP_USER_ROLE_NAME = "access-groups";

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
        List<String> roleNamesFilter = Arrays.asList(roleNames);
        JaxRsWhydahSession jaxRsWhydahSession = WhydahApplicationSessionConfigurator.from(applicationProperties);
        DefaultWhydahService whydahService = new DefaultWhydahService(jaxRsWhydahSession.getApplicationSession());
        String whydahAuthGroupUserRoleName = applicationProperties.get(WHYDAH_AUTH_GROUP_USER_ROLE_NAME, DEFAULT_AUTH_GROUP_USER_ROLE_NAME);
        String whydahAuthGroupApplicationTagName = applicationProperties.get(WHYDAH_AUTH_GROUP_APPLICATION_TAG_NAME, DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME);
        return new WhydahAuthenticationManager(roleNamesFilter, oauth2Uri, jaxRsWhydahSession, whydahService, whydahAuthGroupUserRoleName, whydahAuthGroupApplicationTagName);
    }
}
