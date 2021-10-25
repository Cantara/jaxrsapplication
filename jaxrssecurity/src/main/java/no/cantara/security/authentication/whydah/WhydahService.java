package no.cantara.security.authentication.whydah;

import net.whydah.sso.commands.userauth.CommandGetUserTokenByUserTokenId;
import net.whydah.sso.commands.userauth.CommandRefreshUserToken;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class WhydahService {

    public static final Logger log = LoggerFactory.getLogger(WhydahService.class);

    private final WhydahApplicationSession was;

    public WhydahService(WhydahApplicationSession was) {
        this.was = was;
    }

    public WhydahApplicationSession getWas() {
        return was;
    }

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
}
