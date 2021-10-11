package no.cantara.security.authentication;

import java.time.Instant;
import java.util.StringJoiner;

public class CantaraApplicationAuthentication implements ApplicationAuthentication {
    private final String applicationId;
    private String forwardingToken;

    public CantaraApplicationAuthentication(String applicationId, String forwardingToken) {
        this.applicationId = applicationId;
        this.forwardingToken = forwardingToken;
    }

    @Override
    public String ssoId() {
        return applicationId;
    }

    @Override
    public Instant expires() {
        return null;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CantaraApplicationAuthentication.class.getSimpleName() + "[", "]")
                .add("applicationId='" + applicationId + "'")
                .toString();
    }

    @Override
    public String forwardingToken() {
        return forwardingToken;
    }
}
