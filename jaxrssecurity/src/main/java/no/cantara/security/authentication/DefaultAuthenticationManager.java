package no.cantara.security.authentication;

import com.auth0.jwk.JwkException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.commands.appauth.CommandGetApplicationIdFromApplicationTokenId;
import net.whydah.sso.commands.userauth.CommandCreateTicketForUserTokenID;
import net.whydah.sso.commands.userauth.CommandGetUserTokenByUserTicket;
import net.whydah.sso.commands.userauth.CommandValidateUserTokenId;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import no.cantara.security.whydah.JwtUtils;
import no.cantara.security.whydah.WhydahApplicationCredentialStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.UUID;
import java.util.function.Supplier;

public class DefaultAuthenticationManager implements AuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultAuthenticationManager.class);

    private WhydahApplicationCredentialStore applicationCredentialStore;

    public DefaultAuthenticationManager(WhydahApplicationCredentialStore applicationCredentialStore) {
        this.applicationCredentialStore = applicationCredentialStore;
    }

    public UserAuthentication authenticateAsUser(String authorizationHeader) throws UnauthorizedException {
        return getCustomerRef(authorizationHeader);
    }

    public ApplicationAuthentication authenticateAsApplication(String authorizationHeader) throws UnauthorizedException {
        String authorization = authorizationHeader;
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }
        try {
            String token = authorization.substring("Bearer ".length());

            final String applicationId = new CommandGetApplicationIdFromApplicationTokenId(
                    URI.create(applicationCredentialStore.getSecurityTokenServiceUri()),
                    token).execute();

            log.trace("Lookup application by applicationTokenId {}. Id found {}", token, applicationId);
            if (applicationId != null) {
                return new CantaraApplicationAuthentication(applicationId, token);
            }
        } catch (Exception e) {
            log.error("Exception in attempt to resolve authorization, bearerToken: " + authorizationHeader, e);
        }
        return null;
    }

    public AuthenticationResult authenticate(String authorizationHeader) {
        String authorization = authorizationHeader;
        if (authorization == null || authorization.isEmpty() || authorization.length() < 39) {
            return new CantaraAuthenticationResult(null);
        }
        String token = authorization.substring("Bearer ".length());

        if (token.length() < 33) {
            //Assume application-token-id
            try {
                ApplicationAuthentication applicationAuthentication = authenticateAsApplication(authorization);
                return new CantaraAuthenticationResult(applicationAuthentication);
            } catch (UnauthorizedException e) {
                return new CantaraAuthenticationResult(null);
            }
        } else {
            //Assume jwt or whydah token
            try {
                UserAuthentication userAuthentication = authenticateAsUser(authorization);
                return new CantaraAuthenticationResult(userAuthentication);
            } catch (UnauthorizedException e) {
                return new CantaraAuthenticationResult(null);
            }
        }
    }


    private UserAuthentication getCustomerRef(String bearerToken) throws UnauthorizedException {
        String authorization = bearerToken;
        if (authorization == null || authorization.isEmpty()) {
            throw new UnauthorizedException();
        }
        String usertokenid;
        String token = authorization.substring("Bearer ".length());
        String ssoId;
        String customerRef;
        Supplier<String> forwardingTokenGenerator = () -> token; // forwarding incoming token by default
        if (token.length() > 50) {
            log.debug("Suspected JWT-token is {}", token);
        } else {
            log.debug("Suspected whydah-userticket is {}", token);
        }
        try {
            log.debug("Resolving JWT-token");
            ssoId = JwtUtils.getUserNameFromJwtToken(token);
            customerRef = JwtUtils.getCustomerRefFromJwtToken(token);
            usertokenid = JwtUtils.getUserTokenFromJwtToken(token);
            log.debug("Resolved JWT-token. Found customerRef {} and usertokenid {}", customerRef, usertokenid);
        } catch (JWTDecodeException e) {
            log.debug("JWTDecoding threw exception", e);
            try {
                log.debug("Resolving Whydah-userticket");
                String userticket = token;

                final String userTokenFromUserTokenId = new CommandGetUserTokenByUserTicket(URI.create(applicationCredentialStore.getSecurityTokenServiceUri()),
                        applicationCredentialStore.getApplicationTokenId(),
                        ApplicationCredentialMapper.toXML(applicationCredentialStore.getApplicationCredential()), userticket).execute();
                if (userTokenFromUserTokenId != null && UserTokenMapper.validateUserTokenMD5Signature(userTokenFromUserTokenId)) {
                    final UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenFromUserTokenId);
                    usertokenid = userToken.getUserTokenId();
                    ssoId = userToken.getUid();
                    customerRef = userToken.getPersonRef();
                    final String theUserTokenId = usertokenid;
                    final String theSsoId = ssoId;
                    final String theCustomerRef = customerRef;
                    forwardingTokenGenerator = () -> {
                        String forwardingUserTicket = UUID.randomUUID().toString();
                        CommandCreateTicketForUserTokenID commandCreateTicketForUserTokenID = new CommandCreateTicketForUserTokenID(
                                URI.create(applicationCredentialStore.getSecurityTokenServiceUri()),
                                applicationCredentialStore.getApplicationTokenId(),
                                ApplicationCredentialMapper.toXML(applicationCredentialStore.getApplicationCredential()),
                                forwardingUserTicket,
                                theUserTokenId
                        );
                        Boolean result = commandCreateTicketForUserTokenID.execute();
                        if (result == null || !result) {
                            throw new RuntimeException(String.format("Unable to generate user-ticket for user. ssoId=%s, customerRef=%s", theSsoId, theCustomerRef));
                        }
                        return forwardingUserTicket;
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
        } catch (JwkException e) {
            log.info("Token {} throws JwkException during authentication {} ", token, e);
            throw new UnauthorizedException();
        }

        if (usertokenid == null || usertokenid.isEmpty()) {
            log.debug("Usertoken is null or empty");
            throw new UnauthorizedException();
        }

        final String securityTokenServiceUri = applicationCredentialStore.getSecurityTokenServiceUri();
        final String applicationTokenId = applicationCredentialStore.getApplicationTokenId();
        log.debug("Validate usertokenid using parameters {}, {}", securityTokenServiceUri, applicationTokenId);
        boolean okUserSession = new CommandValidateUserTokenId(URI.create(securityTokenServiceUri),
                applicationTokenId,
                usertokenid).execute();

        if (ssoId == null || ssoId.isEmpty() || customerRef == null || customerRef.isEmpty() || !okUserSession) {
            log.debug("Unsucessful resolving of authentication. ssoid {}, customerRef {}, usersession {} ", ssoId, customerRef, okUserSession);
            throw new UnauthorizedException();
        }
        log.debug("Sucessful user authentication");

        return new CantaraUserAuthentication(ssoId, customerRef, forwardingTokenGenerator);
    }
}

