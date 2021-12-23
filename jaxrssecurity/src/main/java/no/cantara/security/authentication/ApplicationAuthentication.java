package no.cantara.security.authentication;

import java.util.List;

public interface ApplicationAuthentication extends Authentication {

    @Override
    default boolean isApplication() {
        return true;
    }

    List<ApplicationTag> tags();
}
