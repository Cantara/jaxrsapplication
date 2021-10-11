package no.cantara.security.authentication;

public interface AuthenticationManager {

    UserAuthentication authenticateAsUser(String bearerToken) throws UnauthorizedException;

    ApplicationAuthentication authenticateAsApplication(String bearerToken) throws UnauthorizedException;

    AuthenticationResult authenticate(String bearerToken);
}
