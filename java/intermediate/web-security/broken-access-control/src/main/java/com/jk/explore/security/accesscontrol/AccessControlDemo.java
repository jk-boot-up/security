package com.jk.explore.security.accesscontrol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class AccessControlDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1WalkingOrderNumbers(out);
        act2RoutesNobodyChecks(out);
        act3HorizontalAndVertical(out);
        act4OneChokePoint(out);
        act5KeepingItRight(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1WalkingOrderNumbers(StringBuilder out) {
        line(out, "ONE. Walking order numbers.");
        ShopData data = ShopData.sample();
        NaiveStore store = new NaiveStore(data);
        line(out, "  ana reads her own order 1001: " + store.handle(Request.of("GET", "/orders/1001", Attempts.ANA)).status());
        List<String> others = new ArrayList<>();
        int found = 0;
        for (int n = 1000; n <= 1010; n++) {
            Reply reply = store.handle(Request.of("GET", "/orders/" + n, Attempts.ANA));
            if (reply.succeeded()) {
                found++;
                String owner = data.order(String.valueOf(n)).orElseThrow().owner();
                if (!owner.equals("ana")) {
                    others.add(n + " (" + owner + ")");
                }
            }
        }
        line(out, "  ana tries the numbers 1000 to 1010: " + found + " orders answer, " + others.size()
                + " of them other customers': " + String.join(", ", others) + "  <- EVERY ORDER IS ONE NUMBER AWAY");
        line(out, "  ben's gift order has an id nobody could guess, but ben shared the link; ana opens it: "
                + store.handle(Request.of("GET", "/orders/" + ShopData.GIFT_ORDER, Attempts.ANA)).status()
                + "  <- A HARD-TO-GUESS ID IS NOT A PERMISSION");
        line(out, "  signing in proved who ana is; nothing asked whether the order is hers");
    }

    private static void act2RoutesNobodyChecks(StringBuilder out) {
        line(out, "TWO. Routes that nobody checks.");
        ShopData data = ShopData.sample();
        NaiveStore store = new NaiveStore(data);
        line(out, "  the page ana sees: " + store.handle(Request.of("GET", "/menu", Attempts.ANA)).body() + " (the admin links are hidden)");
        line(out, "  ana sends the refund request by hand: " + store.handle(Request.of("POST", "/admin/refunds", Attempts.ANA, "1002")).status()
                + "  <- THE ROLE WAS CHECKED ONLY IN THE PAGE");
        line(out, "  someone signed out asks for /admin/reports: " + store.handle(Request.of("GET", "/admin/reports", null)).status()
                + "  <- NOBODY LINKS TO IT, BUT IT IS STILL A ROUTE");
        line(out, "  ana: GET /admin/users " + store.handle(Request.of("GET", "/admin/users", Attempts.ANA)).status()
                + ", DELETE /admin/users/ben " + store.handle(Request.of("DELETE", "/admin/users/ben", Attempts.ANA)).status()
                + "  <- ONE METHOD CHECKED, THE OTHER FORGOTTEN");
        line(out, "  ben's account still exists: " + data.userFor(Attempts.BEN).isPresent());
    }

    private static void act3HorizontalAndVertical(StringBuilder out) {
        line(out, "THREE. Sideways and upwards.");
        ShopData data = ShopData.sample();
        NaiveStore store = new NaiveStore(data);
        store.handle(Request.of("POST", "/orders/1002/address", Attempts.ANA, "12 Palm Street"));
        line(out, "  sideways, the same role and someone else's data: ana redirects ben's order: "
                + store.handle(Request.of("GET", "/orders/1002", Attempts.BEN)).body());
        store.handle(Request.of("POST", "/admin/refunds", Attempts.ANA, "1002"));
        line(out, "  upwards, a higher role's function: ana's refund of 1002 is recorded: " + data.refunds().contains("1002"));
        ShopData fresh = ShopData.sample();
        NaiveStore naive = new NaiveStore(fresh);
        int succeeded = 0;
        Set<String> routes = new LinkedHashSet<>();
        for (Attempts.Attempt attempt : Attempts.attacks()) {
            if (naive.handle(attempt.request()).succeeded()) {
                succeeded++;
                routes.add(routeOf(attempt.request()));
            }
        }
        line(out, "  the naive store, nine attacks: " + succeeded + " succeed, through " + routes.size() + " of its "
                + naive.routeCount() + " routes");
        line(out, "  the one route written carefully, DELETE /orders/{id}, refused ana: "
                + new NaiveStore(ShopData.sample()).handle(Request.of("DELETE", "/orders/1002", Attempts.ANA)).status());
    }

    private static String routeOf(Request request) {
        return request.method() + " " + request.path().replaceAll("/orders/[^/]+", "/orders/{id}").replaceAll("/admin/users/[^/]+", "/admin/users/{name}");
    }

    private static void act4OneChokePoint(StringBuilder out) {
        line(out, "FOUR. One choke point, deny by default.");
        GuardedStore store = new GuardedStore(ShopData.sample());
        int succeeded = 0;
        for (Attempts.Attempt attempt : Attempts.attacks()) {
            Reply reply = store.handle(attempt.request());
            if (reply.succeeded()) {
                succeeded++;
            }
            line(out, "  " + attempt.label() + ": " + reply.status());
        }
        line(out, "  attacks that succeed: " + succeeded + " of " + Attempts.attacks().size());
        GuardedStore honest = new GuardedStore(ShopData.sample());
        long ok = Attempts.honest().stream().filter(a -> honest.handle(a.request()).succeeded()).count();
        line(out, "  honest requests that still work: " + ok + " of " + Attempts.honest().size());
        line(out, "  someone else's order answers 404, like a missing one, so the reply does not confirm the order exists");
        List<String> log = store.denials();
        line(out, "  refusals logged: " + log.size() + ", for example:");
        line(out, "    " + log.get(0));
        line(out, "    " + log.get(log.size() - 1));
        boolean leaks = log.stream().anyMatch(l -> l.contains("token-"));
        line(out, "  a token appears in the log: " + leaks);
    }

    private static void act5KeepingItRight(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. Keeping it right, and the bill.");
        GuardedStore store = new GuardedStore(ShopData.sample());
        line(out, "  routes without a rule today: " + store.routesWithoutRules().size());
        store.addRouteWithoutRule("GET", "/admin/export", Handlers::exportCustomers);
        line(out, "  a developer adds GET /admin/export and forgets its rule; routes without a rule: " + store.routesWithoutRules()
                + "  <- THE ROUTE INVENTORY TEST FAILS THE BUILD");
        line(out, "  until it has one, even dave the admin is refused: " + store.handle(Request.of("GET", "/admin/export", Attempts.DAVE)).status()
                + ", denied by default");
        try (HttpFront front = new HttpFront(new GuardedStore(ShopData.sample()))) {
            line(out, "  the same rules over real HTTP on this machine: ana's order " + front.send(Request.of("GET", "/orders/1001", Attempts.ANA))
                    + ", ben's order " + front.send(Request.of("GET", "/orders/1002", Attempts.ANA))
                    + ", DELETE /admin/users/ben " + front.send(Request.of("DELETE", "/admin/users/ben", Attempts.ANA))
                    + ", the report signed out " + front.send(Request.of("GET", "/admin/reports", null)));
        }
        line(out, "  the bill: the naive store keeps its checks in " + new NaiveStore(ShopData.sample()).routeCount()
                + " handlers, nine places to forget one; the guarded store keeps them in one table that a test can read");
        line(out, "  every object-level check needs a lookup of who owns the object, and every refusal is a log line to keep and watch");
    }
}
