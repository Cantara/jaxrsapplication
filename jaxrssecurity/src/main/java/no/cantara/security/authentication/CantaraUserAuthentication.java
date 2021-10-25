package no.cantara.security.authentication;

import java.time.Instant;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Supplier;

public class CantaraUserAuthentication implements UserAuthentication {

    private final String userId;
    private final String username;
    private final String usertokenId;
    private final String customerRefId;
    private final Supplier<String> forwardingTokenGenerator;
    private final Supplier<Map<String, String>> rolesSupplier;

    public CantaraUserAuthentication(String userId, String username, String usertokenId, String customerRefId, Supplier<String> forwardingTokenGenerator, Supplier<Map<String, String>> rolesSupplier) {
        this.userId = userId;
        this.username = username;
        this.usertokenId = usertokenId;
        this.customerRefId = customerRefId;
        this.forwardingTokenGenerator = forwardingTokenGenerator;
        this.rolesSupplier = rolesSupplier;
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
    public String username() {
        return username;
    }

    @Override
    public String usertokenId() {
        return usertokenId;
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

    @Override
    public Map<String, String> roles() {
        return rolesSupplier.get();
    }
}
