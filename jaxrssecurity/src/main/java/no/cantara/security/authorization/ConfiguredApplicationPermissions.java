package no.cantara.security.authorization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfiguredApplicationPermissions {

    private final String applicationId;
    private final List<Group> groups;
    private final List<Role> roles;
    private final List<Policy> policies;
    private final Policy effectivePolicy;

    public ConfiguredApplicationPermissions(String applicationId, Collection<Group> groups, Collection<Role> roles, Collection<Policy> policies) {
        this.applicationId = applicationId;
        this.groups = new ArrayList<>(groups);
        this.roles = new ArrayList<>(roles);
        this.policies = new ArrayList<>(policies);
        this.effectivePolicy = Policy.builder()
                .policyId("effective-policy-for-user-" + applicationId)
                .aggregate(groups.stream().map(Group::getPolicy))
                .aggregate(roles.stream().map(Role::getPolicy))
                .aggregate(policies.stream())
                .build();
    }

    public Policy getEffectivePolicy() {
        return effectivePolicy;
    }
}
