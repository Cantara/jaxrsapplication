package no.cantara.security.authorization;

import no.cantara.security.authentication.Authentication;

public interface AccessManager {

    boolean hasAccess(Authentication authentication, String action);

    boolean userHasAccess(String userId, String action);

    boolean applicationHasAccess(String applicationId, String action);
}
