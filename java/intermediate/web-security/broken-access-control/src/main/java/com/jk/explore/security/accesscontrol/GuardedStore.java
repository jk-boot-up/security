package com.jk.explore.security.accesscontrol;

import java.util.List;

/**
 * The fixed store: the same handlers as the naive store, but with every check taken out of them and put in one policy that runs
 * before any handler. A route is served only if the policy has a rule for it.
 */
final class GuardedStore implements Store {

    private final ShopData data;
    private final RouteTable table = new RouteTable();
    private final Policy policy = new Policy();

    GuardedStore(ShopData data) {
        this.data = data;
        table.add("GET", "/menu", Handlers::menu)
                .add("GET", "/account", Handlers::account)
                .add("GET", "/orders/{id}", Handlers::orderDetails)
                .add("POST", "/orders/{id}/address", Handlers::changeAddress)
                .add("DELETE", "/orders/{id}", Handlers::cancelOrder)
                .add("GET", "/admin/users", Handlers::listUsers)
                .add("DELETE", "/admin/users/{name}", Handlers::deleteUser)
                .add("POST", "/admin/refunds", Handlers::refund)
                .add("GET", "/admin/reports", Handlers::salesReport);
        policy.allow("GET", "/menu", Policy.Rule.anyone())
                .allow("GET", "/account", Policy.Rule.signedIn())
                .allow("GET", "/orders/{id}", Policy.Rule.ownerOr(Role.SUPPORT, Role.ADMIN))
                .allow("POST", "/orders/{id}/address", Policy.Rule.ownerOr())
                .allow("DELETE", "/orders/{id}", Policy.Rule.ownerOr(Role.ADMIN))
                .allow("GET", "/admin/users", Policy.Rule.roles(Role.ADMIN))
                .allow("DELETE", "/admin/users/{name}", Policy.Rule.roles(Role.ADMIN))
                .allow("POST", "/admin/refunds", Policy.Rule.roles(Role.ADMIN))
                .allow("GET", "/admin/reports", Policy.Rule.roles(Role.ADMIN));
    }

    @Override
    public synchronized Reply handle(Request request) {
        return table.find(request).map(m -> {
            Call call = new Call(data.userFor(request.token()), m.params(), request.body(), data);
            Policy.Decision decision = policy.decide(m.route(), m.params(), call.user(), data);
            return decision.allowed() ? m.handler().handle(call) : Reply.status(decision.status(), decision.reason());
        }).orElse(Reply.status(404, "no such page"));
    }

    /** A developer adds a route and forgets the rule, as happens in every code base sooner or later. */
    GuardedStore addRouteWithoutRule(String method, String pattern, Handler handler) {
        table.add(method, pattern, handler);
        return this;
    }

    /** Routes with no rule: a test fails while this list is not empty. */
    List<Route> routesWithoutRules() {
        return table.routes().stream().filter(r -> policy.rule(r).isEmpty()).toList();
    }

    List<String> denials() {
        return policy.denials();
    }
}
