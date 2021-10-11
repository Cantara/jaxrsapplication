package no.cantara.security.authorization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfiguredUserPermissions {

    private final String userId;
    private final List<Group> groups;
    private final List<Role> roles;
    private final List<Policy> policies;
    private final Policy effectivePolicy;

    public ConfiguredUserPermissions(String userId, Collection<Group> groups, Collection<Role> roles, Collection<Policy> policies) {
        this.userId = userId;
        this.groups = new ArrayList<>(groups);
        this.roles = new ArrayList<>(roles);
        this.policies = new ArrayList<>(policies);
        this.effectivePolicy = Policy.builder()
                .policyId("effective-policy-for-user-" + userId)
                .aggregate(groups.stream().map(Group::getPolicy))
                .aggregate(roles.stream().map(Role::getPolicy))
                .aggregate(policies.stream())
                .build();
    }

    public Policy getEffectivePolicy() {
        return effectivePolicy;
    }
}
