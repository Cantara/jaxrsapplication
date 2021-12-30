package no.cantara.security.authentication.whydah;

import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WhydahAuthenticationManager implements AuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(WhydahAuthenticationManager.class);
    private static Pattern appTokenIdPattern = Pattern.compile("<applicationtokenID>([^<]*)</applicationtokenID>");

    private final String oauth2Uri;
    private final ApplicationTokenSession applicationTokenSession;
    private final WhydahService whydahService;
    private final String whydahAuthGroupUserRoleNameFix;
    private final String whydahAuthGroupApplicationTagName;

    public WhydahAuthenticationManager(String oauth2Uri, ApplicationTokenSession applicationTokenSession, WhydahService whydahService, String whydahAuthGroupUserRoleNameFix, String whydahAuthGroupApplicationTagName) {
        this.oauth2Uri = oauth2Uri;
        this.applicationTokenSession = applicationTokenSession;
        this.whydahService = whydahService;
        this.whydahAuthGroupUserRoleNameFix = whydahAuthGroupUserRoleNameFix;
        this.whydahAuthGroupApplicationTagName = whydahAuthGroupApplicationTagName;
    }

    public WhydahService getWhydahService() {
        return whydahService;
    }

    public String getWhydahAuthGroupUserRoleNameFix() {
        return whydahAuthGroupUserRoleNameFix;
    }

    public String getWhydahAuthGroupApplicationTagName() {
        return whydahAuthGroupApplicationTagName;
    }

    @Override
    public UserAuthentication authenticateAsUser(final String authorizationHeader) throws UnauthorizedException {
        String authorization = authorizationHeader;
        if (authorization == null || authorization.isEmpty()) {
            throw new UnauthorizedException();
        }
        String usertokenid;
        String token = authorization.substring("Bearer ".length());
        String ssoId;
        String customerRef;
        Supplier<String> forwardingTokenGenerator = () -> token; // forwarding incoming token by default
        Supplier<Map<String, String>> rolesGenerator;
        if (token.length() > 50) {
            log.trace("Suspected JWT-token is {}", token);
        } else {
            log.trace("Suspected whydah-userticket is {}", token);
        }
        DecodedJWT decodedJWT = null;
        try {
            decodedJWT = JWT.decode(token);
            log.trace("token is a JWT token: {}", token);
        } catch (JWTDecodeException e) {
            log.trace("token is not a JWT token: {}", token);
        }
        if (decodedJWT != null) {
            try {
                log.debug("Resolving JWT-token");
                JwtHelper jwtUtils = new JwtHelper(oauth2Uri, token);
                ssoId = jwtUtils.getUserNameFromJwtToken();
                customerRef = jwtUtils.getCustomerRefFromJwtToken();
                usertokenid = jwtUtils.getUserTokenFromJwtToken();
                final String theUserTokenId = usertokenid;
                rolesGenerator = () -> {
                    UserToken userToken = whydahService.findUserTokenFromUserTokenId(theUserTokenId);
                    if (userToken == null) {
                        return Collections.emptyMap();
                    }
                    Map<String, String> roleValueByName = userToken.getRoleList()
                            .stream()
                            .collect(Collectors.toMap(UserApplicationRoleEntry::getRoleName, UserApplicationRoleEntry::getRoleValue));
                    return roleValueByName;
                };
                log.trace("Resolved JWT-token. Found customerRef {} and usertokenid {}", customerRef, usertokenid);
            } catch (JwkException e) {
                log.warn("Token {} throws JwkException during authentication {} ", token, e);
                throw new UnauthorizedException();
            }
        } else {
            try {
                log.debug("Resolving Whydah-userticket");
                String userticket = token;
                final String userTokenFromUserTokenId = whydahService.getUserTokenByUserTicket(userticket);
                if (userTokenFromUserTokenId != null && UserTokenMapper.validateUserTokenMD5Signature(userTokenFromUserTokenId)) {
                    final UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenFromUserTokenId);
                    usertokenid = userToken.getUserTokenId();
                    ssoId = userToken.getUid();
                    customerRef = userToken.getPersonRef();
                    final String theUserTokenId = usertokenid;
                    final String theSsoId = ssoId;
                    final String theCustomerRef = customerRef;
                    forwardingTokenGenerator = () -> {
                        String forwardingUserTicket = whydahService.createTicketForUserTokenID(theUserTokenId);
                        if (forwardingUserTicket == null) {
                            throw new RuntimeException(String.format("Unable to generate user-ticket for user. ssoId=%s, customerRef=%s", theSsoId, theCustomerRef));
                        }
                        return forwardingUserTicket;
                    };
                    rolesGenerator = () -> {
                        Map<String, String> roleValueByName = userToken.getRoleList()
                                .stream()
                                .collect(Collectors.toMap(UserApplicationRoleEntry::getRoleName, UserApplicationRoleEntry::getRoleValue));
                        return roleValueByName;
                    };
                    log.debug("Resolved Whydah-userticket. Found customerRef {} and usertokenid {}", customerRef, usertokenid);
                } else {
                    log.debug("Unresolved Whydah-userticket! Got usertoken {}", userTokenFromUserTokenId);
                    throw new UnauthorizedException();
                }
            } catch (Exception ex) {
                log.debug("Exception", ex);
                if (ex instanceof UnauthorizedException) {
                    throw ex;
                }
                log.info("Token {} throws Exception during whydah-authentication {} ", token, ex);
                throw new UnauthorizedException();
            }
        }

        if (usertokenid == null || usertokenid.isEmpty()) {
            log.debug("Usertoken is null or empty");
            throw new UnauthorizedException();
        }

        boolean okUserSession = whydahService.validateUserTokenId(usertokenid);

        if (ssoId == null || ssoId.isEmpty()) {
            log.debug("Unsucessful resolving of user-authentication, ssoId is null or empty. ssoid '{}'", ssoId);
            throw new UnauthorizedException();
        }
        if (customerRef == null || customerRef.isEmpty()) {
            log.debug("Unsucessful resolving of user-authentication, customerRef is null or empty. customerRef {}", customerRef);
            throw new UnauthorizedException();
        }
        if (!okUserSession) {
            log.debug("Unsucessful resolving of user-authentication, okUserSession false.");
            throw new UnauthorizedException();
        }
        log.debug("Successful user authentication");

        return new CantaraUserAuthentication(ssoId, ssoId, usertokenid, customerRef, forwardingTokenGenerator, rolesGenerator, whydahAuthGroupUserRoleNameFix);
    }


    @Override
    public ApplicationAuthentication authenticateAsApplication(final String authorizationHeader) throws
            UnauthorizedException {
        String authorization = authorizationHeader;
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }
        try {
            String token = authorization.substring("Bearer ".length());

            if (token.contains("<applicationtoken>")) {
                // assume that bearer token is application-token xml, extract the application-token-id from it
                Matcher m = appTokenIdPattern.matcher(token);
                if (m.find()) {
                    String applicationTokenId = m.group(1);
                    // Use only the application-token-id from the xml, we cannot trust anything else unless it was signed
                    final String applicationId = whydahService.getApplicationIdFromApplicationTokenId(applicationTokenId);
                    log.trace("Lookup application by applicationTokenId {}. Id found {}", token, applicationId);
                    if (applicationId != null) {
                        return new CantaraApplicationAuthentication(applicationId, applicationTokenId, () -> {
                            List<ApplicationTag> tags = whydahService.getApplicationTagsFromApplicationTokenId(applicationTokenId);
                            return tags;
                        }, whydahAuthGroupApplicationTagName);
                    }
                } else {
                    log.warn("Invalid application-token XML sent as bearer token. This is not a supported authentication mechanism. Token: {}", token);
                }
            } else {
                // assume that bearer token is application-token-id
                final String applicationId = whydahService.getApplicationIdFromApplicationTokenId(token);
                log.trace("Lookup application by applicationTokenId {}. Id found {}", token, applicationId);
                if (applicationId != null) {
                    return new CantaraApplicationAuthentication(applicationId, token, () -> {
                        List<ApplicationTag> tags = whydahService.getApplicationTagsFromApplicationTokenId(token);
                        return tags;
                    }, whydahAuthGroupApplicationTagName);
                }
            }
        } catch (Exception e) {
            log.error("Exception in attempt to resolve authorization, bearerToken: " + authorizationHeader, e);
        }
        return null;
    }

    @Override
    public AuthenticationResult authenticate(String authorizationHeader) {
        String authorization = authorizationHeader;
        if (authorization == null || authorization.isEmpty() || authorization.length() < 39) {
            return new CantaraAuthenticationResult(null);
        }
        String token = authorization.substring("Bearer ".length());

        if (token.contains("<applicationtoken>") || token.length() < 33) {
            try {
                ApplicationAuthentication applicationAuthentication = authenticateAsApplication(authorization);
                return new CantaraAuthenticationResult(applicationAuthentication);
            } catch (UnauthorizedException e) {
                return new CantaraAuthenticationResult(null);
            }
        }

        //Assume jwt or whydah token
        try {
            UserAuthentication userAuthentication = authenticateAsUser(authorization);
            return new CantaraAuthenticationResult(userAuthentication);
        } catch (UnauthorizedException e) {
            return new CantaraAuthenticationResult(null);
        }
    }

    @Override
    public ApplicationTokenSession getApplicationTokenSession() {
        return applicationTokenSession;
    }
}

