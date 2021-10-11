package no.cantara.security.authentication.test;

import no.cantara.security.authentication.ApplicationAuthentication;
import no.cantara.security.authentication.AuthenticationManager;
import no.cantara.security.authentication.AuthenticationResult;
import no.cantara.security.authentication.CantaraApplicationAuthentication;
import no.cantara.security.authentication.CantaraAuthenticationResult;
import no.cantara.security.authentication.CantaraUserAuthentication;
import no.cantara.security.authentication.UnauthorizedException;
import no.cantara.security.authentication.UserAuthentication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FakeAuthenticationManager implements AuthenticationManager {

    public static final String BEARER_TOKEN_UNAUTHORIZED = "Bearer unauthorized";

    static final Pattern fakeUserTokenPattern = Pattern.compile("Bearer\\s+fake-sso-id:\\s*(?<ssoid>[^,]*),\\s*fake-customer-ref:\\s*(?<customerref>.*)");
    static final Pattern fakeApplicationTokenPattern = Pattern.compile("Bearer\\s+fake-application-id:\\s*(?<applicationid>.*)");

    private final UserAuthentication fakeUser;
    private final ApplicationAuthentication fakeApplication;

    public FakeAuthenticationManager(String defaultFakeUserId, String defaultFakeCustomerRef, String defaultFakeApplicationId) {
        fakeUser = new CantaraUserAuthentication(defaultFakeUserId, defaultFakeCustomerRef, () -> String.format("fake-sso-id: %s, fake-customer-ref: %s", defaultFakeUserId, defaultFakeCustomerRef));
        fakeApplication = new CantaraApplicationAuthentication(defaultFakeApplicationId, String.format("fake-application-id: %s", defaultFakeApplicationId));
    }

    @Override
    public UserAuthentication authenticateAsUser(String authorizationHeader) throws UnauthorizedException {
        final String authorization = authorizationHeader;
        if (authorization == null || authorization.isEmpty()) {
            return fakeUser; // use default fake user when token is missing
        }
        if (authorization.equals(BEARER_TOKEN_UNAUTHORIZED)) {
            // special case where test intentionally attempts to set an illegal token
            throw new UnauthorizedException();
        }
        Matcher m = fakeUserTokenPattern.matcher(authorizationHeader);
        if (m.matches()) {
            String ssoId = m.group("ssoid");
            String customerRef = m.group("customerref");
            return new CantaraUserAuthentication(ssoId, customerRef, () -> String.format("fake-sso-id: %s, fake-customer-ref: %s", ssoId, customerRef));
        }
        return fakeUser;
    }

    @Override
    public ApplicationAuthentication authenticateAsApplication(String authorizationHeader) throws UnauthorizedException {
        final String authorization = authorizationHeader;
        if (authorization == null || authorization.isEmpty()) {
            return fakeApplication; // use default fake application when token is missing
        }
        if (authorization.equals(BEARER_TOKEN_UNAUTHORIZED)) {
            // special case where test intentionally attempts to set an illegal token
            throw new UnauthorizedException();
        }
        Matcher m = fakeApplicationTokenPattern.matcher(authorizationHeader);
        if (m.matches()) {
            String applicationid = m.group("applicationid");
            return new CantaraApplicationAuthentication(applicationid, String.format("fake-application-id: %s", applicationid));
        }
        return fakeApplication;
    }

    @Override
    public AuthenticationResult authenticate(String authorizationHeader) throws UnauthorizedException {
        final String authorization = authorizationHeader;
        if (authorization == null || authorization.isEmpty()) {
            return new CantaraAuthenticationResult(fakeApplication); // use default fake application when token is missing
        }
        if (authorization.equals(BEARER_TOKEN_UNAUTHORIZED)) {
            // special case where test intentionally attempts to set an illegal token
            return new CantaraAuthenticationResult(null);
        }
        Matcher userMatcher = fakeUserTokenPattern.matcher(authorizationHeader);
        if (userMatcher.matches()) {
            String ssoId = userMatcher.group("ssoid");
            String customerRef = userMatcher.group("customerref");
            return new CantaraAuthenticationResult(new CantaraUserAuthentication(ssoId, customerRef, () -> String.format("fake-sso-id: %s, fake-customer-ref: %s", ssoId, customerRef)));
        }
        Matcher appMatcher = fakeApplicationTokenPattern.matcher(authorizationHeader);
        if (appMatcher.matches()) {
            String applicationid = appMatcher.group("applicationid");
            return new CantaraAuthenticationResult(new CantaraApplicationAuthentication(applicationid, String.format("fake-application-id: %s", applicationid)));
        }
        return new CantaraAuthenticationResult(fakeApplication);
    }
}
