package no.cantara.security.authentication;

import java.time.Instant;
import java.util.StringJoiner;
import java.util.function.Supplier;

public class CantaraUserAuthentication implements UserAuthentication {

    private final String userId;
    private final String customerRefId;
    private final Supplier<String> forwardingTokenGenerator;

    public CantaraUserAuthentication(String userId, String customerRefId, Supplier<String> forwardingTokenGenerator) {
        this.userId = userId;
        this.customerRefId = customerRefId;
        this.forwardingTokenGenerator = forwardingTokenGenerator;
    }

    @Override
    public String ssoId() {
        return userId;
    }

    @Override
    public Instant expires() {
        return null;
    }

    public String customerRef() {
        return customerRefId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CantaraUserAuthentication.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("customerRefId='" + customerRefId + "'")
                .toString();
    }

    @Override
    public String forwardingToken() {
        return forwardingTokenGenerator.get();
    }
}
