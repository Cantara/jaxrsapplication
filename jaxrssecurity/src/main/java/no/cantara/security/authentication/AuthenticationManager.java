package no.cantara.security.authentication;

public interface AuthenticationManager {

    UserAuthentication authenticateAsUser(String authorizationHeader) throws UnauthorizedException;

    ApplicationAuthentication authenticateAsApplication(String authorizationHeader) throws UnauthorizedException;

    AuthenticationResult authenticate(String authorizationHeader);
}
