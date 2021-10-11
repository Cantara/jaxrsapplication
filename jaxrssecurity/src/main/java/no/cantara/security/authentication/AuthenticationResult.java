package no.cantara.security.authentication;

import java.util.Optional;

public interface AuthenticationResult {

    /**
     * Whether the authentication attempt succeeded.
     *
     * @return true if the authentication attempt succeeded, false otherwise.
     */
    boolean isValid();

    /**
     * Whether the authenticated client is a user.
     *
     * @return true if authentication is valid and the authenticated client is a user.
     */
    boolean isUser();

    /**
     * Whether the authenticated client is an application.
     *
     * @return true if authentication is valid and the authenticated client is an application.
     */
    boolean isApplication();

    /**
     * The authenticated client, either an application or a user.
     *
     * @return the authenticated client or null if not authenticated.
     */
    Authentication authentication();

    /**
     * Get the authenticated application client.
     *
     * @return an optional with the authenticated application or empty if the authentication is not valid or is a user.
     */
    default Optional<ApplicationAuthentication> application() {
        if (isApplication()) {
            return Optional.of((ApplicationAuthentication) authentication());
        }
        return Optional.empty();
    }

    /**
     * Get the authenticated user client.
     *
     * @return an optional with the authenticated user or empty if the authentication is not valid or is an application.
     */
    default Optional<UserAuthentication> user() {
        if (isUser()) {
            return Optional.of((UserAuthentication) authentication());
        }
        return Optional.empty();
    }
}
