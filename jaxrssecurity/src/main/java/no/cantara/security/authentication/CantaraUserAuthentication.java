package no.cantara.security.authentication;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CantaraUserAuthentication implements UserAuthentication {

    private final String userId;
    private final String username;
    private final String usertokenId;
    private final String customerRefId;
    private final Supplier<String> forwardingTokenGenerator;
    private final Supplier<Map<String, String>> rolesSupplier;
    private final AtomicReference<Map<String, String>> rolesRef = new AtomicReference<>();
    private final String accessGroupRoleNameFix;

    public CantaraUserAuthentication(String userId, String username, String usertokenId, String customerRefId, Supplier<String> forwardingTokenGenerator, Supplier<Map<String, String>> rolesSupplier, String accessGroupRoleNameFix) {
        this.userId = userId;
        this.username = username;
        this.usertokenId = usertokenId;
        this.customerRefId = customerRefId;
        this.forwardingTokenGenerator = forwardingTokenGenerator;
        this.rolesSupplier = rolesSupplier;
        this.accessGroupRoleNameFix = accessGroupRoleNameFix;
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
    public List<String> groups() {
        List<String> groups = new ArrayList<>();
        for (Map.Entry<String, String> entry : roles().entrySet()) {
            String roleName = entry.getKey();
            if (roleName.toLowerCase().startsWith(accessGroupRoleNameFix.toLowerCase())
                    || roleName.toLowerCase().endsWith(accessGroupRoleNameFix.toLowerCase())) {
                String accessGroups = entry.getValue();
                if (accessGroups != null) {
                    String[] parts = accessGroups.split("[ ,;:]+");
                    for (String part : parts) {
                        groups.add(part.trim());
                    }
                }
            }
        }
        return groups;
    }

    @Override
    public Map<String, String> roles() {
        Map<String, String> roles = rolesRef.get();
        if (roles == null) {
            roles = rolesSupplier.get();
            rolesRef.set(roles);
        }
        return roles;
    }
}
