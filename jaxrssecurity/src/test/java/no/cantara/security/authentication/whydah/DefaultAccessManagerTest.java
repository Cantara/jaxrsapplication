package no.cantara.security.authentication.whydah;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.authentication.ApplicationAuthentication;
import no.cantara.security.authentication.ApplicationTag;
import no.cantara.security.authentication.CantaraApplicationAuthentication;
import no.cantara.security.authentication.CantaraUserAuthentication;
import no.cantara.security.authentication.UserAuthentication;
import no.cantara.security.authorization.DefaultAccessManager;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultAccessManagerTest {

    @Test
    public void thatUserAccessWorks() {
        DefaultAccessManager accessManager = new DefaultAccessManager("test", ApplicationProperties.builder()
                .values()
                .put("actions", "read,write")
                .put("policy.read-only.allow", "read")
                .put("role.reader.policies", "read-only")
                .put("group.readers.roles", "reader")
                .put("group.admins.roles", "superuser")
                .end()
                .build());

        UserAuthentication authenticatedJohn = new CantaraUserAuthentication("john-uid", "john", "utid-john", "custref-john", () -> "", () -> {
            Map<String, String> roleValueByName = new LinkedHashMap<>();
            roleValueByName.put("access-groups-for-unit-testing", "readers");
            return roleValueByName;
        }, "access-groups");
        assertTrue(accessManager.hasAccess(authenticatedJohn, "read"));
        assertFalse(accessManager.hasAccess(authenticatedJohn, "write"));

        UserAuthentication authenticatedSuperuser = new CantaraUserAuthentication("super-uid", "super", "utid-super", "custref-super", () -> "", () -> {
            Map<String, String> roleValueByName = new LinkedHashMap<>();
            roleValueByName.put("access-groups-for-unit-testing", "admins");
            return roleValueByName;
        }, "access-groups");
        assertTrue(accessManager.hasAccess(authenticatedSuperuser, "read"));
        assertTrue(accessManager.hasAccess(authenticatedSuperuser, "write"));

        ApplicationAuthentication authenticatedMyApp = new CantaraApplicationAuthentication("myapp", "dummy-token", () -> {
            List<ApplicationTag> tags = new LinkedList<>();
            tags.add(new ApplicationTag("access-groups", "readers"));
            return tags;
        }, "access-groups");
        assertTrue(accessManager.hasAccess(authenticatedMyApp, "read"));
        assertFalse(accessManager.hasAccess(authenticatedMyApp, "write"));

        ApplicationAuthentication authenticatedSuperApp = new CantaraApplicationAuthentication("superapp", "dummy-token", () -> {
            List<ApplicationTag> tags = new LinkedList<>();
            tags.add(new ApplicationTag("access-groups", "admins"));
            return tags;
        }, "access-groups");
        assertTrue(accessManager.hasAccess(authenticatedSuperApp, "read"));
        assertTrue(accessManager.hasAccess(authenticatedSuperApp, "write"));
    }

    @Test
    public void thatAccessWorksWhenUserAssignedGroupIsNotFound() {
        DefaultAccessManager accessManager = new DefaultAccessManager("test", ApplicationProperties.builder()
                .values()
                .put("actions", "read,write")
                .put("policy.read-only.allow", "read")
                .put("role.reader.policies", "read-only")
                .put("group.readers.roles", "reader")
                .put("group.admins.roles", "superuser")
                .end()
                .build());
        assertFalse(accessManager.userHasAccess("someuser", Collections.singletonList("badgroup"), "read"));
    }

    @Test
    public void thatAccessWorksWhenApplicationAssignedGroupIsNotFound() {
        DefaultAccessManager accessManager = new DefaultAccessManager("test", ApplicationProperties.builder()
                .values()
                .put("actions", "read,write")
                .put("policy.read-only.allow", "read")
                .put("role.reader.policies", "read-only")
                .put("group.readers.roles", "reader")
                .put("group.admins.roles", "superuser")
                .end()
                .build());
        assertFalse(accessManager.applicationHasAccess("someapplication", Collections.singletonList("badgroup"), "read"));
    }
}
