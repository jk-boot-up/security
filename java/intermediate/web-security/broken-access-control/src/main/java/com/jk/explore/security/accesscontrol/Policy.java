package com.jk.explore.security.accesscontrol;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Every access rule of the store in one table, checked at one place before any handler runs. A route with no rule is refused:
 * deny by default. Each refusal is logged with who asked and why, and never with the token.
 */
final class Policy {

    /** Who may use a route: anyone, anyone signed in, certain roles, or the owner of the order named in the path (plus certain roles). */
    record Rule(boolean signInNeeded, Set<Role> roles, boolean ownerAllowed) {

        static Rule anyone() {
            return new Rule(false, EnumSet.allOf(Role.class), false);
        }

        static Rule signedIn() {
            return new Rule(true, EnumSet.allOf(Role.class), false);
        }

        static Rule roles(Role first, Role... rest) {
            return new Rule(true, EnumSet.of(first, rest), false);
        }

        static Rule ownerOr(Role... staff) {
            Set<Role> roles = EnumSet.noneOf(Role.class);
            roles.addAll(List.of(staff));
            return new Rule(true, roles, true);
        }
    }

    /** The result of a check: allowed, or refused with a status and a reason. */
    record Decision(boolean allowed, int status, String reason) {

        static final Decision ALLOW = new Decision(true, 200, "allowed");
    }

    private final Map<Route, Rule> rules = new LinkedHashMap<>();
    private final List<String> denials = new ArrayList<>();

    Policy allow(String method, String pattern, Rule rule) {
        rules.put(new Route(method, pattern), rule);
        return this;
    }

    Optional<Rule> rule(Route route) {
        return Optional.ofNullable(rules.get(route));
    }

    Decision decide(Route route, Map<String, String> params, Optional<User> user, ShopData data) {
        Rule rule = rules.get(route);
        String who = user.map(User::name).orElse("someone signed out");
        if (rule == null) {
            return deny(who, route, 403, "no rule for this route, so it is refused");
        }
        if (!rule.signInNeeded()) {
            return Decision.ALLOW;
        }
        if (user.isEmpty()) {
            return deny(who, route, 401, "not signed in");
        }
        if (rule.roles().contains(user.get().role())) {
            return Decision.ALLOW;
        }
        if (rule.ownerAllowed()) {
            Optional<Order> order = data.order(params.get("id"));
            if (order.isPresent() && order.get().owner().equals(user.get().name())) {
                return Decision.ALLOW;
            }
            // someone else's order gets the same answer as a missing one, so the reply does not confirm that the id exists
            return deny(who, route, 404, "no order " + params.get("id") + " that belongs to " + who);
        }
        return deny(who, route, 403, "needs " + rule.roles() + ", is " + user.get().role());
    }

    private Decision deny(String who, Route route, int status, String reason) {
        denials.add("DENY " + who + " " + route + ": " + reason);
        return new Decision(false, status, reason);
    }

    List<String> denials() {
        return List.copyOf(denials);
    }
}
