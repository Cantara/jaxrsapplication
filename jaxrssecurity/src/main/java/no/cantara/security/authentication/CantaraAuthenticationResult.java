package no.cantara.security.authentication;

public class CantaraAuthenticationResult implements AuthenticationResult {

    private final Authentication authentication;

    public CantaraAuthenticationResult(Authentication authentication) {
        this.authentication = authentication;
    }

    public boolean isValid() {
        return authentication != null;
    }

    public boolean isUser() {
        return isValid() && authentication instanceof UserAuthentication;
    }

    public boolean isApplication() {
        return isValid() && authentication instanceof ApplicationAuthentication;
    }

    public Authentication authentication() {
        return authentication;
    }
}
