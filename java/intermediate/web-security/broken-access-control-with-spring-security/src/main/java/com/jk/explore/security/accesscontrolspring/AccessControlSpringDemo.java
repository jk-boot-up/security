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
            new Attempt("ana reads ben's order 1002 (object level, horizontal)", "GET", "/orders/1002", "ana"),
            new Attempt("ana reads ben's gift order from a leaked link", "GET", "/orders/" + Catalogue.GIFT_ORDER, "ana"),
            new Attempt("ana sends ben's order 1002 to her address (horizontal)", "POST", "/orders/1002" + TO_ANA, "ana"),
            new Attempt("ana cancels ben's order 1002", "DELETE", "/orders/1002", "ana"),
            new Attempt("someone signed out reads the sales report (forgotten route)", "GET", "/admin/reports", null),
            new Attempt("ana reads the sales report (vertical)", "GET", "/admin/reports", "ana"),
            new Attempt("ana refunds order 1002 (button hidden, route open)", "POST", "/admin/refunds?order=1002", "ana"),
            new Attempt("ana lists the users", "GET", "/admin/users", "ana"),
            new Attempt("ana deletes ben's account", "DELETE", "/admin/users/ben", "ana"));

    /** Six honest requests that must keep working. */
    static final List<Attempt> HONEST = List.of(
            new Attempt("someone signed out reads the menu", "GET", "/menu", null),
            new Attempt("ana reads her account page", "GET", "/account", "ana"),
            new Attempt("ana reads her order 1001", "GET", "/orders/1001", "ana"),
            new Attempt("ana moves her order 1001", "POST", "/orders/1001" + TO_ANA, "ana"),
            new Attempt("dave the admin lists the users", "GET", "/admin/users", "dave"),
            new Attempt("sam from support refunds order 1002", "POST", "/admin/refunds?order=1002", "sam"));

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

    private static void act1Naive(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "ONE. A Spring store with three common mistakes.");
        line(out, "  its rules: /account and /orders/** need a sign-in, /admin/users needs ADMIN, and anyRequest().permitAll() for the rest");
        line(out, "  ana reads her own order 1001: " + shop.status("GET", "/orders/1001", "ana"));
        line(out, "  ana reads ben's order 1002: " + shop.status("GET", "/orders/1002", "ana") + "  <- THE RULE SAID SIGNED IN, NOT WHOSE ORDER");
        line(out, "  someone signed out reads /admin/reports: " + shop.status("GET", "/admin/reports", null)
                + "  <- permitAll() LET THROUGH A ROUTE NOBODY LISTED");
        int deleted = shop.status("DELETE", "/admin/users/ben", "ana");
        line(out, "  ana: GET /admin/users " + shop.status("GET", "/admin/users", "ana") + ", DELETE /admin/users/ben " + deleted
                + "  <- THE PATTERN /admin/users DOES NOT COVER /admin/users/ben");
        line(out, "  ben's account still exists: " + shop.bean(InMemoryUserDetailsManager.class).userExists("ben"));
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
        line(out, "  the nine attacks of the plain project: " + succeeded + " of " + ATTACKS.size()
                + " succeed; the one handler written carefully, DELETE /orders/{id}, refused ana: " + shop.status("DELETE", "/orders/1002", "ana"));
        line(out, "  the page hid the admin links from ana: " + Wire.call(shop.port(), "GET", "/account", "ana").body());
    }

    private static void act2DenyByDefault(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "TWO. Deny by default in the filter chain.");
        line(out, "  every route has a rule by method and path, and anyRequest().denyAll() is the last rule");
        line(out, "  someone signed out reads /admin/reports: " + shop.status("GET", "/admin/reports", null) + "  <- WHO ARE YOU?");
        line(out, "  ana reads /admin/reports: " + shop.status("GET", "/admin/reports", "ana"));
        line(out, "  ana refunds order 1002: " + shop.status("POST", "/admin/refunds?order=1002", "ana"));
        line(out, "  ana: GET /admin/users " + shop.status("GET", "/admin/users", "ana") + ", DELETE /admin/users/ben "
                + shop.status("DELETE", "/admin/users/ben", "ana"));
        line(out, "  ana sends PUT /orders/1001, a method nobody wrote a rule for: " + shop.status("PUT", "/orders/1001", "ana")
                + "  <- NO RULE, NO ACCESS");
        line(out, "  dave the admin asks for /admin/debug, a path nobody wrote: " + shop.status("GET", "/admin/debug", "dave"));
        line(out, "  but the rule for GET /orders/{id} can only say signed in: a URL rule cannot know whose order 1002 is");
    }

    private static void act3ObjectLevel(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "THREE. The object-level check, in the query.");
        for (Attempt attack : ATTACKS.subList(0, 4)) {
            line(out, "  " + attack.label() + ": " + shop.status(attack));
        }
        line(out, "  the lookup is findByIdAndOwner(id, ana), so ben's order is not found: 404, like a missing order");
        line(out, "  sam from support reads order 1002: " + shop.status("GET", "/orders/1002", "sam") + ", staff may read any order");
        OrderAccess access = shop.bean(OrderAccess.class);
        int before = access.byLinkCalls();
        int byLink = shop.status("GET", "/orders/by-link/" + Catalogue.GIFT_ORDER, "ana");
        line(out, "  the same check with @PostAuthorize instead: ana opens ben's gift link: " + byLink + ", and the lookup ran: "
                + (access.byLinkCalls() == before + 1) + "  <- 403 CONFIRMS THE ORDER EXISTS, AND THE WORK WAS ALREADY DONE");
        line(out, "  ben opens his own gift link: " + shop.status("GET", "/orders/by-link/" + Catalogue.GIFT_ORDER, "ben"));
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
        line(out, "  attacks that succeed: " + succeeded + " of " + ATTACKS.size() + "; honest requests that still work: " + honest + " of " + HONEST.size());
    }

    private static void act4MethodSecurityAndLog(StringBuilder out, Running shop) {
        line(out, "FOUR. Method security, and a log of every refusal.");
        line(out, "  RefundService.refund carries @PreAuthorize(\"hasAnyRole('SUPPORT', 'ADMIN')\"), so the rule travels with the method");
        line(out, "  a nightly job running as ana calls the refund service directly, with no URL in between: " + refundAs(shop, "ana", "CUSTOMER"));
        line(out, "  the same job running as sam: " + refundAs(shop, "sam", "SUPPORT"));
        List<String> log = shop.bean(DenialLog.class).lines();
        line(out, "  refusals logged so far: " + log.size() + ", for example:");
        for (String wanted : List.of("DENY someone signed out GET /admin/reports", "DENY ana DELETE /admin/users/ben",
                "DENY ana GET /orders/{id}: order 1002 is not theirs", "DENY ana RefundService.refund")) {
            line(out, "    " + log.stream().filter(l -> l.equals(wanted)).findFirst().orElse("missing: " + wanted));
        }
        boolean secret = log.stream().anyMatch(l -> l.contains(Catalogue.PASSWORD) || l.contains("Basic "));
        line(out, "  a password or an Authorization header appears in the log: " + secret);
        line(out, "  the filter chain and method security publish an AuthorizationDeniedEvent; the store logs the object-level refusals itself");
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
        line(out, "  routes the controllers serve, read from Spring MVC's handler mapping: " + routes.size());
        line(out, "  a developer has added GET /admin/export and forgot its rule; routes without a rule: " + missing
                + "  <- THE ROUTE INVENTORY TEST FAILS THE BUILD");
        line(out, "  until it has one, even dave the admin is refused: " + shop.status("GET", "/admin/export", "dave") + ", denied by default");
        line(out, "  the bill: rules now live in two places, the route list and the method annotations, and the inventory test keeps the first complete");
        line(out, "  every object-level check needs the owner in the query, and @PostAuthorize is a fallback for when it cannot be there");
    }
}
