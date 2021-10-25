package no.cantara.security.authentication.whydah;

import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.session.WhydahApplicationSession;
import no.cantara.config.ApplicationProperties;


public class WhydahApplicationCredentialStore {
    private final WhydahApplicationSession applicationSession;
    private final ApplicationCredential applicationCredential;
    private final String securityTokenServiceUri;
    private final String userAdminServiceUri;


    public WhydahApplicationCredentialStore(ApplicationProperties properties) {
        String whydahUri = properties.get(WhydahSecurityProperties.WHYDAH_URI);
        securityTokenServiceUri = whydahUri + "tokenservice/";
        userAdminServiceUri = whydahUri + "useradminservice/";
        String applicationId = properties.get(WhydahSecurityProperties.WHYDAH_APPLICATION_ID);
        String applicationName = properties.get(WhydahSecurityProperties.WHYDAH_APPLICATION_NAME);
        String applicationSecret = properties.get(WhydahSecurityProperties.WHYDAH_APPLICATION_SECRET);

        this.applicationCredential = new ApplicationCredential(applicationId, applicationName, applicationSecret);
        this.applicationSession = WhydahApplicationSession.getInstance(securityTokenServiceUri, userAdminServiceUri, applicationCredential);
    }

    public String getApplicationTokenId() {
        return applicationSession.getActiveApplicationTokenId();
    }

    public WhydahApplicationSession getApplicationSession() {
        return applicationSession;
    }

    public String getSecurityTokenServiceUri() {
        return securityTokenServiceUri;
    }

    public ApplicationCredential getApplicationCredential() {
        return applicationCredential;
    }
}
