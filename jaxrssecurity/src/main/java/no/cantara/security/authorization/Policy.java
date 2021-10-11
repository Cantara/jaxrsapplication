package no.cantara.security.authorization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Policy {

    final String policyId;
    final Map<Action, Permission> permissionByAction;

    private Policy(String policyId, Map<Action, Permission> permissionByAction) {
        this.policyId = policyId;
        this.permissionByAction = new LinkedHashMap<>(permissionByAction);
    }

    public String getPolicyId() {
        return policyId;
    }

    public String toJson() {
        return toJackson().toPrettyString();
    }

    public ObjectNode toJackson() {
        ObjectNode policyNode = Jackson.mapper.createObjectNode();
        policyNode.put("policyId", policyId);
        List<Action> allowedActions = new ArrayList<>(permissionByAction.size());
        List<Action> deniedActions = new ArrayList<>(permissionByAction.size());
        for (Map.Entry<Action, Permission> entry : permissionByAction.entrySet()) {
            Action action = entry.getKey();
            Permission permission = entry.getValue();
            if (permission == Permission.ALLOW) {
                allowedActions.add(action);
            } else {
                deniedActions.add(action);
            }
        }
        if (allowedActions.size() > 0) {
            ArrayNode allow = policyNode.putArray("allow");
            for (Action action : allowedActions) {
                allow.add(action.toString());
            }
        }
        if (deniedActions.size() > 0) {
            ArrayNode deny = policyNode.putArray("deny");
            for (Action action : deniedActions) {
                deny.add(action.toString());
            }
        }
        return policyNode;
    }

    public PermissionDecision decidePermission(Action action) {
        if ("*".equals(action.getService()) || "*".equals(action.getAction())) {
            throw new IllegalArgumentException("Cannot check action: " + action);
        }

        Permission allActionsOnAllServicesPermission = permissionByAction.get(Action.ALL_ACTIONS_ON_ALL_SERVICES);
        if (Permission.DENY == allActionsOnAllServicesPermission) {
            return PermissionDecision.EXPLICITLY_DENIED; // policy explicitly denies all actions on all services
        }
        Action allActionsOnService = new Action(action.getService(), "*");
        Permission allActionsOnServicePermission = permissionByAction.get(allActionsOnService);
        if (Permission.DENY == allActionsOnServicePermission) {
            return PermissionDecision.EXPLICITLY_DENIED; // policy explicitly denies all actions on service
        }
        Permission actionPermission = permissionByAction.get(action);
        if (Permission.DENY == actionPermission) {
            return PermissionDecision.EXPLICITLY_DENIED; // policy explicitly denies action on service
        }

        if (Permission.ALLOW == allActionsOnAllServicesPermission) {
            return PermissionDecision.ALLOWED; // policy allows all actions on all services
        }
        if (Permission.ALLOW == allActionsOnServicePermission) {
            return PermissionDecision.ALLOWED; // policy allows all actions on service
        }
        if (Permission.ALLOW == actionPermission) {
            return PermissionDecision.ALLOWED; // policy allows action on service
        }

        return PermissionDecision.IMPLICITLY_DENIED; // default if no other deny or allow permissions are found
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        String policyId;
        Map<Action, Permission> permissionByAction = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder policyId(String policyId) {
            this.policyId = policyId;
            return this;
        }

        public Builder json(String policyJson) {
            try {
                return jackson(Jackson.mapper.readTree(policyJson));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public Builder jackson(JsonNode policyNode) {
            if (!policyNode.has("policyId")) {
                policyId = UUID.randomUUID().toString();
            } else {
                JsonNode policyIdNode = policyNode.get("policyId");
                if (policyIdNode.isNull()) {
                    policyId = UUID.randomUUID().toString();
                } else {
                    policyId = policyIdNode.asText(UUID.randomUUID().toString());
                }
            }
            permissionByAction.clear();
            if (policyNode.has("allow")) {
                JsonNode allowNode = policyNode.get("allow");
                if (allowNode.isArray()) {
                    ArrayNode allowArrayNode = (ArrayNode) allowNode;
                    for (JsonNode actionNode : allowArrayNode) {
                        if (actionNode.isTextual()) {
                            Action action = Action.from(actionNode.textValue());
                            permissionByAction.put(action, Permission.ALLOW);
                        }
                    }
                }
            }
            if (policyNode.has("deny")) {
                JsonNode allowNode = policyNode.get("deny");
                if (allowNode.isArray()) {
                    ArrayNode allowArrayNode = (ArrayNode) allowNode;
                    for (JsonNode actionNode : allowArrayNode) {
                        if (actionNode.isTextual()) {
                            Action action = Action.from(actionNode.textValue());
                            permissionByAction.put(action, Permission.DENY);
                        }
                    }
                }
            }
            return this;
        }

        public Builder allow(Iterable<Action> actions) {
            for (Action action : actions) {
                permissionByAction.put(action, Permission.ALLOW);
            }
            return this;
        }

        public Builder allow(Action action) {
            permissionByAction.put(action, Permission.ALLOW);
            return this;
        }

        public Builder allow(String action) {
            permissionByAction.put(Action.from(action), Permission.ALLOW);
            return this;
        }

        public Builder deny(Iterable<Action> deniedActions) {
            if (deniedActions != null) {
                for (Action action : deniedActions) {
                    permissionByAction.put(action, Permission.DENY);
                }
            }
            return this;
        }

        public Builder deny(Action action) {
            permissionByAction.put(action, Permission.DENY);
            return this;
        }

        public Builder deny(String action) {
            permissionByAction.put(Action.from(action), Permission.DENY);
            return this;
        }

        public Builder allowAllActionsOnService(String service) {
            permissionByAction.put(new Action(service, "*"), Permission.ALLOW);
            return this;
        }

        public Builder allowSomeActionsOnService(String service, String... someActions) {
            for (String someAction : someActions) {
                permissionByAction.put(new Action(service, someAction), Permission.ALLOW);
            }
            return this;
        }

        public Builder denySomeActionsOnService(String service, String... someActions) {
            for (String someAction : someActions) {
                permissionByAction.put(new Action(service, someAction), Permission.DENY);
            }
            return this;
        }

        public Builder denyAllActionsOnService(String service) {
            permissionByAction.put(new Action(service, "*"), Permission.DENY);
            return this;
        }

        public Builder aggregate(Policy... policies) {
            return aggregate(Arrays.asList(policies));
        }

        public Builder aggregate(Stream<Policy> policies) {
            return aggregate(policies.collect(Collectors.toList()));
        }

        public Builder aggregate(Iterable<Policy> policies) {
            for (Policy policy : policies) {
                for (Map.Entry<Action, Permission> entry : policy.permissionByAction.entrySet()) {
                    Action action = entry.getKey();
                    Permission permission = entry.getValue();
                    Permission existingPermission = permissionByAction.get(action);
                    if (existingPermission == null || existingPermission == Permission.ALLOW) {
                        permissionByAction.put(action, permission);
                    }
                }
            }
            return this;
        }

        public Policy build() {
            return new Policy(policyId, permissionByAction);
        }
    }
}
