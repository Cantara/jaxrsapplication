package no.cantara.security.authentication.whydah;

import no.cantara.config.ApplicationProperties;

import java.util.Objects;


public class WhydahApplicationSessionConfigurator {

    public static JaxRsWhydahSession from(ApplicationProperties properties) {
        String configuredWhydahBaseUri = properties.get(WhydahSecurityProperties.WHYDAH_URI);
        String configuredWhydahStsUri = properties.get("whydah_uri_sts");
        String configuredWhydahUasUri = properties.get("whydah_uri_uas");
        String normalizedWhydahBaseUri = normalizeBaseUri(configuredWhydahBaseUri);
        String securityTokenServiceUri;
        if (configuredWhydahStsUri != null) {
            securityTokenServiceUri = normalizeBaseUri(configuredWhydahStsUri);
        } else {
            securityTokenServiceUri = normalizedWhydahBaseUri + "tokenservice/";
        }
        String userAdminServiceUri;
        if (configuredWhydahUasUri != null) {
            userAdminServiceUri = normalizeBaseUri(configuredWhydahUasUri);
        } else {
            userAdminServiceUri = normalizedWhydahBaseUri + "useradminservice/";
        }
        String applicationId = properties.get(WhydahSecurityProperties.WHYDAH_APPLICATION_ID);
        String applicationName = properties.get(WhydahSecurityProperties.WHYDAH_APPLICATION_NAME);
        String applicationSecret = properties.get(WhydahSecurityProperties.WHYDAH_APPLICATION_SECRET);
        return new JaxRsWhydahSession(securityTokenServiceUri, userAdminServiceUri, applicationId, applicationName, applicationSecret);
    }

    public static String normalizeBaseUri(String baseUri) {
        Objects.requireNonNull(baseUri);
        String normalized = baseUri;
        // trim trailing slashes
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        // add single trailing slash
        if (normalized.length() > 0) {
            normalized = normalized + "/";
        }
        return normalized;
    }
}
