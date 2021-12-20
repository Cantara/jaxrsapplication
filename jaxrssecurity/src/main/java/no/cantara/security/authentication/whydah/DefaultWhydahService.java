package no.cantara.security.authentication.whydah;

import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.commands.appauth.CommandGetApplicationIdFromApplicationTokenId;
import net.whydah.sso.commands.userauth.CommandCreateTicketForUserTokenID;
import net.whydah.sso.commands.userauth.CommandGetUserTokenByUserTicket;
import net.whydah.sso.commands.userauth.CommandGetUserTokenByUserTokenId;
import net.whydah.sso.commands.userauth.CommandRefreshUserToken;
import net.whydah.sso.commands.userauth.CommandValidateUserTokenId;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.UUID;

public class DefaultWhydahService implements WhydahService {

    public static final Logger log = LoggerFactory.getLogger(DefaultWhydahService.class);

    private final WhydahApplicationSession was;

    public DefaultWhydahService(WhydahApplicationSession was) {
        this.was = was;
    }

    public WhydahApplicationSession getWas() {
        return was;
    }

    @Override
    public UserToken findUserTokenFromUserTokenId(String userTokenId) {
        log.info("findUserTokenFromUserTokenId - Attempting to lookup usertokenId:" + userTokenId);
        String userTokenXml = "";
        try {
            UserToken userToken;
            URI tokenServiceUri = URI.create(was.getSTS());
            String appTokenId = was.getActiveApplicationTokenId();
            log.info("findUserTokenFromUserTokenId - Attempting to lookup apptoken:" + appTokenId);
            String oauth2proxyAppTokenXml = was.getActiveApplicationTokenXML();
            log.info("findUserTokenFromUserTokenId - Attempting to lookup oauth2proxyAppTokenXml:" + oauth2proxyAppTokenXml.replace("\n", ""));
            log.info("findUserTokenFromUserTokenId - Attempting to lookup (get_usertoken_by_usertokenid) tokenServiceUri:" + tokenServiceUri);

            new CommandRefreshUserToken(tokenServiceUri, appTokenId, was.getActiveApplicationTokenXML(), userTokenId).execute();
            userTokenXml = new CommandGetUserTokenByUserTokenId(tokenServiceUri, appTokenId, oauth2proxyAppTokenXml, userTokenId).execute();
            if (userTokenXml != null) {
                log.info("findUserTokenFromUserTokenId - Got lookup userTokenXml:" + userTokenXml.replace("\n", ""));
                userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
                log.info("findUserTokenFromUserTokenId - Got userToken:" + userToken);
                return userToken;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.warn("findUserTokenFromUserTokenId- Unable to parse userTokenXml returned from sts: " + userTokenXml.replace("\n", "") + "", e);
            return null;
        }
    }

    @Override
    public String getUserTokenByUserTicket(String userticket) {
        final String userTokenFromUserTokenId = new CommandGetUserTokenByUserTicket(URI.create(was.getSTS()),
                was.getActiveApplicationTokenId(),
                ApplicationCredentialMapper.toXML(was.getMyApplicationCredential()), userticket).execute();
        return userTokenFromUserTokenId;
    }

    @Override
    public String getApplicationIdFromApplicationTokenId(String applicationTokenId) {
        final String applicationId = new CommandGetApplicationIdFromApplicationTokenId(
                URI.create(was.getSTS()),
                applicationTokenId).execute();
        return applicationId;
    }

    @Override
    public String createTicketForUserTokenID(String userTokenId) {
        String forwardingUserTicket = UUID.randomUUID().toString();
        CommandCreateTicketForUserTokenID commandCreateTicketForUserTokenID = new CommandCreateTicketForUserTokenID(
                URI.create(was.getSTS()),
                was.getActiveApplicationTokenId(),
                ApplicationCredentialMapper.toXML(was.getMyApplicationCredential()),
                forwardingUserTicket,
                userTokenId
        );
        Boolean result = commandCreateTicketForUserTokenID.execute();
        if (result == null || !result) {
            return null;
        }
        return forwardingUserTicket;
    }

    @Override
    public boolean validateUserTokenId(String usertokenid) {
        final String securityTokenServiceUri = was.getSTS();
        final String applicationTokenId = was.getActiveApplicationTokenId();
        log.debug("Validate usertokenid using parameters {}, {}", securityTokenServiceUri, applicationTokenId);
        boolean okUserSession = new CommandValidateUserTokenId(URI.create(securityTokenServiceUri), applicationTokenId, usertokenid)
                .execute();
        return okUserSession;
    }
}