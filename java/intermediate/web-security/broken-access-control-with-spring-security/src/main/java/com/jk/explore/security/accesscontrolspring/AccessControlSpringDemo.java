package com.jk.explore.security.accesscontrolspring;

import java.io.IOException;
import java.util.List;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/** Runs the online store story in five numbered acts against real Spring Boot servers on the loopback interface. */
public class AccessControlSpringDemo {

    /** One request: what it is, and who sends it (null for someone signed out). */
    record Attempt(String label, String method, String path, String user) {
    }

    static final String TO_ANA = "/address?to=12%20Palm%20Street";

    /** The nine attacks of the plain project, as real requests. */
    static final List<Attempt> ATTACKS = List.of(
            new Attempt("ana reads ben's order 1002", "GET", "/orders/1002", "ana"),
            new Attempt("ana opens ben's leaked gift link", "GET", "/orders/" + Catalogue.GIFT_ORDER, "ana"),
            new Attempt("ana sends ben's order to her address", "POST", "/orders/1002" + TO_ANA, "ana"),
            new Attempt("ana cancels ben's order 1002", "DELETE", "/orders/1002", "ana"),
            new Attempt("signed out, reads the sales report", "GET", "/admin/reports", null),
            new Attempt("ana reads the sales report", "GET", "/admin/reports", "ana"),
            new Attempt("ana refunds order 1002", "POST", "/admin/refunds?order=1002", "ana"),
            new Attempt("ana lists the users", "GET", "/admin/users", "ana"),
            new Attempt("ana deletes ben's account", "DELETE", "/admin/users/ben", "ana"));

    /** Six honest requests that must keep working. */
    static final List<Attempt> HONEST = List.of(
            new Attempt("signed out, reads the menu", "GET", "/menu", null),
            new Attempt("ana reads her account page", "GET", "/account", "ana"),
            new Attempt("ana reads her order 1001", "GET", "/orders/1001", "ana"),
            new Attempt("ana moves her order 1001", "POST", "/orders/1001" + TO_ANA, "ana"),
            new Attempt("dave (admin) lists the users", "GET", "/admin/users", "dave"),
            new Attempt("sam (support) refunds order 1002", "POST", "/admin/refunds?order=1002", "sam"));

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** A store started on a free loopback port with the given profiles. Closing it stops the server. */
    record Running(ConfigurableApplicationContext context, int port) implements AutoCloseable {

        <T> T bean(Class<T> type) {
            return context.getBean(type);
        }

        int status(String method, String path, String user) throws IOException, InterruptedException {
            return Wire.call(port, method, path, user).status();
        }

        int status(Attempt attempt) throws IOException, InterruptedException {
            return status(attempt.method(), attempt.path(), attempt.user());
        }

        @Override
        public void close() {
            context.close();
        }
    }

