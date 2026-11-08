package com.jk.explore.security.leastprivilegespring;

import java.io.IOException;
import java.util.List;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Runs the online store story in five numbered acts against a real Spring Boot server on the loopback interface. */
public class LeastPrivilegeSpringDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** A store started on a free port. Closing it stops the server. */
    record Running(ConfigurableApplicationContext context, int port) implements AutoCloseable {

        <T> T bean(Class<T> type) {
            return context.getBean(type);
        }

        @Override
        public void close() {
            context.close();
        }
    }

    static Running start() {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Shop.class)
                .properties("server.port=0", "server.address=127.0.0.1", "logging.level.root=OFF", "spring.main.banner-mode=off")
                .run();
        return new Running(context, Integer.parseInt(context.getEnvironment().getProperty("local.server.port")));
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Running shop = start()) {
            act1DenyByDefault(out, shop);
            act2MethodSecurity(out, shop);
            act3SeparationOfDuties(out, shop);
            act4HierarchyAndBlastRadius(out, shop);
            act5Grants(out, shop);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static int status(Running shop, String method, String path, String user) throws IOException, InterruptedException {
        return Wire.call(shop.port(), method, path, user).status();
    }

    private static void act1DenyByDefault(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "ONE. Deny by default: every path needs a rule.");
        line(out, "  THE RULES (in Shop.chain, checked top to bottom):");
        line(out, "    GET /orders       needs orders:read");
        line(out, "    GET /reports      needs reports:read");
        line(out, "    GET /secrets      needs secrets:read");
        line(out, "    GET /staff/**     needs the role SUPPORT");
        line(out, "    PUT /payments/**  needs payments:write, or a grant");
        line(out, "    /refunds/**       signed in; the methods decide");
        line(out, "    anything else     denyAll");
        String[][] requests = {
            {"no credentials", "GET", "/orders", null, null},
            {"alice, wrong password", "GET", "/orders", "alice", "guess"},
            {"alice (support)", "GET", "/orders", "alice", Catalogue.PASSWORD},
            {"alice (support)", "GET", "/reports", "alice", Catalogue.PASSWORD},
            {"dave (admin)", "GET", "/admin/debug", "dave", Catalogue.PASSWORD}};
        String[] why = {"who are you?", "wrong password, so still unknown", "she holds orders:read",
            "known, but no reports:read", "no rule matches, so denyAll  <- EVEN FOR AN ADMIN"};
        line(out, "  THE REQUESTS:");
        for (int i = 0; i < requests.length; i++) {
            line(out, "    " + (i + 1) + "  " + pad(requests[i][0], 23) + requests[i][1] + " " + requests[i][2]);
        }
        line(out, "  THE RESPONSES:");
        for (int i = 0; i < requests.length; i++) {
            String[] r = requests[i];
            int status = Wire.call(shop.port(), r[1], r[2], r[3], r[4]).status();
            line(out, "    " + (i + 1) + "  " + status + "  " + why[i]);
        }
        line(out, "  401: not signed in.  403: signed in, but not allowed.");
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    private static void act2MethodSecurity(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "TWO. Method security: the rule sits next to the code it guards.");
        line(out, "  THE RULES ON THE METHODS (in Refunds):");
        line(out, "    create()  @PreAuthorize   needs refunds:create");
        line(out, "    view()    @PostAuthorize  requester, or refunds:approve");
        Refunds refunds = shop.bean(Refunds.class);
        int bobCreate = status(shop, "POST", "/refunds", "bob");
        Wire.Reply created = Wire.call(shop.port(), "POST", "/refunds", "alice");
        int aliceView = status(shop, "GET", "/refunds/" + created.body(), "alice");
        int carolView = status(shop, "GET", "/refunds/" + created.body(), "carol");
        int ranBefore = refunds.timesViewMethodRan();
        int bobView = status(shop, "GET", "/refunds/" + created.body(), "bob");
        boolean ranForBob = refunds.timesViewMethodRan() == ranBefore + 1;
        line(out, "  THE REQUESTS:");
        line(out, "    1  bob (warehouse)  POST /refunds");
        line(out, "    2  alice (support)  POST /refunds");
        line(out, "    3  alice            GET /refunds/" + created.body());
        line(out, "    4  carol (finance)  GET /refunds/" + created.body());
        line(out, "    5  bob              GET /refunds/" + created.body());
        line(out, "  THE RESPONSES:");
        line(out, "    1  " + bobCreate + "  no refunds:create, so create() never ran");
        line(out, "    2  " + created.status() + "  the new refund is " + created.body());
        line(out, "    3  " + aliceView + "  her own request");
        line(out, "    4  " + carolView + "  an approver may read any");
        line(out, "    5  " + bobView + "  someone else's request");
        line(out, "  did view() run for bob before his 403? " + ranForBob);
        line(out, "  <- @PostAuthorize RUNS FIRST: NO SIDE EFFECTS");
    }

    private static void act3SeparationOfDuties(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "THREE. Separation of duties: two people for a refund.");
        line(out, "  THE RULE ON approve() (in Refunds):");
        line(out, "    hasAuthority('refunds:approve')");
        line(out, "    and @refundRules.differentPerson(#id, authentication)");
        int aliceOwn = status(shop, "POST", "/refunds/R-1/approve", "alice");
        Wire.Reply second = Wire.call(shop.port(), "POST", "/refunds", "dave");
        int daveOwn = status(shop, "POST", "/refunds/" + second.body() + "/approve", "dave");
        Wire.Reply carol = Wire.call(shop.port(), "POST", "/refunds/R-1/approve", "carol");
        Wire.Reply third = Wire.call(shop.port(), "POST", "/refunds", "alice");
        Wire.Reply dave = Wire.call(shop.port(), "POST", "/refunds/" + third.body() + "/approve", "dave");
        line(out, "  THE REQUESTS:");
        line(out, "    1  alice (support)  approves her own R-1");
        line(out, "    2  dave (admin)     creates " + second.body() + ", then approves it");
        line(out, "    3  carol (finance)  approves R-1, alice's");
        line(out, "    4  dave (admin)     approves " + third.body() + ", which alice created");
        line(out, "  THE RESPONSES:");
        line(out, "    1  " + aliceOwn + "  no refunds:approve");
        line(out, "    2  " + daveOwn + "  same person  <- ADMIN POWER DOES NOT HELP");
        line(out, "    3  " + carol.status() + "  " + carol.body());
        line(out, "    4  " + dave.status() + "  " + dave.body() + ": someone else's request");
        line(out, "  the rule is one bean the annotation calls: no role turns it off");
    }

    private static void act4HierarchyAndBlastRadius(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "FOUR. A role hierarchy, and what a stolen credential can reach.");
        line(out, "  THE HIERARCHY: ADMIN includes SUPPORT, FINANCE and WAREHOUSE");
        line(out, "  GET /staff/queue NEEDS THE ROLE SUPPORT:");
        for (String user : List.of("alice", "dave", "carol", "exporter")) {
            String role = Catalogue.ACCOUNTS.get(user).role();
            line(out, "    " + pad(user + " (" + role + ")", 21) + status(shop, "GET", "/staff/queue", user)
                    + (role.equals("ADMIN") ? "  ADMIN includes SUPPORT" : ""));
        }
        int total = Catalogue.PERMISSIONS.size();
        Catalogue.Account exporter = Catalogue.ACCOUNTS.get("exporter");
        line(out, "  BEFORE THE FIX (the nightly export runs as dave, the admin):");
        line(out, "    a stolen token reaches " + BlastRadius.reach(Catalogue.ACCOUNTS.get("dave")) + " of " + total
                + "  <- EVERYTHING");
        line(out, "  AFTER THE FIX (the exporter service account):");
        line(out, "    it holds " + String.join(" ", exporter.permissions()));
        line(out, "    a stolen token reaches " + BlastRadius.reach(exporter) + " of " + total);
        line(out, "    GET /orders: " + status(shop, "GET", "/orders", "exporter")
                + ", GET /secrets: " + status(shop, "GET", "/secrets", "exporter"));
        line(out, "  the others: alice " + BlastRadius.reach(Catalogue.ACCOUNTS.get("alice")) + " of " + total
                + ", bob " + BlastRadius.reach(Catalogue.ACCOUNTS.get("bob"))
                + ", carol " + BlastRadius.reach(Catalogue.ACCOUNTS.get("carol")));
        line(out, "  each count asks Spring Security's own authorization manager");
    }

    private static void act5Grants(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "FIVE. Access that expires by itself.");
        SimulatedClock clock = shop.bean(SimulatedClock.class);
        shop.bean(Grants.class).give("carol", "payments:write", 10, 12);
        line(out, "  THE GRANT (for a month-end fix):");
        line(out, "    carol (finance) gets payments:write");
        line(out, "    from tick 10 to tick 12, both included");
        line(out, "  THE REQUESTS AND RESPONSES (PUT /payments/P-1):");
        String[][] checks = {{"9", "carol", "before the grant"}, {"10", "carol", "inside the grant"},
            {"12", "carol", "inside the grant"}, {"13", "carol", "expired by itself"},
            {"11", "alice", "never had a grant"}};
        for (String[] check : checks) {
            clock.set(Long.parseLong(check[0]));
            line(out, "    tick " + pad(check[0], 4) + pad(check[1], 7)
                    + status(shop, "PUT", "/payments/P-1", check[1]) + "  " + check[2]);
        }
        line(out, "  GrantAuthorization asks a clock the tests can set");
    }
}
