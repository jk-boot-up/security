package com.jk.explore.security.accesscontrol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs the online store story in five numbered acts and prints exact, repeatable results. Each act prints the requests sent,
 * then what came back, in separate labelled sections.
 */
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

    /** One request as a table row: who, method, path, status, and an optional marker. */
    private static void row(StringBuilder out, String who, String method, String path, int status, String mark) {
        line(out, String.format("    %-7s%-7s%-19s%d%s", who, method, path, status, mark.isEmpty() ? "" : "  <- " + mark));
    }

    private static void attempt(StringBuilder out, Attempts.Attempt attempt, Reply reply) {
        line(out, String.format("    %-38s%d", attempt.label(), reply.status()));
    }

    private static void act1WalkingOrderNumbers(StringBuilder out) {
        line(out, "ONE. Walking order numbers.");
        ShopData data = ShopData.sample();
        NaiveStore store = new NaiveStore(data);
        line(out, "  THE WALK (ana is signed in, and changes the number):");
        List<String> others = new ArrayList<>();
        int found = 0;
        for (int n = 1000; n <= 1010; n++) {
            Reply reply = store.handle(Request.of("GET", "/orders/" + n, Attempts.ANA));
            String owner = "";
            if (reply.succeeded()) {
                found++;
                owner = data.order(String.valueOf(n)).orElseThrow().owner();
                if (!owner.equals("ana")) {
                    others.add(n + " (" + owner + ")");
                }
            }
            String mark = owner.isEmpty() ? "" : owner.equals("ana") ? "  hers" : "  " + owner + "'s  <- NOT HERS";
            line(out, "    GET /orders/" + n + "  " + reply.status() + mark);
        }
        line(out, "  THE RESULT:");
        line(out, "    " + found + " orders answer, " + others.size() + " of them other customers'");
        line(out, "    " + String.join(", ", others) + ": each one number away");
        line(out, "  THE GIFT ORDER (an id nobody could guess; ben shared the link):");
        line(out, "    GET /orders/" + ShopData.GIFT_ORDER + "  "
                + store.handle(Request.of("GET", "/orders/" + ShopData.GIFT_ORDER, Attempts.ANA)).status());
        line(out, "    <- A HARD-TO-GUESS ID IS NOT A PERMISSION");
        line(out, "  signing in proved who ana is; nothing asked if the order is hers");
    }

    private static void act2RoutesNobodyChecks(StringBuilder out) {
        line(out, "TWO. Routes that nobody checks.");
        ShopData data = ShopData.sample();
        NaiveStore store = new NaiveStore(data);
        line(out, "  THE PAGE ANA SEES:");
        line(out, "    " + store.handle(Request.of("GET", "/menu", Attempts.ANA)).body());
        line(out, "    the admin links are hidden");
        line(out, "  THE REQUESTS SENT BY HAND (who, method, path, status):");
        row(out, "ana", "POST", "/admin/refunds", store.handle(Request.of("POST", "/admin/refunds", Attempts.ANA, "1002")).status(),
                "CHECKED ONLY IN THE PAGE");
        row(out, "nobody", "GET", "/admin/reports", store.handle(Request.of("GET", "/admin/reports", null)).status(), "NOT LINKED, STILL A ROUTE");
        row(out, "ana", "GET", "/admin/users", store.handle(Request.of("GET", "/admin/users", Attempts.ANA)).status(), "");
        row(out, "ana", "DELETE", "/admin/users/ben", store.handle(Request.of("DELETE", "/admin/users/ben", Attempts.ANA)).status(),
                "THIS METHOD FORGOTTEN");
        line(out, "  THE RESULT:");
        line(out, "    ben's account still exists: " + data.userFor(Attempts.BEN).isPresent());
    }

    private static void act3HorizontalAndVertical(StringBuilder out) {
        line(out, "THREE. Sideways and upwards.");
        ShopData data = ShopData.sample();
        NaiveStore store = new NaiveStore(data);
        line(out, "  SIDEWAYS (the same role, someone else's data):");
        store.handle(Request.of("POST", "/orders/1002/address", Attempts.ANA, "12 Palm Street"));
        line(out, "    ana  POST /orders/1002/address  \"12 Palm Street\"");
        line(out, "    ben's order now: " + store.handle(Request.of("GET", "/orders/1002", Attempts.BEN)).body());
        line(out, "  UPWARDS (a higher role's function):");
        store.handle(Request.of("POST", "/admin/refunds", Attempts.ANA, "1002"));
        line(out, "    ana  POST /admin/refunds  \"1002\"");
        line(out, "    the refund of 1002 is recorded: " + data.refunds().contains("1002"));
        NaiveStore naive = new NaiveStore(ShopData.sample());
        int succeeded = 0;
        Set<String> routes = new LinkedHashSet<>();
        line(out, "  BEFORE THE FIX (the naive store, nine attacks):");
        for (Attempts.Attempt attempt : Attempts.attacks()) {
            Reply reply = naive.handle(attempt.request());
            attempt(out, attempt, reply);
            if (reply.succeeded()) {
                succeeded++;
                routes.add(routeOf(attempt.request()));
            }
        }
        line(out, "    " + succeeded + " succeed, through " + routes.size() + " of its " + naive.routeCount() + " routes");
        line(out, "  the 403 is DELETE /orders/{id}, the one route written carefully");
    }

    private static String describe(Policy.Rule rule) {
        if (!rule.signInNeeded()) {
            return "anyone";
        }
        if (!rule.ownerAllowed() && rule.roles().size() == Role.values().length) {
            return "anyone signed in";
        }
        List<String> who = new ArrayList<>();
        if (rule.ownerAllowed()) {
            who.add("the owner");
        }
        rule.roles().forEach(r -> who.add(r.name()));
        return String.join(", ", who);
    }

    private static String routeOf(Request request) {
        return request.method() + " " + request.path().replaceAll("/orders/[^/]+", "/orders/{id}").replaceAll("/admin/users/[^/]+", "/admin/users/{name}");
    }

    private static void act4OneChokePoint(StringBuilder out) {
        line(out, "FOUR. One choke point, deny by default.");
        GuardedStore store = new GuardedStore(ShopData.sample());
        int succeeded = 0;
        line(out, "  AFTER THE FIX (one policy before every handler):");
        for (Attempts.Attempt attempt : Attempts.attacks()) {
            Reply reply = store.handle(attempt.request());
            if (reply.succeeded()) {
                succeeded++;
            }
            attempt(out, attempt, reply);
        }
        line(out, "    attacks that succeed: " + succeeded + " of " + Attempts.attacks().size());
        GuardedStore honest = new GuardedStore(ShopData.sample());
        line(out, "  THE HONEST REQUESTS (they must still work):");
        long ok = 0;
        for (Attempts.Attempt attempt : Attempts.honest()) {
            Reply reply = honest.handle(attempt.request());
            ok += reply.succeeded() ? 1 : 0;
            attempt(out, attempt, reply);
        }
        line(out, "    still work: " + ok + " of " + Attempts.honest().size());
        line(out, "  WHY 404 AND NOT 403:");
        line(out, "    someone else's order answers like a missing one,");
        line(out, "    so the reply does not confirm that the order exists");
        List<String> log = store.denials();
        line(out, "  THE LOG (" + log.size() + " refusals, two of them):");
        line(out, "    " + log.get(0));
        line(out, "    " + log.get(log.size() - 1));
        boolean leaks = log.stream().anyMatch(l -> l.contains("token-"));
        line(out, "    a token appears in the log: " + leaks);
    }

    private static void act5KeepingItRight(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. Keeping it right, and the bill.");
        GuardedStore store = new GuardedStore(ShopData.sample());
        line(out, "  THE RULE TABLE (one rule per route):");
        store.rules().forEach((route, rule) -> line(out, String.format("    %-7s%-22s%s", route.method(), route.pattern(), describe(rule))));
        line(out, "  THE ROUTE INVENTORY (a test that reads every route):");
        line(out, "    routes without a rule today: " + store.routesWithoutRules().size());
        store.addRouteWithoutRule("GET", "/admin/export", Handlers::exportCustomers);
        line(out, "    a developer adds GET /admin/export, and forgets its rule");
        line(out, "    routes without a rule: " + store.routesWithoutRules() + "  <- BUILD FAILS");
        line(out, "    dave the admin asks for it: " + store.handle(Request.of("GET", "/admin/export", Attempts.DAVE)).status()
                + ", denied by default");
        line(out, "  OVER REAL HTTP ON THIS MACHINE (who, method, path, status):");
        try (HttpFront front = new HttpFront(new GuardedStore(ShopData.sample()))) {
            row(out, "ana", "GET", "/orders/1001", front.send(Request.of("GET", "/orders/1001", Attempts.ANA)), "");
            row(out, "ana", "GET", "/orders/1002", front.send(Request.of("GET", "/orders/1002", Attempts.ANA)), "");
            row(out, "ana", "DELETE", "/admin/users/ben", front.send(Request.of("DELETE", "/admin/users/ben", Attempts.ANA)), "");
            row(out, "nobody", "GET", "/admin/reports", front.send(Request.of("GET", "/admin/reports", null)), "");
        }
        line(out, "  THE BILL:");
        line(out, "    naive: checks in " + new NaiveStore(ShopData.sample()).routeCount() + " handlers, nine places to forget one");
        line(out, "    guarded: one table of rules that a test can read");
        line(out, "    every object check needs a lookup of who owns the object");
        line(out, "    every refusal is a log line to keep and watch");
    }
}
