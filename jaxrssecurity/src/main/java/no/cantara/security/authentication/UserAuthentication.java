package no.cantara.security.authentication;

public interface UserAuthentication extends Authentication {

    @Override
    default boolean isUser() {
        return true;
    }

    String customerRef();
}
