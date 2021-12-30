package no.cantara.security.authentication.whydah;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.authentication.AuthenticationManagerFactory;

public class WhydahAuthenticationManagerFactory implements AuthenticationManagerFactory {

    public static final String WHYDAH_AUTH_GROUP_USER_ROLE_NAME_FIX = "whydah_auth_group_user_role_name_fix";
    public static final String WHYDAH_AUTH_GROUP_APPLICATION_TAG_NAME = "whydah_auth_group_application_tag_name";

    public static final String DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME = "access-groups";
    public static final String DEFAULT_AUTH_GROUP_USER_ROLE_NAME_FIX = "access-groups";

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
        JaxRsWhydahSession jaxRsWhydahSession = WhydahApplicationSessionConfigurator.from(applicationProperties);
        DefaultWhydahService whydahService = new DefaultWhydahService(jaxRsWhydahSession.getApplicationSession());
        String whydahAuthGroupUserRoleNameFix = applicationProperties.get(WHYDAH_AUTH_GROUP_USER_ROLE_NAME_FIX, DEFAULT_AUTH_GROUP_USER_ROLE_NAME_FIX);
        String whydahAuthGroupApplicationTagName = applicationProperties.get(WHYDAH_AUTH_GROUP_APPLICATION_TAG_NAME, DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME);
        return new WhydahAuthenticationManager(oauth2Uri, jaxRsWhydahSession, whydahService, whydahAuthGroupUserRoleNameFix, whydahAuthGroupApplicationTagName);
    }
}
