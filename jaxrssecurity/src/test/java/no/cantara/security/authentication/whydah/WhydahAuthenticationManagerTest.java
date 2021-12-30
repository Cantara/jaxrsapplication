package no.cantara.security.authentication.whydah;

import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.application.types.Tag;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.whydah.DEFCON;
import no.cantara.security.authentication.ApplicationAuthentication;
import no.cantara.security.authentication.ApplicationTag;
import no.cantara.security.authentication.AuthenticationManager;
import no.cantara.security.authentication.AuthenticationResult;
import no.cantara.security.authentication.UserAuthentication;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WhydahAuthenticationManagerTest {

    final String USERTOKEN_ID = UUID.randomUUID().toString();
    final String USERTICKET = UUID.randomUUID().toString();

    final String appTokenXml = String.format("<applicationtoken>\n" +
            "     <params>\n" +
            "         <applicationtokenID>2c14bf76cc4a78078bf216a815ed5cd1</applicationtokenID>\n" +
            "         <applicationid>899e0ae9b790765998c99bbe5</applicationid>\n" +
            "         <applicationname>Observation Flowtest</applicationname>\n" +
            "         <applicationtags>HIDDEN, JURISDICTION_NORWAY, My!uTag_Val!uue</applicationtags>\n" +
            "         <expires>%s</expires>\n" +
            "     </params> \n" +
            "     <Url type=\"application/xml\" method=\"POST\"                 template=\"https://entrasso-qa.entraos.io/tokenservice/user/2c14bf76cc4a78078bf216a815ed5cd1/get_usertoken_by_usertokenid\"/> \n" +
            " </applicationtoken>", System.currentTimeMillis() + 60 * 60 * 1000);
    final ApplicationToken applicationToken = ApplicationTokenMapper.fromXml(appTokenXml);

    @Test
    public void thatAppTokenXmlIsRecognizedAsApplication() {
        AuthenticationManager authenticationManager = new WhydahAuthenticationManager(
                "", () -> "", new TestWhydahService(), WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_USER_ROLE_NAME_FIX, WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME
        );
        AuthenticationResult authenticationResult = authenticationManager.authenticate("Bearer " + appTokenXml);
        assertTrue(authenticationResult.isValid());
        assertTrue(authenticationResult.isApplication());
        ApplicationAuthentication applicationAuthentication = authenticationResult.application().get();
        assertEquals("testapp", applicationAuthentication.ssoId());
        assertEquals(3, applicationAuthentication.tags().size());
        assertEquals(Tag.DEFAULTNAME, applicationAuthentication.tags().get(0).getName());
        assertEquals("HIDDEN", applicationAuthentication.tags().get(0).getValue());
        assertEquals("JURISDICTION", applicationAuthentication.tags().get(1).getName());
        assertEquals("NORWAY", applicationAuthentication.tags().get(1).getValue());
        assertEquals("My_Tag", applicationAuthentication.tags().get(2).getName());
        assertEquals("Val_ue", applicationAuthentication.tags().get(2).getValue());
    }

    @Test
    public void thatAppTokenIdIsRecognizedAsApplication() {
        AuthenticationManager authenticationManager = new WhydahAuthenticationManager(
                "", () -> "", new TestWhydahService(), WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_USER_ROLE_NAME_FIX, WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME
        );
        AuthenticationResult authenticationResult = authenticationManager.authenticate("Bearer " + applicationToken.getApplicationTokenId());
        assertTrue(authenticationResult.isValid());
        assertTrue(authenticationResult.isApplication());
        ApplicationAuthentication applicationAuthentication = authenticationResult.application().get();
        assertEquals("testapp", applicationAuthentication.ssoId());
        assertEquals(3, applicationAuthentication.tags().size());
        assertEquals(Tag.DEFAULTNAME, applicationAuthentication.tags().get(0).getName());
        assertEquals("HIDDEN", applicationAuthentication.tags().get(0).getValue());
        assertEquals("JURISDICTION", applicationAuthentication.tags().get(1).getName());
        assertEquals("NORWAY", applicationAuthentication.tags().get(1).getValue());
        assertEquals("My_Tag", applicationAuthentication.tags().get(2).getName());
        assertEquals("Val_ue", applicationAuthentication.tags().get(2).getValue());
    }

    @Test
    public void thatUserTicketIsRecognizedAsUser() {
        AuthenticationManager authenticationManager = new WhydahAuthenticationManager(
                "", () -> "", new TestWhydahService(), WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_USER_ROLE_NAME_FIX, WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME
        );

        AuthenticationResult authenticationResult = authenticationManager.authenticate("Bearer " + USERTICKET);
        assertTrue(authenticationResult.isValid());
        assertTrue(authenticationResult.isUser());
        UserAuthentication userAuthentication = authenticationResult.user().get();

        assertEquals("MyUUIDValue", userAuthentication.ssoId());
        assertEquals(2, userAuthentication.groups().size());
        Set<String> groups = new LinkedHashSet<>(userAuthentication.groups());
        assertTrue(groups.contains("post"));
        assertTrue(groups.contains("pre"));
    }

    private class TestWhydahService implements WhydahService {
        @Override
        public UserToken findUserTokenFromUserTokenId(String userTokenId) {
            return null;
        }

        @Override
        public String getUserTokenByUserTicket(String userticket) {
            if (USERTICKET.equals(userticket)) {
                UserToken userToken = new UserToken();
                userToken.setUid("MyUUIDValue");
                userToken.setUserName("ola");
                userToken.setCellPhone("");
                userToken.setSecurityLevel("5");
                userToken.setNs2link("");
                userToken.setFirstName("Ola");
                userToken.setEmail("test@whydah.net");
                userToken.setLastName("Nordmann");
                userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));
                userToken.setLifespan("3000");
                userToken.setPersonRef("73637276722376");
                userToken.setDefcon(DEFCON.DEFCON5.toString());
                userToken.setUserTokenId(USERTOKEN_ID);
                userToken.setEncryptedSignature("");
                userToken.setEmbeddedPublicKey("");
                List<UserApplicationRoleEntry> roleList = new LinkedList<>();
                roleList.add(new UserApplicationRoleEntry("MyUUIDValue", "testapp", "Test-Application", "Cantara", "access-groups-post", "post"));
                roleList.add(new UserApplicationRoleEntry("MyUUIDValue", "testapp", "Test-Application", "Cantara", "pre-access-groups", "pre"));
                userToken.setRoleList(roleList);
                String xml = UserTokenMapper.toXML(userToken);
                return xml;
            }
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
            if ("2c14bf76cc4a78078bf216a815ed5cd1".equals(applicationTokenId)) {
                return applicationToken.getTags().stream()
                        .map(tag -> new ApplicationTag(tag.getName(), tag.getValue()))
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        @Override
        public String createTicketForUserTokenID(String userTokenId) {
            return null;
        }

        @Override
        public boolean validateUserTokenId(String usertokenid) {
            if (USERTOKEN_ID.equals(usertokenid)) {
                return true;
            }
            return false;
        }
    }
}
