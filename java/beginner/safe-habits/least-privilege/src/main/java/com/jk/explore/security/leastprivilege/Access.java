package com.jk.explore.security.leastprivilege;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Who has which roles, plus grants that expire on a simulated clock (in ticks). */
final class Access {

    private record Grant(String permission, long from, long until) {
    }

    private final Map<String, Set<String>> roles = new HashMap<>();
    private final Map<String, List<Grant>> grants = new HashMap<>();
    private final Map<String, Policy> extraPolicies = new HashMap<>();

    Access assign(String user, String... roleNames) {
        roles.put(user, Set.of(roleNames));
        return this;
    }

    /** A user-specific policy, such as an explicit deny, added on top of the roles. */
    Access restrict(String user, Policy policy) {
        extraPolicies.put(user, policy);
        return this;
    }

    /** Allows one permission from tick `from` up to but not including tick `until`. */
    void grant(String user, String permission, long from, long until) {
        grants.computeIfAbsent(user, u -> new ArrayList<>()).add(new Grant(permission, from, until));
    }

    Policy policyOf(String user) {
        List<Policy> policies = new ArrayList<>();
        for (String role : roles.getOrDefault(user, Set.of())) {
            policies.add(Catalogue.ROLES.get(role));
        }
        if (extraPolicies.containsKey(user)) {
            policies.add(extraPolicies.get(user));
        }
        return Policy.combine(policies);
    }

    boolean can(String user, String permission, long tick) {
        Policy policy = policyOf(user);
        Policy.Decision decision = policy.decide(permission);
        if (decision == Policy.Decision.DENY_EXPLICIT) {
            return false;
        }
        if (decision == Policy.Decision.ALLOW) {
            return true;
        }
        for (Grant grant : grants.getOrDefault(user, List.of())) {
            if (Policy.matches(grant.permission(), permission) && tick >= grant.from() && tick < grant.until()) {
                return true;
            }
        }
        return false;
    }
}
