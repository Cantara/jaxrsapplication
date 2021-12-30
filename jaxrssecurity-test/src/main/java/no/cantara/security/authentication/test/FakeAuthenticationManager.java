package no.cantara.security.authentication.test;

import net.whydah.sso.application.mappers.ApplicationTagMapper;
import net.whydah.sso.application.types.Tag;
import no.cantara.security.authentication.ApplicationAuthentication;
import no.cantara.security.authentication.ApplicationTag;
import no.cantara.security.authentication.ApplicationTokenSession;
import no.cantara.security.authentication.AuthenticationManager;
import no.cantara.security.authentication.AuthenticationResult;
import no.cantara.security.authentication.CantaraApplicationAuthentication;
import no.cantara.security.authentication.CantaraAuthenticationResult;
import no.cantara.security.authentication.CantaraUserAuthentication;
import no.cantara.security.authentication.UnauthorizedException;
import no.cantara.security.authentication.UserAuthentication;
import no.cantara.security.authentication.whydah.WhydahAuthenticationManagerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FakeAuthenticationManager implements AuthenticationManager {

    public static final String BEARER_TOKEN_UNAUTHORIZED = "Bearer unauthorized";

    static final Pattern fakeUserTokenPattern = Pattern.compile("Bearer\\s+fake-sso-id:\\s*(?<ssoid>[^,]*),\\s*(?:fake-username:\\s*(?<username>[^,]*),)?\\s*(?:fake-usertoken-id:\\s*(?<usertokenid>[^,]*),)?\\s*fake-customer-ref:\\s*(?<customerref>[^,]*)(?:,\\s*fake-roles:\\s*(?<roles>.*))?");
    static final Pattern fakeApplicationTokenPattern = Pattern.compile("Bearer\\s+fake-application-id:\\s*(?<applicationid>[^,]*)(?:,\\s*fake-tags:\\s*(?<tags>.*))?");

    private final FakeApplicationTokenSession fakeApplicationTokenSession;
    private final UserAuthentication fakeUser;
    private final ApplicationAuthentication fakeApplication;

    public FakeAuthenticationManager(String selfApplicationId, String defaultFakeUserId, String defaultFakeUsername, String defaultFakeUsertokenId, String defaultFakeCustomerRef, String defaultFakeApplicationId) {
        fakeApplicationTokenSession = new FakeApplicationTokenSession(selfApplicationId);
        fakeUser = new CantaraUserAuthentication(defaultFakeUserId, defaultFakeUsername, defaultFakeUsertokenId, defaultFakeCustomerRef, () -> String.format("fake-sso-id: %s, fake-customer-ref: %s", defaultFakeUserId, defaultFakeCustomerRef), () -> {
            Map<String, String> roles = new LinkedHashMap<>();
            return roles;
        }, WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_USER_ROLE_NAME_FIX);
        fakeApplication = new CantaraApplicationAuthentication(defaultFakeApplicationId, String.format("fake-application-id: %s", defaultFakeApplicationId), Collections::emptyList, WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME);
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
            return createUserFromMatch(m);
        }
        return fakeUser;
    }

    private CantaraUserAuthentication createUserFromMatch(Matcher m) {
        String ssoId = m.group("ssoid");
        String username = m.group("username");
        if (username == null) {
            username = ssoId;
        }
        String theUsername = username;
        String usertokenId = m.group("usertokenid");
        if (usertokenId == null) {
            usertokenId = UUID.randomUUID().toString();
        }
        String theUsertokenId = usertokenId;
        String customerRef = m.group("customerref");
        String fakeRoles = m.group("roles");
        Supplier<String> forwardingTokenGenerator = () -> {
            return String.format("fake-sso-id: %s, fake-username: %s, fake-usertoken-id: %s, fake-customer-ref: %s, fake-roles: %s", ssoId, theUsername, theUsertokenId, customerRef, fakeRoles != null ? fakeRoles : "");
        };
        Supplier<Map<String, String>> rolesSupplier = () -> {
            Map<String, String> roleValueByName = new LinkedHashMap<>();
            for (String keyValuePair : fakeRoles.split(",")) {
                String[] keyAndValue = keyValuePair.split("=");
                if (!keyAndValue[0].trim().isEmpty()) {
                    roleValueByName.put(keyAndValue[0].trim(), keyAndValue[1].trim());
                }
            }
            return roleValueByName;
        };
        return new CantaraUserAuthentication(ssoId, username, usertokenId, customerRef, forwardingTokenGenerator, rolesSupplier, WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_USER_ROLE_NAME_FIX);
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
            return createApplicationFromMatch(m);
        }
        return fakeApplication;
    }

    private CantaraApplicationAuthentication createApplicationFromMatch(Matcher m) {
        String applicationid = m.group("applicationid");
        String tags = m.group("tags");
        List<Tag> whydahTagList = ApplicationTagMapper.getTagList(tags);
        List<ApplicationTag> tagList = whydahTagList.stream().map(tag -> new ApplicationTag(tag.getName(), tag.getValue())).collect(Collectors.toList());
        return new CantaraApplicationAuthentication(applicationid, String.format("fake-application-id: %s", applicationid), () -> tagList, WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME);
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
            return new CantaraAuthenticationResult(createUserFromMatch(userMatcher));
        }
        Matcher appMatcher = fakeApplicationTokenPattern.matcher(authorizationHeader);
        if (appMatcher.matches()) {
            return new CantaraAuthenticationResult(createApplicationFromMatch(appMatcher));
        }
        return new CantaraAuthenticationResult(fakeApplication);
    }

    @Override
    public ApplicationTokenSession getApplicationTokenSession() {
        return fakeApplicationTokenSession;
    }
}
