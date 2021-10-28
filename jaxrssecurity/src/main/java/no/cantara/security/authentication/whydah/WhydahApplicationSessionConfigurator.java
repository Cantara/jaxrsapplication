package no.cantara.security.authentication.whydah;

import no.cantara.config.ApplicationProperties;

import java.util.Objects;


public class WhydahApplicationSessionConfigurator {

    public static JaxRsWhydahSession from(ApplicationProperties properties) {
        String configuredWhydahBaseUri = properties.get(WhydahSecurityProperties.WHYDAH_URI);
        String normalizedWhydahBaseUri = normalizeBaseUri(configuredWhydahBaseUri);
        String securityTokenServiceUri = normalizedWhydahBaseUri + "tokenservice/";
        String userAdminServiceUri = normalizedWhydahBaseUri + "useradminservice/";
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
