package com.jk.explore.security.accesscontrol;

import java.util.Optional;

/**
 * The first version of the store: every handler checks what its author remembered to check. Some checks are right, some only ask
 * whether someone is signed in, and some are missing, which is how access control goes wrong in real code.
 */
final class NaiveStore implements Store {

    private final ShopData data;
    private final RouteTable table = new RouteTable();

    NaiveStore(ShopData data) {
        this.data = data;
        table.add("GET", "/menu", Handlers::menu)
                .add("GET", "/account", signedIn(Handlers::account))
                // signed in is enough to read any order: the id is the only thing between one customer and another
                .add("GET", "/orders/{id}", signedIn(Handlers::orderDetails))
                // and enough to change any order's delivery address
                .add("POST", "/orders/{id}/address", signedIn(Handlers::changeAddress))
                // this one was written carefully: the owner or an admin
                .add("DELETE", "/orders/{id}", call -> ownerOrAdmin(call) ? Handlers.cancelOrder(call) : forbidden())
                // listing users checks the role...
                .add("GET", "/admin/users", call -> isAdmin(call) ? Handlers.listUsers(call) : forbidden())
                // ...deleting one does not: the DELETE was added later and copied without its check
                .add("DELETE", "/admin/users/{name}", Handlers::deleteUser)
                // the refund button is only shown to admins in the page, so nobody added a check here
                .add("POST", "/admin/refunds", signedIn(Handlers::refund))
                // nobody links to the report, so nobody thought to protect it
                .add("GET", "/admin/reports", Handlers::salesReport);
    }

    @Override
    public Reply handle(Request request) {
        return table.find(request)
                .map(m -> m.handler().handle(new Call(data.userFor(request.token()), m.params(), request.body(), data)))
                .orElse(Reply.status(404, "no such page"));
    }

    private static Handler signedIn(Handler handler) {
        return call -> call.user().isPresent() ? handler.handle(call) : Reply.status(401, "sign in first");
    }

    private static boolean isAdmin(Call call) {
        return call.user().map(u -> u.role() == Role.ADMIN).orElse(false);
    }

    private static boolean ownerOrAdmin(Call call) {
        Optional<User> user = call.user();
        if (user.isEmpty()) {
            return false;
        }
        return user.get().role() == Role.ADMIN
                || call.data().order(call.param("id")).map(o -> o.owner().equals(user.get().name())).orElse(false);
    }

    private static Reply forbidden() {
        return Reply.status(403, "not allowed");
    }

    int routeCount() {
        return table.routes().size();
    }
}
