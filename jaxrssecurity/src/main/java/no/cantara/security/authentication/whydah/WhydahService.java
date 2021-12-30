package no.cantara.security.authentication.whydah;

import net.whydah.sso.user.types.UserToken;
import no.cantara.security.authentication.ApplicationTag;

import java.util.List;

public interface WhydahService {

    UserToken findUserTokenFromUserTokenId(String userTokenId);

    String getUserTokenByUserTicket(String userticket);

    String getApplicationIdFromApplicationTokenId(String applicationTokenId);

    List<ApplicationTag> getApplicationTagsFromApplicationTokenId(String applicationTokenId);

    String createTicketForUserTokenID(String userTokenId);

    boolean validateUserTokenId(String usertokenid);
}
