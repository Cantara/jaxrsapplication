package no.cantara.security.authentication.test;

import no.cantara.security.authentication.ApplicationAuthentication;
import no.cantara.security.authentication.Authentication;
import no.cantara.security.authentication.AuthenticationManager;
import no.cantara.security.authentication.AuthenticationResult;
import no.cantara.security.authentication.UnauthorizedException;
import no.cantara.security.authentication.UserAuthentication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeAuthenticationManagerTest {

    @Test
    public void thatSimpleFakeUserPatternWorks() {
        Matcher m = FakeAuthenticationManager.fakeUserTokenPattern.matcher("Bearer fake-sso-id: oh.yes this is fake!, fake-customer-ref: so-fake-so-fake");
        assertTrue(m.matches());
        assertEquals("oh.yes this is fake!", m.group("ssoid"));
        assertEquals("so-fake-so-fake", m.group("customerref"));
    }

    @Test
    public void thatFullFakeUserPatternWorks() {
        Matcher m = FakeAuthenticationManager.fakeUserTokenPattern.matcher("Bearer fake-sso-id: oh.yes this is fake!, fake-username: bob, fake-usertoken-id: tokid52, fake-customer-ref: so-fake-so-fake, fake-roles: question=what,answer=42");
        assertTrue(m.matches());
        assertEquals("oh.yes this is fake!", m.group("ssoid"));
        assertEquals("bob", m.group("username"));
        assertEquals("tokid52", m.group("usertokenid"));
        assertEquals("so-fake-so-fake", m.group("customerref"));
        assertEquals("question=what,answer=42", m.group("roles"));
    }

    @Test
    public void thatFakeApplicationPatternWorks() {
        Matcher m = FakeAuthenticationManager.fakeApplicationTokenPattern.matcher("Bearer fake-application-id: a very fake application-id");
        assertTrue(m.matches());
        assertEquals("a very fake application-id", m.group("applicationid"));
    }

    @Test
    public void thatFakeAuthenticationReconizesPatterns() {
        AuthenticationManager manager = new FakeAuthenticationManager("fake-user", "fake-username", "fake-usertoken-id", "fake-customer-ref", "fake-application");
        {
            UserAuthentication userAuthentication = manager.authenticateAsUser("Bearer fake-sso-id: 4ug402jfn, fake-customer-ref: aertoh5893oi4ngf");
            assertEquals("4ug402jfn", userAuthentication.ssoId());
            assertEquals("aertoh5893oi4ngf", userAuthentication.customerRef());
        }
        {
            UserAuthentication userAuthentication = manager.authenticateAsUser("Bearer fake-sso-id: 4ug402jfn, fake-username: hei, fake-usertoken-id: token123, fake-customer-ref: aertoh5893oi4ngf, fake-roles: roleA=123,brol=456");
            assertEquals("4ug402jfn", userAuthentication.ssoId());
            assertEquals("hei", userAuthentication.username());
            assertEquals("token123", userAuthentication.usertokenId());
            assertEquals("aertoh5893oi4ngf", userAuthentication.customerRef());
            Map<String, String> expectedRoles = new LinkedHashMap<>();
            expectedRoles.put("roleA", "123");
            expectedRoles.put("brol", "456");
            assertEquals(expectedRoles, userAuthentication.roles());
        }
        {
            AuthenticationResult authenticationResult = manager.authenticate("Bearer fake-sso-id: 4ug402jfn, fake-username: hei, fake-usertoken-id: token123, fake-customer-ref: aertoh5893oi4ngf, fake-roles: roleA=123,brol=456");
            UserAuthentication userAuthentication = authenticationResult.user().orElse(null);
            assertEquals("4ug402jfn", userAuthentication.ssoId());
            assertEquals("hei", userAuthentication.username());
            assertEquals("token123", userAuthentication.usertokenId());
            assertEquals("aertoh5893oi4ngf", userAuthentication.customerRef());
            Map<String, String> expectedRoles = new LinkedHashMap<>();
            expectedRoles.put("roleA", "123");
            expectedRoles.put("brol", "456");
            assertEquals(expectedRoles, userAuthentication.roles());
        }
        {
            ApplicationAuthentication appAuthentication = manager.authenticateAsApplication("Bearer fake-application-id: ajihrgui57849hiu");
            assertEquals("ajihrgui57849hiu", appAuthentication.ssoId());
        }
        {
            Authentication authentication = manager.authenticate("Bearer fake-sso-id: 389ugieoi, fake-customer-ref: i3ognlf").authentication();
            assertTrue(UserAuthentication.class.isAssignableFrom(authentication.getClass()));
            assertEquals("389ugieoi", authentication.ssoId());
            assertEquals("i3ognlf", ((UserAuthentication) authentication).customerRef());
        }
        {
            Authentication authentication = manager.authenticate("Bearer fake-application-id: h4578guienakl").authentication();
            assertTrue(ApplicationAuthentication.class.isAssignableFrom(authentication.getClass()));
            assertEquals("h4578guienakl", authentication.ssoId());
        }
    }

    @Test
    public void thatFakeAuthenticationCanProduceFakeForwardingTokens() {
        AuthenticationManager manager = new FakeAuthenticationManager("fake-user", "fake-username", "fake-usertoken-id", "fake-customer-ref", "fake-application");
        UserAuthentication userAuthentication = manager.authenticateAsUser("Bearer fake-sso-id: 4ug402jfn, fake-usertoken-id: 7be22680-fdaf-4544-938e-33cf1b1bd91f, fake-customer-ref: aertoh5893oi4ngf, fake-roles: AB=C4");
        assertEquals("fake-sso-id: 4ug402jfn, fake-username: 4ug402jfn, fake-usertoken-id: 7be22680-fdaf-4544-938e-33cf1b1bd91f, fake-customer-ref: aertoh5893oi4ngf, fake-roles: AB=C4", userAuthentication.forwardingToken());
        ApplicationAuthentication appAuthentication = manager.authenticateAsApplication("Bearer fake-application-id: ajihrgui57849hiu");
        assertEquals("fake-application-id: ajihrgui57849hiu", appAuthentication.forwardingToken());
        Authentication authentication1 = manager.authenticate("Bearer fake-sso-id: 389ugieoi, fake-usertoken-id: 8f09800a-2722-41ab-9ed2-ce4c202e829f, fake-customer-ref: i3ognlf").authentication();
        assertEquals("fake-sso-id: 389ugieoi, fake-username: 389ugieoi, fake-usertoken-id: 8f09800a-2722-41ab-9ed2-ce4c202e829f, fake-customer-ref: i3ognlf, fake-roles: ", ((UserAuthentication) authentication1).forwardingToken());
        Authentication authentication2 = manager.authenticate("Bearer fake-application-id: h4578guienakl").authentication();
        assertEquals("fake-application-id: h4578guienakl", ((ApplicationAuthentication) authentication2).forwardingToken());
    }

    @Test
    public void thatFakeAuthenticationCanProduceEmptyRoleListWithoutException() {
        AuthenticationManager manager = new FakeAuthenticationManager("fake-user", "fake-username", "fake-usertoken-id", "fake-customer-ref", "fake-application");
        UserAuthentication userAuthentication = manager.authenticateAsUser("Bearer fake-sso-id: 4ug402jfn, fake-customer-ref: aertoh5893oi4ngf, fake-roles: ace=123,bee=2x");
        Map<String, String> expectedRoles = new LinkedHashMap<>();
        expectedRoles.put("ace", "123");
        expectedRoles.put("bee", "2x");
        assertEquals(expectedRoles, userAuthentication.roles());
    }

    @Test
    public void thatFakeAuthenticationProvidesDefaultFakeUserAndApplicationWhenHeaderOrTokenIsMissing() {
        AuthenticationManager manager = new FakeAuthenticationManager("fake-user", "fake-username", "fake-usertoken-id", "fake-customer", "fake-application");
        {
            UserAuthentication userAuthentication = manager.authenticateAsUser("").user().get();
            assertEquals("fake-user", userAuthentication.ssoId());
            assertEquals("fake-customer", userAuthentication.customerRef());
        }
        {
            ApplicationAuthentication applicationAuthentication = manager.authenticateAsApplication("").application().get();
            assertEquals("fake-application", applicationAuthentication.ssoId());
        }
        {
            AuthenticationResult result = manager.authenticate("");
            assertTrue(result.isValid());
            assertTrue(result.isApplication());
            assertFalse(result.isUser());
            ApplicationAuthentication applicationAuthentication = result.application().get();
            assertEquals("fake-application", applicationAuthentication.ssoId());
        }
    }

    @Test
    public void thatFakeAuthenticationEvaluatesUnauthorizedWhenSpecialTokenIsUsed() {
        AuthenticationManager manager = new FakeAuthenticationManager("fake-user", "fake-username", "fake-usertoken-id", "fake-customer", "fake-application");
        try {
            manager.authenticateAsUser(FakeAuthenticationManager.BEARER_TOKEN_UNAUTHORIZED);
            Assertions.fail("Did not throw expected UnauthorizedException");
        } catch (UnauthorizedException e) {
        }
        try {
            manager.authenticateAsApplication(FakeAuthenticationManager.BEARER_TOKEN_UNAUTHORIZED);
            Assertions.fail("Did not throw expected UnauthorizedException");
        } catch (UnauthorizedException e) {
        }
        AuthenticationResult result = manager.authenticate(FakeAuthenticationManager.BEARER_TOKEN_UNAUTHORIZED);
        assertFalse(result.isValid());
        assertFalse(result.isApplication());
        assertFalse(result.isUser());
        assertNull(result.authentication());
        assertFalse(result.application().isPresent());
        assertFalse(result.user().isPresent());
    }
}