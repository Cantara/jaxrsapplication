package no.cantara.security.authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface Authentication {

    String ssoId();

    Instant expires();

    String forwardingToken();

    List<String> groups();

    default boolean isApplication() {
        return false;
    }

    default boolean isUser() {
        return false;
    }

    default Optional<ApplicationAuthentication> application() {
        if (isApplication()) {
            return Optional.of((ApplicationAuthentication) this);
        }
        return Optional.empty();
    }

    default Optional<UserAuthentication> user() {
        if (isUser()) {
            return Optional.of((UserAuthentication) this);
        }
        return Optional.empty();
    }
}
