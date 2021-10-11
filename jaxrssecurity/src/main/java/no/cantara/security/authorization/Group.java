package no.cantara.security.authorization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Group {

    private final String groupId;
    private final Policy policy;
    private final List<String> roleIds;

    private Group(String groupId, List<String> roleIds, Policy policy) {
        this.groupId = groupId;
        this.policy = policy;
        this.roleIds = roleIds;
    }

    public String getGroupId() {
        return groupId;
    }

    public Policy getPolicy() {
        return policy;
    }

    public List<String> getRoleIds() {
        return roleIds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        String groupId;
        List<Role> roles = new ArrayList<>();
        List<Policy> policies = new ArrayList<>();

        private Builder() {
        }

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder json(String groupJson) {
            JsonNode groupNode;
            try {
                groupNode = Jackson.mapper.readTree(groupJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return jackson(groupNode);
        }

        public Builder jackson(JsonNode groupNode) {
            if (!groupNode.has("groupId")) {
                groupId = UUID.randomUUID().toString();
            } else {
                JsonNode groupIdNode = groupNode.get("groupId");
                if (groupIdNode.isNull()) {
                    groupId = UUID.randomUUID().toString();
                } else {
                    groupId = groupIdNode.asText(UUID.randomUUID().toString());
                }
            }

            return this;
        }

        public Builder addRoles(Iterable<Role> roles) {
            for (Role role : roles) {
                this.roles.add(role);
            }
            return this;
        }

        public Builder addRole(Role role) {
            this.roles.add(role);
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

        public Group build() {
            List<String> roleIds = roles.stream().map(Role::getRoleId).collect(Collectors.toList());
            Policy policy = Policy.builder()
                    .policyId("aggregate-policy-for-group-" + groupId)
                    .aggregate(Stream.concat(roles.stream().map(Role::getPolicy), policies.stream()))
                    .build();
            return new Group(
                    groupId,
                    roleIds,
                    policy
            );

        }
    }
}
