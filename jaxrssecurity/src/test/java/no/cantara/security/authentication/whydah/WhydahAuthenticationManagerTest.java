package no.cantara.security.authentication.whydah;

import edu.emory.mathcs.backport.java.util.Collections;
import net.whydah.sso.user.types.UserToken;
import no.cantara.security.authentication.ApplicationTag;
import no.cantara.security.authentication.AuthenticationManager;
import no.cantara.security.authentication.AuthenticationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WhydahAuthenticationManagerTest {

    final String appTokenXml = "<applicationtoken>\n" +
            "     <params>\n" +
            "         <applicationtokenID>2c14bf76cc4a78078bf216a815ed5cd1</applicationtokenID>\n" +
            "         <applicationid>899e0ae9b790765998c99bbe5</applicationid>\n" +
            "         <applicationname>Observation Flowtest</applicationname>\n" +
            "         <expires>1639503263640</expires>\n" +
            "     </params> \n" +
            "     <Url type=\"application/xml\" method=\"POST\"                 template=\"https://entrasso-qa.entraos.io/tokenservice/user/2c14bf76cc4a78078bf216a815ed5cd1/get_usertoken_by_usertokenid\"/> \n" +
            " </applicationtoken>";

    @Test
    public void thatAppTokenXmlIsRecognizedAsApplication() {
        AuthenticationManager authenticationManager = new WhydahAuthenticationManager(
                Collections.emptyList(), "", () -> "", new TestWhydahService(), WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_USER_ROLE_NAME, WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME
        );
        AuthenticationResult authenticationResult = authenticationManager.authenticate("Bearer " + appTokenXml);
        assertTrue(authenticationResult.isValid());
        assertTrue(authenticationResult.isApplication());
        assertEquals("testapp", authenticationResult.application().get().ssoId());
    }

    private static class TestWhydahService implements WhydahService {
        @Override
        public UserToken findUserTokenFromUserTokenId(String userTokenId) {
            return null;
        }

        @Override
        public String getUserTokenByUserTicket(String userticket) {
            return null;
        }

        @Override
        public String getApplicationIdFromApplicationTokenId(String applicationTokenId) {
            if ("2c14bf76cc4a78078bf216a815ed5cd1".equals(applicationTokenId)) {
                return "testapp";
            }
            return null;
        }

        @Override
        public List<ApplicationTag> getApplicationTagsFromApplicationTokenId(String applicationTokenId) {
            return null;
        }

        @Override
        public String createTicketForUserTokenID(String userTokenId) {
            return null;
        }

        @Override
        public boolean validateUserTokenId(String usertokenid) {
            return false;
        }
    }
}
