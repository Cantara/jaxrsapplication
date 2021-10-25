package no.cantara.security.authentication;

import java.util.Map;

public interface UserAuthentication extends Authentication {

    @Override
    default boolean isUser() {
        return true;
    }

    String customerRef();

    String username();

    String usertokenId();

    Map<String, String> roles();
}