    static Running start(String... profiles) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Shop.class)
                .profiles(profiles)
                .properties("server.port=0", "server.address=127.0.0.1", "logging.level.root=OFF", "spring.main.banner-mode=off")
                .run();
        return new Running(context, Integer.parseInt(context.getEnvironment().getProperty("local.server.port")));
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Only statuses and counts are printed, never ports. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Running naive = start("naive")) {
            act1Naive(out, naive);
        }
        try (Running guarded = start("guarded")) {
            act2DenyByDefault(out, guarded);
            act3ObjectLevel(out, guarded);
            act4MethodSecurityAndLog(out, guarded);
        }
        try (Running later = start("guarded", "with-export")) {
            act5InventoryAndBill(out, later);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** One request as a table row: who, method, path, status, and an optional marker. */
    private static void row(StringBuilder out, Running shop, String method, String path, String user, String mark)
            throws IOException, InterruptedException {
        row(out, user, method, path, shop.status(method, path, user), mark);
    }

    private static void row(StringBuilder out, String user, String method, String path, int status, String mark) {
        line(out, String.format("    %-7s%-7s%-19s%d%s", user == null ? "nobody" : user, method, path.replaceAll("\\?.*", ""), status,
                mark.isEmpty() ? "" : "  <- " + mark));
    }

    private static void act1Naive(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "ONE. A Spring store with three common mistakes.");
        line(out, "  THE RULES AS WRITTEN (the filter chain, in order):");
        line(out, "    /account, /orders/**   authenticated()");
        line(out, "    /admin/users           hasRole(\"ADMIN\")");
        line(out, "    anyRequest()           permitAll()");
        line(out, "  THE REQUESTS (who, method, path, status):");
        row(out, shop, "GET", "/orders/1001", "ana", "");
        row(out, shop, "GET", "/orders/1002", "ana", "NOT HER ORDER");
        row(out, shop, "GET", "/admin/reports", null, "permitAll()");
        row(out, shop, "GET", "/admin/users", "ana", "");
        int deleted = shop.status("DELETE", "/admin/users/ben", "ana");
        row(out, "ana", "DELETE", "/admin/users/ben", deleted, "NOT /admin/users");
        line(out, "  THE RESULT:");
        line(out, "    ben's account still exists: " + shop.bean(InMemoryUserDetailsManager.class).userExists("ben"));
        int succeeded = 0;
        for (Attempt attack : ATTACKS) {
            if (attack.path().equals("/admin/users/ben")) {
                continue;
            }
            if (shop.status(attack) == 200) {
                succeeded++;
            }
        }
        // the account deletion was sent once, above
        if (deleted == 200) {
            succeeded++;
        }
        line(out, "    the nine attacks of the plain project: " + succeeded + " of " + ATTACKS.size() + " succeed");
        line(out, "    DELETE /orders/{id}, written carefully, refused ana: " + shop.status("DELETE", "/orders/1002", "ana"));
        line(out, "    the page ana sees: " + Wire.call(shop.port(), "GET", "/account", "ana").body().replace(" (the admin links are hidden)", ""));
        line(out, "    (the admin links are hidden)");
    }

    private static void act2DenyByDefault(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "TWO. Deny by default in the filter chain.");
        line(out, "  THE RULES AS WRITTEN (by method and path):");
        line(out, "    every route has its own rule");
        line(out, "    anyRequest().denyAll() is the last rule");
        line(out, "  THE REQUESTS (who, method, path, status):");
        row(out, shop, "GET", "/admin/reports", null, "WHO ARE YOU?");
        row(out, shop, "GET", "/admin/reports", "ana", "");
        row(out, shop, "POST", "/admin/refunds?order=1002", "ana", "");
        row(out, shop, "GET", "/admin/users", "ana", "");
        row(out, shop, "DELETE", "/admin/users/ben", "ana", "");
        row(out, shop, "PUT", "/orders/1001", "ana", "NO RULE, NO ACCESS");
        row(out, shop, "GET", "/admin/debug", "dave", "NO RULE, NO ACCESS");
        line(out, "  WHAT A URL RULE CANNOT DO:");
        line(out, "    GET /orders/{id} can only say: signed in");
        line(out, "    it cannot know whose order 1002 is");
    }

    private static void act3ObjectLevel(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "THREE. The object-level check, in the query.");
        line(out, "  THE ATTACKS ON ORDERS (after the fix):");
        for (Attempt attack : ATTACKS.subList(0, 4)) {
            line(out, String.format("    %-38s%d", attack.label(), shop.status(attack)));
        }
        line(out, "  WHY 404 (the lookup is findByIdAndOwner(id, ana)):");
        line(out, "    ben's order is not found, like a missing order");
        line(out, "    sam from support reads order 1002: " + shop.status("GET", "/orders/1002", "sam") + ", staff read any");
        OrderAccess access = shop.bean(OrderAccess.class);
        int before = access.byLinkCalls();
        int byLink = shop.status("GET", "/orders/by-link/" + Catalogue.GIFT_ORDER, "ana");
        boolean ran = access.byLinkCalls() == before + 1;
        line(out, "  THE SAME CHECK WITH @PostAuthorize (after the lookup):");
        line(out, "    ana opens ben's gift link: " + byLink + "  <- CONFIRMS IT EXISTS");
        line(out, "    the lookup had already run: " + ran);
        line(out, "    ben opens his own gift link: " + shop.status("GET", "/orders/by-link/" + Catalogue.GIFT_ORDER, "ben"));
        int succeeded = 0;
        for (Attempt attack : ATTACKS) {
            if (shop.status(attack) == 200) {
                succeeded++;
            }
        }
        int honest = 0;
        for (Attempt request : HONEST) {
            if (shop.status(request) == 200) {
                honest++;
            }
        }
        line(out, "  THE TOTAL (all nine attacks, six honest requests):");
        line(out, "    attacks that succeed: " + succeeded + " of " + ATTACKS.size());
        line(out, "    honest requests that still work: " + honest + " of " + HONEST.size());
    }

    private static void act4MethodSecurityAndLog(StringBuilder out, Running shop) {
        line(out, "FOUR. Method security, and a log of every refusal.");
        line(out, "  THE RULE ON THE METHOD (it travels with the method):");
        line(out, "    RefundService.refund carries");
        line(out, "    @PreAuthorize(\"hasAnyRole('SUPPORT', 'ADMIN')\")");
        line(out, "  A NIGHTLY JOB CALLS IT DIRECTLY (no URL in between):");
        line(out, "    running as ana (CUSTOMER): " + refundAs(shop, "ana", "CUSTOMER"));
        line(out, "    running as sam (SUPPORT): " + refundAs(shop, "sam", "SUPPORT"));
        List<String> log = shop.bean(DenialLog.class).lines();
        line(out, "  THE LOG (" + log.size() + " refusals so far, four of them):");
        for (String wanted : List.of("DENY someone signed out GET /admin/reports", "DENY ana DELETE /admin/users/ben",
                "DENY ana GET /orders/{id}: order 1002 is not theirs", "DENY ana RefundService.refund")) {
            line(out, "    " + log.stream().filter(l -> l.equals(wanted)).findFirst().orElse("missing: " + wanted));
        }
        boolean secret = log.stream().anyMatch(l -> l.contains(Catalogue.PASSWORD) || l.contains("Basic "));
        line(out, "    a password or Authorization header in it: " + secret);
        line(out, "  WHO WRITES THE LOG:");
        line(out, "    Spring: an AuthorizationDeniedEvent for each refusal");
        line(out, "    the store: the object-level refusals");
    }

    private static String refundAs(Running shop, String name, String role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(name, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
        SecurityContextHolder.setContext(context);
        try {
            shop.bean(RefundService.class).refund("1002");
            return "recorded";
        } catch (AccessDeniedException e) {
            return "refused";
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static void act5InventoryAndBill(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "FIVE. The route inventory, and the bill.");
        List<String> routes = RouteInventory.routes(shop.context());
        List<String> missing = RouteInventory.withoutRule(shop.context());
        line(out, "  THE ROUTE INVENTORY (read from Spring MVC's handler mapping):");
        line(out, "    routes the controllers serve: " + routes.size());
        line(out, "    a developer adds GET /admin/export, and forgets its rule");
        line(out, "    routes without a rule: " + missing + "  <- BUILD FAILS");
        line(out, "    dave the admin asks for it: " + shop.status("GET", "/admin/export", "dave") + ", denied by default");
        line(out, "  THE BILL:");
        line(out, "    rules live in two places: route list and annotations");
        line(out, "    the inventory test keeps the route list complete");
        line(out, "    every object check needs the owner in the query");
        line(out, "    @PostAuthorize is a fallback when it cannot be there");
    }
}
