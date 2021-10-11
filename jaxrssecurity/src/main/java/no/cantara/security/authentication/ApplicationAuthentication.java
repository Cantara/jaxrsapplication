package no.cantara.security.authentication;

public interface ApplicationAuthentication extends Authentication {

    @Override
    default boolean isApplication() {
        return true;
    }
}
