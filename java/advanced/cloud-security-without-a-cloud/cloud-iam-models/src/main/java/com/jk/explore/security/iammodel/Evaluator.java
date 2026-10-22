package com.jk.explore.security.iammodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The evaluation logic, as the cloud providers document it. An explicit deny in any policy wins. Then every layer that restricts (an organization policy, the permission boundary,
 * the session policy) must allow. Then within one account an allow in an identity policy or in the resource's policy is enough, and across accounts both are needed.
 * A resource policy that names the principal itself (not just its account) is not narrowed by the boundary or the session policy.
 */
public final class Evaluator {

    private Evaluator() {
    }

    /** What guards the resource: its policy (or null), the account it is in, and organization policies (or null). */
    public record Guard(Policy resourcePolicy, String resourceAccount, List<Policy> organization) {

        public static Guard none(String account) {
            return new Guard(null, account, null);
        }

        public static Guard of(Policy resourcePolicy, String account) {
            return new Guard(resourcePolicy, account, null);
        }
    }

    public static Decision decide(Request r, Guard guard) {
        Principal p = r.principal();
        Map<String, String> context = new LinkedHashMap<>(r.context());
        context.putIfAbsent("aws:username", p.name());
        context.putIfAbsent("aws:PrincipalArn", p.arn());
        p.tags().forEach((k, v) -> context.putIfAbsent("aws:PrincipalTag/" + k, v));
        Request req = new Request(p, r.action(), r.resource(), context);

        for (Policy pol : p.policies()) {
            if (matches(pol, req, false, true)) {
                return new Decision(false, "explicit deny in " + pol.name());
            }
        }
        if (p.boundary() != null && matches(p.boundary(), req, false, true)) {
            return new Decision(false, "explicit deny in the permission boundary");
        }
        if (p.sessionPolicy() != null && matches(p.sessionPolicy(), req, false, true)) {
            return new Decision(false, "explicit deny in the session policy");
        }
        if (guard.resourcePolicy() != null && matches(guard.resourcePolicy(), req, true, true)) {
            return new Decision(false, "explicit deny in " + guard.resourcePolicy().name());
        }
        if (guard.organization() != null) {
            for (Policy o : guard.organization()) {
                if (matches(o, req, false, true)) {
                    return new Decision(false, "explicit deny in the organization policy " + o.name());
                }
            }
            if (guard.organization().stream().noneMatch(o -> matches(o, req, false, false))) {
                return new Decision(false, "implicit deny: no allow in the organization policies");
            }
        }
        boolean resourceNamesPrincipal = guard.resourcePolicy() != null && namesPrincipal(guard.resourcePolicy(), req);
        boolean resourceAllow = guard.resourcePolicy() != null && matches(guard.resourcePolicy(), req, true, false);
        boolean sameAccount = guard.resourceAccount().equals(p.account());
        if (!resourceNamesPrincipal) {
            if (p.boundary() != null && !matches(p.boundary(), req, false, false)) {
                return new Decision(false, "implicit deny: outside the permission boundary");
            }
            if (p.sessionPolicy() != null && !matches(p.sessionPolicy(), req, false, false)) {
                return new Decision(false, "implicit deny: outside the session policy");
            }
        }
        if (p.account().isEmpty()) {
            return resourceAllow ? new Decision(true, "allowed to everyone by the resource policy") : new Decision(false, "implicit deny: an anonymous caller needs the resource policy");
        }
        boolean identityAllow = p.policies().stream().anyMatch(pol -> matches(pol, req, false, false));
        if (sameAccount) {
            if (resourceNamesPrincipal && resourceAllow) {
                return new Decision(true, "the resource policy names the principal");
            }
            if (identityAllow) {
                return new Decision(true, "allowed by an identity policy");
            }
            if (resourceAllow) {
                return new Decision(true, "allowed by the resource policy");
            }
            return new Decision(false, "implicit deny: nothing allows it");
        }
        if (identityAllow && resourceAllow) {
            return new Decision(true, "allowed by both accounts");
        }
        if (!resourceAllow) {
            return new Decision(false, "implicit deny: the resource's account does not allow it");
        }
        return new Decision(false, "implicit deny: the principal's own account does not allow it");
    }

    /** Whether some statement of this effect applies to the request: action, resource, principal (for resource policies) and conditions. */
    static boolean matches(Policy policy, Request r, boolean resourcePolicy, boolean deny) {
        for (Policy.Statement s : policy.statements()) {
            if (s.allow() == deny) {
                continue;
            }
            if (applies(s, r, resourcePolicy)) {
                return true;
            }
        }
        return false;
    }

    private static boolean applies(Policy.Statement s, Request r, boolean resourcePolicy) {
        boolean action = s.actions().stream().anyMatch(a -> Wild.matches(a, r.action(), true));
        if (s.notAction() == action) {
            return false;
        }
        boolean resource = s.resources().stream().anyMatch(x -> Wild.matches(Conditions.substitute(x, r.context()), r.resource(), false));
        if (s.notResource() == resource) {
            return false;
        }
        if (resourcePolicy && !principalMatches(s.principals(), r.principal())) {
            return false;
        }
        return Conditions.holds(s.conditions(), r.context());
    }

    private static boolean principalMatches(List<String> principals, Principal p) {
        for (String x : principals) {
            if (x.equals("*") || x.equals(p.arn()) || x.equals("arn:aws:iam::" + p.account() + ":root") || x.equals(p.account())) {
                return true;
            }
        }
        return false;
    }

    private static boolean namesPrincipal(Policy policy, Request r) {
        for (Policy.Statement s : policy.statements()) {
            if (s.allow() && s.principals().contains(r.principal().arn()) && applies(s, r, true)) {
                return true;
            }
        }
        return false;
    }

    /** The actions out of {@code actions} that the principal may do on the resource. */
    public static List<String> allowed(Principal p, List<String> actions, String resource, Guard guard) {
        List<String> out = new ArrayList<>();
        for (String a : actions) {
            if (decide(new Request(p, a, resource), guard).allowed()) {
                out.add(a);
            }
        }
        return out;
    }
}
