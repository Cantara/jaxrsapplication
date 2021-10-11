package no.cantara.security.authorization;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Role {

    private final String roleId;
    private final Policy policy;
    private final Set<Role> children;

    private Role(String roleId, Policy policy, Set<Role> children) {
        this.roleId = roleId;
        this.policy = policy;
        this.children = children;
    }

    public String getRoleId() {
        return roleId;
    }

    public Policy getPolicy() {
        return policy;
    }

    public Set<Role> getChildren() {
        return children;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        String roleId;
        List<Policy> policies = new ArrayList<>();
        Set<Role> children = new LinkedHashSet<>();

        private Builder() {
        }

        public Builder roleId(String roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder addPolicies(Iterable<Policy> policies) {
            for (Policy policy : policies) {
                this.policies.add(policy);
            }
            return this;
        }

        public Builder addPolicy(Policy policy) {
            this.policies.add(policy);
            return this;
        }

        public Builder addChildren(Iterable<Role> children) {
            for (Role child : children) {
                this.children.add(child);
            }
            return this;
        }

        public Builder addChild(Role child) {
            this.children.add(child);
            return this;
        }

        public Role build() {
            Policy policy = Policy.builder()
                    .policyId(roleId)
                    .aggregate(Stream.concat(policies.stream(), children.stream().map(Role::getPolicy))
                            .collect(Collectors.toList()))
                    .build();
            return new Role(roleId, policy, children);
        }
    }
}
