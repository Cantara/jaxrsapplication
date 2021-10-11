package no.cantara.security.authorization;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.authentication.ApplicationAuthentication;
import no.cantara.security.authentication.Authentication;
import no.cantara.security.authentication.UserAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultAccessManager implements AccessManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultAccessManager.class);

    final String actionService;

    final Map<String, Role> roleById;
    final Map<String, Policy> policyById;
    final Map<String, Group> groupById;
    final Map<String, Action> actionByDefintion;

    final Map<String, ConfiguredUserPermissions> configuredUserPermissionsByUserId;
    final Map<String, ConfiguredApplicationPermissions> configuredApplicationPermissionsByApplicationId;

    final ConfiguredUserPermissions defaultUserPermissions;
    final ConfiguredApplicationPermissions defaultApplicationPermissions;

    /**
     * @param actionService       the service part of actions. e.g. if "abc" is passed, any {@link Action} instances made as
     *                            part of initializing this access-manager will get the service field as "abc".
     * @param authorizationConfig authorization configuration.
     */
    public DefaultAccessManager(String actionService, ApplicationProperties authorizationConfig) {
        this.actionService = actionService;
        this.actionByDefintion = initActions(authorizationConfig);
        this.policyById = initPolicies(authorizationConfig);
        this.roleById = initRoles(authorizationConfig);
        this.groupById = initGroups(authorizationConfig);
        this.configuredUserPermissionsByUserId = initUserPermissions(authorizationConfig);
        this.defaultUserPermissions = initDefaultUserPermissions(authorizationConfig);
        this.configuredApplicationPermissionsByApplicationId = initApplicationPermissions(authorizationConfig);
        this.defaultApplicationPermissions = initDefaultApplicationPermissions(authorizationConfig);
    }

    Map<String, Action> initActions(ApplicationProperties authorizationConfig) {
        Map<String, Action> actionByDefinition = new LinkedHashMap<>();
        String[] actionValues = authorizationConfig.get("actions").split(",");
        for (String actionValue : actionValues) {
            actionValue = actionValue.trim();
            if (!actionValue.isEmpty()) {
                Action action = new Action(actionService, actionValue);
                actionByDefinition.put(action.toString(), action);
            }
        }
        log.info("Added actions for service '{}': '{}'", actionService, actionByDefinition.values().stream().map(Action::getAction).collect(Collectors.joining(", ")));
        return actionByDefinition;
    }

    private void initSystemPolicies(Map<String, Policy> policyById) {
        Policy fullaccessPolicy = Policy.builder().policyId("fullaccess").allow(Action.ALL_ACTIONS_ON_ALL_SERVICES).build();
        policyById.put(fullaccessPolicy.getPolicyId(), fullaccessPolicy);
        logPolicyAdded(fullaccessPolicy, true);
        Policy noaccessPolicy = Policy.builder().policyId("noaccess").deny(Action.ALL_ACTIONS_ON_ALL_SERVICES).build();
        policyById.put(noaccessPolicy.getPolicyId(), noaccessPolicy);
        logPolicyAdded(noaccessPolicy, true);
        final Action ALL_ACTIONS_ON_SERVICE = new Action(actionService, "*");
        Policy serviceaccessPolicy = Policy.builder().policyId("serviceaccess").allow(ALL_ACTIONS_ON_SERVICE).build();
        policyById.put(serviceaccessPolicy.getPolicyId(), serviceaccessPolicy);
        logPolicyAdded(serviceaccessPolicy, true);
        Policy noserviceaccessPolicy = Policy.builder().policyId("noserviceaccess").deny(ALL_ACTIONS_ON_SERVICE).build();
        policyById.put(noserviceaccessPolicy.getPolicyId(), noserviceaccessPolicy);
        logPolicyAdded(noserviceaccessPolicy, true);
    }

    Map<String, Policy> initPolicies(ApplicationProperties authorizationConfig) {
        Map<String, Policy> policyById = new LinkedHashMap<>();
        initSystemPolicies(policyById);
        ApplicationProperties policyConfig = authorizationConfig.subTree("policy");
        Set<String> policyKeys = policyConfig.map().keySet();
        Set<String> policyIds = new LinkedHashSet<>();
        for (String policyKey : policyKeys) {
            int dotIndex = policyKey.indexOf('.');
            if (dotIndex > 0) {
                String policyId = policyKey.substring(0, dotIndex);
                policyIds.add(policyId);
            }
        }
        for (String policyId : policyIds) {
            ApplicationProperties policyProperties = policyConfig.subTree(policyId);
            Policy.Builder policyBuilder = Policy.builder()
                    .policyId(policyId);
            {
                String allowStr = policyProperties.get("allow");
                if (allowStr != null) {
                    String[] parts = allowStr.split(",");
                    for (String action : parts) {
                        action = action.trim();
                        if (!action.isEmpty()) {
                            policyBuilder.allow(new Action(actionService, action));
                        }
                    }
                }
            }
            {
                String denyStr = policyProperties.get("deny");
                if (denyStr != null) {
                    String[] parts = denyStr.split(",");
                    for (String action : parts) {
                        action = action.trim();
                        if (!action.isEmpty()) {
                            policyBuilder.deny(new Action(actionService, action));
                        }
                    }
                }
            }
            Policy policy = policyBuilder.build();
            policyById.put(policy.getPolicyId(), policy);
            logPolicyAdded(policy, false);
        }

        return policyById;
    }

    private void logPolicyAdded(Policy policy, boolean system) {
        log.info("Added {}policy '{}'. allow: '{}', deny: '{}'", system ? "system " : "", policy.getPolicyId(),
                policy.permissionByAction.entrySet().stream().filter(e -> e.getValue() == Permission.ALLOW).map(Map.Entry::getKey).map(Action::toString).collect(Collectors.joining(",")),
                policy.permissionByAction.entrySet().stream().filter(e -> e.getValue() == Permission.DENY).map(Map.Entry::getKey).map(Action::toString).collect(Collectors.joining(",")));
    }

    private void initSystemRoles(Map<String, Role> roleById) {
        Role.Builder superUserRoleBuilder = Role.builder();
        Role superuserRole = superUserRoleBuilder
                .roleId("superuser")
                .addPolicy(policyById.get("fullaccess"))
                .build();
        roleById.put(superuserRole.getRoleId(), superuserRole);
        logRoleAdded(superUserRoleBuilder, true);
    }

    Map<String, Role> initRoles(ApplicationProperties authorizationConfig) {
        Map<String, Role> roleById = new LinkedHashMap<>();
        initSystemRoles(roleById);
        Map<String, Role.Builder> roleBuilderById = new LinkedHashMap<>();
        Map<String, List<String>> roleChildrenByRoleId = new LinkedHashMap<>();

        ApplicationProperties roleConfig = authorizationConfig.subTree("role");
        Set<String> roleKeys = roleConfig.map().keySet();
        Set<String> roleIds = new LinkedHashSet<>();
        for (String roleKey : roleKeys) {
            int dotIndex = roleKey.indexOf('.');
            if (dotIndex > 0) {
                String roleId = roleKey.substring(0, dotIndex);
                roleIds.add(roleId);
            }
        }

        for (String roleId : roleIds) {
            ApplicationProperties roleProperties = roleConfig.subTree(roleId);
            Role.Builder roleBuilder = Role.builder()
                    .roleId(roleId);
            roleBuilderById.put(roleId, roleBuilder);
            {
                String policiesStr = roleProperties.get("policies");
                if (policiesStr != null) {
                    String[] parts = policiesStr.split(",");
                    for (String policyId : parts) {
                        policyId = policyId.trim();
                        if (!policyId.isEmpty()) {
                            Policy policy = policyById.get(policyId);
                            if (policy == null) {
                                throw new IllegalArgumentException(
                                        String.format("Role mis-configuration. Property 'role.%s.policies=%s' refers to policyId '%s' which must be configured with either 'policy.%s.allow=actions...' and/or 'policy.%s.deny=actions...'",
                                                roleId, policiesStr, policyId, policyId, policyId));
                            }
                            roleBuilder.addPolicy(policy);
                        }
                    }
                }
            }
            {
                List<String> childrenRoleIds = new ArrayList<>();
                String childrenStr = roleProperties.get("children");
                if (childrenStr != null) {
                    String[] parts = childrenStr.split(",");
                    for (String childRoleId : parts) {
                        childRoleId = childRoleId.trim();
                        if (!childRoleId.isEmpty()) {
                            childrenRoleIds.add(childRoleId);
                        }
                    }
                }
                roleChildrenByRoleId.put(roleId, childrenRoleIds);
            }
        }

        Set<String> parents = new LinkedHashSet<>();
        for (String roleId : roleBuilderById.keySet()) {
            Role role = roleById.get(roleId);
            if (role == null) {
                role = recursiveBuildAndAddRole(parents, roleId, roleById, roleBuilderById, roleChildrenByRoleId);
            }
        }

        return roleById;
    }

    Role recursiveBuildAndAddRole(Set<String> parents, String roleId, Map<String, Role> roleById, Map<String, Role.Builder> roleBuilderById, Map<String, List<String>> roleChildrenByRoleId) {
        if (parents.contains(roleId)) {
            throw new IllegalArgumentException(String.format("Circular role hierarchy. role '%s' refers to itself through chain: %s->%s", roleId, String.join("->", parents), roleId));
        }
        Role role = roleById.get(roleId);
        if (role != null) {
            return role;
        }

        Role.Builder roleBuilder = roleBuilderById.get(roleId);

        if (roleBuilder == null) {
            return null;
        }

        List<String> childrenIds = roleChildrenByRoleId.get(roleId);
        if (childrenIds != null) {
            parents.add(roleId);
            try {
                for (String childRoleId : childrenIds) {
                    Role childRole = roleById.get(childRoleId);
                    if (childRole == null) {
                        childRole = recursiveBuildAndAddRole(parents, childRoleId, roleById, roleBuilderById, roleChildrenByRoleId);
                    }
                    if (childRole == null) {
                        throw new IllegalArgumentException(
                                String.format("Role mis-configuration. Property 'role.%s.children=%s' refers to roleId '%s' which must be configured with either 'role.%s.policies=policyId...' and/or 'role.%s.children=childRoleId...'",
                                        roleId, String.join(",", childrenIds), childRoleId, childRoleId, childRoleId));
                    }
                    roleBuilder.addChild(childRole);
                }
            } finally {
                parents.remove(roleId);
            }
        }

        role = roleBuilder.build();
        roleById.put(roleId, role);
        logRoleAdded(roleBuilder, false);

        return role;
    }

    private void logRoleAdded(Role.Builder roleBuilder, boolean system) {
        log.info("Added {}role '{}'. children: '{}', policies: '{}'", system ? "system " : "", roleBuilder.roleId,
                roleBuilder.children.stream().map(Role::getRoleId).collect(Collectors.joining(",")),
                roleBuilder.policies.stream().map(Policy::getPolicyId).collect(Collectors.joining(",")));
    }

    private void initSystemGroups(Map<String, Group> groupById) {
        Group entraOsAdminsGroup = Group.builder()
                .groupId("entraosadmins")
                .addRole(roleById.get("superuser"))
                .build();
        groupById.put(entraOsAdminsGroup.getGroupId(), entraOsAdminsGroup);
        logGroupAdded(entraOsAdminsGroup.getGroupId(), Collections.emptyList(), Collections.singletonList("superuser"), true);
        Group blacklistGroup = Group.builder()
                .groupId("blacklist")
                .addPolicy(policyById.get("noaccess"))
                .build();
        groupById.put(blacklistGroup.getGroupId(), blacklistGroup);
        logGroupAdded(blacklistGroup.getGroupId(), Collections.singletonList("noaccess"), Collections.emptyList(), true);
    }

    private Map<String, Group> initGroups(ApplicationProperties authorizationConfig) {
        Map<String, Group> groupById = new LinkedHashMap<>();
        initSystemGroups(groupById);

        ApplicationProperties groupConfig = authorizationConfig.subTree("group");
        Set<String> groupKeys = groupConfig.map().keySet();
        Set<String> groupIds = new LinkedHashSet<>();
        for (String groupKey : groupKeys) {
            int dotIndex = groupKey.indexOf('.');
            if (dotIndex > 0) {
                String roleId = groupKey.substring(0, dotIndex);
                groupIds.add(roleId);
            }
        }

        for (String groupId : groupIds) {
            ApplicationProperties groupProperties = groupConfig.subTree(groupId);
            Group.Builder groupBuilder = Group.builder()
                    .groupId(groupId);
            List<String> policyIds = new ArrayList<>();
            {
                String policiesStr = groupProperties.get("policies");
                if (policiesStr != null) {
                    String[] parts = policiesStr.split(",");
                    for (String policyId : parts) {
                        policyId = policyId.trim();
                        if (!policyId.isEmpty()) {
                            Policy policy = policyById.get(policyId);
                            if (policy == null) {
                                throw new IllegalArgumentException(
                                        String.format("Group mis-configuration. Property 'group.%s.policies=%s' refers to policyId '%s' which must be configured with either 'policy.%s.allow=actions...' and/or 'policy.%s.deny=actions...'",
                                                groupId, policiesStr, policyId, policyId, policyId));
                            }
                            groupBuilder.addPolicy(policy);
                            policyIds.add(policyId);
                        }
                    }
                }
            }
            List<String> roleIds = new ArrayList<>();
            {
                String rolesStr = groupProperties.get("roles");
                if (rolesStr != null) {
                    String[] parts = rolesStr.split(",");
                    for (String roleId : parts) {
                        roleId = roleId.trim();
                        if (!roleId.isEmpty()) {
                            Role role = roleById.get(roleId);
                            if (role == null) {
                                throw new IllegalArgumentException(
                                        String.format("Group mis-configuration. Property 'group.%s.roles=%s' refers to roleId '%s' which must be configured with either 'role.%s.policies=policyId...' and/or 'role.%s.children=childRoleId...'",
                                                groupId, rolesStr, roleId, roleId, roleId));
                            }
                            groupBuilder.addRole(role);
                            roleIds.add(roleId);
                        }
                    }
                }
            }
            Group group = groupBuilder.build();
            groupById.put(groupId, group);
            logGroupAdded(groupId, policyIds, roleIds, false);
        }

        return groupById;
    }

    private void logGroupAdded(String groupId, Iterable<String> policyIds, Iterable<String> roleIds, boolean system) {
        log.info("Added {}group '{}'. roles: '{}', policies: '{}'", system ? "system " : "", groupId,
                String.join(",", roleIds),
                String.join(",", policyIds));
    }

    Map<String, ConfiguredUserPermissions> initUserPermissions(ApplicationProperties authorizationConfig) {
        Map<String, ConfiguredUserPermissions> configuredUserPermissionsByUserId = new ConcurrentHashMap<>();

        ApplicationProperties userConfig = authorizationConfig.subTree("user");
        Set<String> userKeys = userConfig.map().keySet();
        Set<String> userIds = new LinkedHashSet<>();
        for (String userKey : userKeys) {
            int dotIndex = userKey.indexOf('.');
            if (dotIndex > 0) {
                String userId = userKey.substring(0, dotIndex);
                userIds.add(userId);
            }
        }
        for (String userId : userIds) {
            ApplicationProperties userProperties = userConfig.subTree(userId);
            List<Group> groups = new ArrayList<>();
            List<Role> roles = new ArrayList<>();
            List<Policy> policies = new ArrayList<>();

            {
                String groupsStr = userProperties.get("groups");
                if (groupsStr != null) {
                    String[] parts = groupsStr.split(",");
                    for (String groupId : parts) {
                        groupId = groupId.trim();
                        if (!groupId.isEmpty()) {
                            Group group = groupById.get(groupId);
                            if (group != null) {
                                groups.add(group);
                            } else {
                                log.warn("Configured property 'user.{}.groups' assigned non-existing group '{}'", userId, groupId);
                            }
                        }
                    }
                }
            }
            {
                String rolesStr = userProperties.get("roles");
                if (rolesStr != null) {
                    String[] parts = rolesStr.split(",");
                    for (String roleId : parts) {
                        roleId = roleId.trim();
                        if (!roleId.isEmpty()) {
                            Role role = roleById.get(roleId);
                            if (role != null) {
                                roles.add(role);
                            } else {
                                log.warn("Configured property 'user.{}.roles' assigned non-existing role '{}'", userId, roleId);
                            }
                        }
                    }
                }
            }
            {
                String policiesStr = userProperties.get("policies");
                if (policiesStr != null) {
                    String[] parts = policiesStr.split(",");
                    for (String policyId : parts) {
                        policyId = policyId.trim();
                        if (!policyId.isEmpty()) {
                            Policy policy = policyById.get(policyId);
                            if (policy != null) {
                                policies.add(policy);
                            } else {
                                log.warn("Configured property 'user.{}.policies' assigned non-existing policy '{}'", userId, policyId);
                            }
                        }
                    }
                }
            }

            configuredUserPermissionsByUserId.put(userId, new ConfiguredUserPermissions(userId, groups, roles, policies));
            log.info("Assigned user '{}' to groups: '{}', roles: '{}', policies: '{}'", userId,
                    groups.stream().map(Group::getGroupId).collect(Collectors.joining(",")),
                    roles.stream().map(Role::getRoleId).collect(Collectors.joining(",")),
                    policies.stream().map(Policy::getPolicyId).collect(Collectors.joining(",")));
        }

        return configuredUserPermissionsByUserId;
    }

    ConfiguredUserPermissions initDefaultUserPermissions(ApplicationProperties authorizationConfig) {
        String defaultUserGroupId = authorizationConfig.get("default.user-group");
        List<Group> defaultGroups = new LinkedList<>();
        if (defaultUserGroupId != null) {
            Group defaultUserGroup = groupById.get(defaultUserGroupId);
            if (defaultUserGroup != null) {
                defaultGroups.add(defaultUserGroup);
            }
        }
        ConfiguredUserPermissions defaultConfiguredUserPermissions = new ConfiguredUserPermissions("default-user", defaultGroups, Collections.emptyList(), Collections.emptyList());
        log.info("Assigned default users to groups: '{}'",
                defaultGroups.stream().map(Group::getGroupId).collect(Collectors.joining(","))
        );
        return defaultConfiguredUserPermissions;
    }

    Map<String, ConfiguredApplicationPermissions> initApplicationPermissions(ApplicationProperties authorizationConfig) {
        Map<String, ConfiguredApplicationPermissions> configuredApplicationPermissionsByApplicationId = new ConcurrentHashMap<>();

        ApplicationProperties applicationConfig = authorizationConfig.subTree("application");
        Set<String> applicationKeys = applicationConfig.map().keySet();
        Set<String> applicationIds = new LinkedHashSet<>();
        for (String applicationKey : applicationKeys) {
            int dotIndex = applicationKey.indexOf('.');
            if (dotIndex > 0) {
                String applicationId = applicationKey.substring(0, dotIndex);
                applicationIds.add(applicationId);
            }
        }
        for (String applicationId : applicationIds) {
            ApplicationProperties applicationProperties = applicationConfig.subTree(applicationId);
            List<Group> groups = new ArrayList<>();
            List<Role> roles = new ArrayList<>();
            List<Policy> policies = new ArrayList<>();
            {
                String groupsStr = applicationProperties.get("groups");
                if (groupsStr != null) {
                    String[] parts = groupsStr.split(",");
                    for (String groupId : parts) {
                        groupId = groupId.trim();
                        if (!groupId.isEmpty()) {
                            Group group = groupById.get(groupId);
                            if (group != null) {
                                groups.add(group);
                            } else {
                                log.warn("Configured property 'application.{}.groups' assigned non-existing group '{}'", applicationId, groupId);
                            }
                        }
                    }
                }
            }
            {
                String rolesStr = applicationProperties.get("roles");
                if (rolesStr != null) {
                    String[] parts = rolesStr.split(",");
                    for (String roleId : parts) {
                        roleId = roleId.trim();
                        if (!roleId.isEmpty()) {
                            Role role = roleById.get(roleId);
                            if (role != null) {
                                roles.add(role);
                            } else {
                                log.warn("Configured property 'application.{}.roles' assigned non-existing role '{}'", applicationId, roleId);
                            }
                        }
                    }
                }
            }
            {
                String policiesStr = applicationProperties.get("policies");
                if (policiesStr != null) {
                    String[] parts = policiesStr.split(",");
                    for (String policyId : parts) {
                        policyId = policyId.trim();
                        if (!policyId.isEmpty()) {
                            Policy policy = policyById.get(policyId);
                            if (policy != null) {
                                policies.add(policy);
                            } else {
                                log.warn("Configured property 'application.{}.policies' assigned non-existing policy '{}'", applicationId, policyId);
                            }
                        }
                    }
                }
            }

            configuredApplicationPermissionsByApplicationId.put(applicationId, new ConfiguredApplicationPermissions(applicationId, groups, roles, policies));
            log.info("Assigned application '{}' to groups: '{}', roles: '{}', policies: '{}'", applicationId,
                    groups.stream().map(Group::getGroupId).collect(Collectors.joining(",")),
                    roles.stream().map(Role::getRoleId).collect(Collectors.joining(",")),
                    policies.stream().map(Policy::getPolicyId).collect(Collectors.joining(",")));
        }

        return configuredApplicationPermissionsByApplicationId;
    }

    ConfiguredApplicationPermissions initDefaultApplicationPermissions(ApplicationProperties authorizationConfig) {
        String defaultApplicationGroupId = authorizationConfig.get("default.application-group");
        List<Group> defaultGroups = new LinkedList<>();
        if (defaultApplicationGroupId != null) {
            Group defaultApplicationGroup = groupById.get(defaultApplicationGroupId);
            if (defaultApplicationGroup != null) {
                defaultGroups.add(defaultApplicationGroup);
            }
        }
        ConfiguredApplicationPermissions defaultConfiguredApplicationPermissions = new ConfiguredApplicationPermissions("default-application", defaultGroups, Collections.emptyList(), Collections.emptyList());
        log.info("Assigned default applications to groups: '{}'",
                defaultGroups.stream().map(Group::getGroupId).collect(Collectors.joining(","))
        );
        return defaultConfiguredApplicationPermissions;
    }

    @Override
    public boolean hasAccess(Authentication authentication, String action) {
        Objects.requireNonNull(authentication);
        if (authentication instanceof UserAuthentication) {
            UserAuthentication userAuthentication = (UserAuthentication) authentication;
            return userHasAccess(userAuthentication.ssoId(), action);
        }
        if (authentication instanceof ApplicationAuthentication) {
            ApplicationAuthentication applicationAuthentication = (ApplicationAuthentication) authentication;
            return applicationHasAccess(applicationAuthentication.ssoId(), action);
        }
        return false;
    }

    @Override
    public boolean userHasAccess(String userId, String actionValue) {
        Objects.requireNonNull(userId);
        validateAction(actionValue);
        ConfiguredUserPermissions cup = configuredUserPermissionsByUserId.get(userId);
        if (cup == null) {
            // TODO get user-group assignment or user-tags from token or SSO api
            cup = defaultUserPermissions;
        }
        Action action = new Action(actionService, actionValue.trim());
        Policy policy = cup.getEffectivePolicy();
        PermissionDecision permissionDecision = policy.decidePermission(action);
        return permissionDecision.isAllowed();
    }

    @Override
    public boolean applicationHasAccess(String applicationId, String actionValue) {
        Objects.requireNonNull(applicationId);
        validateAction(actionValue);
        ConfiguredApplicationPermissions cap = configuredApplicationPermissionsByApplicationId.get(applicationId);
        if (cap == null) {
            // TODO get application-group assignment or user-tags from token or SSO api
            cap = defaultApplicationPermissions;
        }
        Action action = new Action(actionService, actionValue.trim());
        Policy policy = cap.getEffectivePolicy();
        PermissionDecision permissionDecision = policy.decidePermission(action);
        return permissionDecision.isAllowed();
    }

    private void validateAction(String action) {
        Objects.requireNonNull(action);
        action = action.trim();
        if (action.isEmpty()) {
            throw new IllegalArgumentException("Empty action not allowed.");
        }
        if ("*".equals(action)) {
            throw new IllegalArgumentException("Action '*' not allowed.");
        }
    }
}
