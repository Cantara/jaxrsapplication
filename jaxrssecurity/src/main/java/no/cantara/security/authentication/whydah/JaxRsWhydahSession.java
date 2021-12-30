package no.cantara.security.authentication.whydah;

import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.session.WhydahApplicationSession2;
import no.cantara.security.authentication.ApplicationTokenSession;

public class JaxRsWhydahSession implements ApplicationTokenSession {
    private final WhydahApplicationSession2 applicationSession;
    private final ApplicationCredential applicationCredential;
    private final String securityTokenServiceUri;
    private final String userAdminServiceUri;

    public JaxRsWhydahSession(String securityTokenServiceUri, String userAdminServiceUri, String applicationId, String applicationName, String applicationSecret) {
        this.securityTokenServiceUri = securityTokenServiceUri;
        this.userAdminServiceUri = userAdminServiceUri;
        this.applicationCredential = new ApplicationCredential(applicationId, applicationName, applicationSecret);
        this.applicationSession = WhydahApplicationSession2.getInstance(securityTokenServiceUri, userAdminServiceUri, applicationCredential);
    }

    public WhydahApplicationSession2 getApplicationSession() {
        return applicationSession;
    }

    public String getSecurityTokenServiceUri() {
        return securityTokenServiceUri;
    }

    public String getUserAdminServiceUri() {
        return userAdminServiceUri;
    }

    public ApplicationCredential getApplicationCredential() {
        return applicationCredential;
    }

    @Override
    public String getApplicationToken() {
        return applicationSession.getActiveApplicationTokenId();
    }
}
