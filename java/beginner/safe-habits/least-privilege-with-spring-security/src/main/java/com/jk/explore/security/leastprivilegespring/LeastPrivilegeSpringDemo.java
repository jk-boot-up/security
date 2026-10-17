package com.jk.explore.security.leastprivilegespring;

import java.io.IOException;
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
        line(out, "  no credentials, GET /orders: " + status(shop, "GET", "/orders", null) + "  <- WHO ARE YOU?");
        line(out, "  alice (support) with a wrong password: " + Wire.call(shop.port(), "GET", "/orders", "alice", "guess").status());
        line(out, "  alice (support) GET /orders: " + status(shop, "GET", "/orders", "alice"));
        line(out, "  alice (support) GET /reports: " + status(shop, "GET", "/reports", "alice") + "  <- KNOWN, BUT NOT ALLOWED");
        line(out, "  dave (admin) GET /admin/debug, a path nobody wrote a rule for: " + status(shop, "GET", "/admin/debug", "dave")
                + "  <- NO RULE MEANS NO, EVEN FOR AN ADMIN");
        line(out, "  the last rule is anyRequest().denyAll(), so a forgotten endpoint stays closed");
    }

    private static void act2MethodSecurity(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "TWO. Method security: the rule sits next to the code it guards.");
        line(out, "  bob (warehouse) POST /refunds: " + status(shop, "POST", "/refunds", "bob") + "  <- NO refunds:create");
        Wire.Reply created = Wire.call(shop.port(), "POST", "/refunds", "alice");
        line(out, "  alice (support) POST /refunds: " + created.status() + ", the new refund is " + created.body());
        Refunds refunds = shop.bean(Refunds.class);
        line(out, "  alice GET /refunds/R-1: " + status(shop, "GET", "/refunds/R-1", "alice") + ", her own request");
        line(out, "  carol (finance) GET /refunds/R-1: " + status(shop, "GET", "/refunds/R-1", "carol") + ", an approver may read any");
        int deniedBefore = refunds.timesViewMethodRan();
        line(out, "  bob GET /refunds/R-1: " + status(shop, "GET", "/refunds/R-1", "bob") + ", someone else's request");
        line(out, "  the method ran for bob before the answer was withheld: " + (refunds.timesViewMethodRan() == deniedBefore + 1)
                + "  <- @PostAuthorize JUDGES THE RESULT, SO KEEP SIDE EFFECTS OUT OF SUCH METHODS");
        line(out, "  @PreAuthorize stops a call before it runs, and @PostAuthorize judges it after it has run");
    }

    private static void act3SeparationOfDuties(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "THREE. Separation of duties: two people for a refund.");
        line(out, "  alice approves her own refund R-1: " + status(shop, "POST", "/refunds/R-1/approve", "alice") + "  <- NO refunds:approve");
        Wire.Reply second = Wire.call(shop.port(), "POST", "/refunds", "dave");
        line(out, "  dave (admin) creates refund " + second.body() + " and then approves it himself: "
                + status(shop, "POST", "/refunds/" + second.body() + "/approve", "dave") + "  <- ADMIN POWER DOES NOT REMOVE THE RULE");
        line(out, "  carol (finance) approves R-1: " + Wire.call(shop.port(), "POST", "/refunds/R-1/approve", "carol").body());
        Wire.Reply third = Wire.call(shop.port(), "POST", "/refunds", "alice");
        line(out, "  alice creates " + third.body() + " and dave approves it: " + Wire.call(shop.port(), "POST", "/refunds/" + third.body() + "/approve", "dave").body()
                + "  <- AN ADMIN MAY APPROVE SOMEONE ELSE'S REQUEST");
        line(out, "  the rule is in one place, a bean that the annotation calls, and no role can switch it off");
    }

    private static void act4HierarchyAndBlastRadius(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "FOUR. A role hierarchy, and what a stolen credential can reach.");
        line(out, "  the staff queue needs the support role. alice (support): " + status(shop, "GET", "/staff/queue", "alice")
                + ", dave (admin, which includes support): " + status(shop, "GET", "/staff/queue", "dave")
                + ", carol (finance): " + status(shop, "GET", "/staff/queue", "carol")
                + ", exporter: " + status(shop, "GET", "/staff/queue", "exporter"));
        int total = Catalogue.PERMISSIONS.size();
        line(out, "  the nightly order export runs as dave, because that was quick: a stolen token reaches "
                + BlastRadius.reach(Catalogue.ACCOUNTS.get("dave")) + " of " + total + "  <- EVERYTHING");
        line(out, "  the same job as the exporter service account: a stolen token reaches " + BlastRadius.reach(Catalogue.ACCOUNTS.get("exporter")) + " of " + total);
        line(out, "  alice reaches " + BlastRadius.reach(Catalogue.ACCOUNTS.get("alice")) + " of " + total + ", bob "
                + BlastRadius.reach(Catalogue.ACCOUNTS.get("bob")) + ", carol " + BlastRadius.reach(Catalogue.ACCOUNTS.get("carol")));
        line(out, "  the exporter can read orders: " + status(shop, "GET", "/orders", "exporter") + ", and cannot reach the secrets: " + status(shop, "GET", "/secrets", "exporter"));
        line(out, "  the numbers come from asking Spring Security's own authorization manager about each action, not from a separate list");
    }

    private static void act5Grants(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "FIVE. Access that expires by itself.");
        SimulatedClock clock = shop.bean(SimulatedClock.class);
        shop.bean(Grants.class).give("carol", "payments:write", 10, 12);
        line(out, "  carol (finance) is granted payments:write for ticks 10 to 12, for a month-end fix");
        for (long tick : new long[] {9, 10, 12, 13}) {
            clock.set(tick);
            line(out, "  at tick " + tick + " PUT /payments/P-1: " + status(shop, "PUT", "/payments/P-1", "carol"));
        }
        line(out, "  alice, who never had a grant, at tick 11: " + tickStatus(shop, clock, 11, "alice"));
        line(out, "  the rule is a custom AuthorizationManager that asks an injected clock, so the demo and the tests control time");
    }

    private static int tickStatus(Running shop, SimulatedClock clock, long tick, String user) throws IOException, InterruptedException {
        clock.set(tick);
        return status(shop, "PUT", "/payments/P-1", user);
    }
}
