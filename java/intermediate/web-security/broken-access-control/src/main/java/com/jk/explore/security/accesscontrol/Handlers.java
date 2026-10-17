package com.jk.explore.security.accesscontrol;

import java.util.stream.Collectors;

/**
 * The store's business logic, with no access checks at all. The naive store wraps each handler in its own check; the guarded store
 * puts one policy in front of all of them. The handlers are the same in both, so the only difference is where the checks live.
 */
final class Handlers {

    private Handlers() {
    }

    static Reply menu(Call call) {
        String links = call.user().map(u -> u.role() == Role.ADMIN ? "orders, account, admin" : "orders, account").orElse("sign in");
        return Reply.ok("links shown: " + links);
    }

    static Reply account(Call call) {
        User user = call.user().orElseThrow();
        return Reply.ok("signed in as " + user.name());
    }

    static Reply orderDetails(Call call) {
        return call.data().order(call.param("id"))
                .map(o -> Reply.ok("order " + o.id() + ": " + o.item() + " for " + o.owner() + ", to " + o.address()))
                .orElse(Reply.status(404, "no such order"));
    }

    static Reply changeAddress(Call call) {
        return call.data().order(call.param("id")).map(o -> {
            o.moveTo(call.body());
            return Reply.ok("order " + o.id() + " now goes to " + o.address());
        }).orElse(Reply.status(404, "no such order"));
    }

    static Reply cancelOrder(Call call) {
        return call.data().cancel(call.param("id")) ? Reply.ok("cancelled") : Reply.status(404, "no such order");
    }

    static Reply listUsers(Call call) {
        return Reply.ok(call.data().users().stream().map(User::name).collect(Collectors.joining(", ")));
    }

    static Reply deleteUser(Call call) {
        return call.data().removeUser(call.param("name")) ? Reply.ok("deleted") : Reply.status(404, "no such user");
    }

    static Reply refund(Call call) {
        if (call.data().order(call.body()).isEmpty()) {
            return Reply.status(404, "no such order");
        }
        call.data().refund(call.body());
        return Reply.ok("refunded " + call.body());
    }

    static Reply salesReport(Call call) {
        return Reply.ok("sales report: every order, every customer, every address");
    }

    static Reply exportCustomers(Call call) {
        return Reply.ok("every customer's name and address");
    }
}
