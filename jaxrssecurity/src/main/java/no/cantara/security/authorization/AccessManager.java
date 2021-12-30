package no.cantara.security.authorization;

import no.cantara.security.authentication.Authentication;

import java.util.List;

public interface AccessManager {

    boolean hasAccess(Authentication authentication, String action);

    boolean userHasAccess(String userId, List<String> assignedGroups, String action);

    boolean applicationHasAccess(String applicationId, List<String> assignedGroups, String action);
}
