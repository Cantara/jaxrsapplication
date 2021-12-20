package no.cantara.security.authentication.whydah;

import net.whydah.sso.user.types.UserToken;

public interface WhydahService {

    UserToken findUserTokenFromUserTokenId(String userTokenId);

    String getUserTokenByUserTicket(String userticket);

    String getApplicationIdFromApplicationTokenId(String applicationTokenId);

    String createTicketForUserTokenID(String userTokenId);

    boolean validateUserTokenId(String usertokenid);
}
