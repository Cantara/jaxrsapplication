package no.cantara.security.authentication.test;

import no.cantara.security.authentication.ApplicationTokenSession;

public class FakeApplicationTokenSession implements ApplicationTokenSession {

    final String applicationId;

    public FakeApplicationTokenSession(String applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public String getApplicationToken() {
        return "fake-application-id: " + applicationId;
    }
}
