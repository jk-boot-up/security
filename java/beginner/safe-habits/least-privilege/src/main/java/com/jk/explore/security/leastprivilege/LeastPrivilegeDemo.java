package com.jk.explore.security.leastprivilege;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class LeastPrivilegeDemo {

    static final int TOTAL = Catalogue.PERMISSIONS.size();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        act1RolesAndPermissions(out);
        act2BlastRadius(out);
        act3SeparationOfDuties(out);
        act4ExpiringGrants(out);
        act5CreepAndTheOperatingSystem(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1RolesAndPermissions(StringBuilder out) {
        line(out, "ONE. Roles and permissions, and deny by default.");
        line(out, "  THE CATALOGUE (" + TOTAL + " things that can be done in the store):");
        Map<String, List<String>> byResource = new LinkedHashMap<>();
        for (String permission : Catalogue.PERMISSIONS) {
            String[] parts = permission.split(":");
            byResource.computeIfAbsent(parts[0], r -> new ArrayList<>()).add(parts[1]);
        }
        byResource.forEach((resource, actions) ->
                line(out, "    " + pad(resource, 11) + String.join("  ", actions)));
        line(out, "  THE ROLES (what each one allows):");
        for (String role : List.of("admin", "support", "warehouse", "finance")) {
            Policy policy = Catalogue.ROLES.get(role);
            String count = policy.reachable(Catalogue.PERMISSIONS).size() + " of " + TOTAL;
            String patterns = String.join(" ", policy.statements().stream().map(Policy.Statement::pattern).toList());
            line(out, "    " + pad(role, 11) + pad(count, 10) + patterns);
        }
        decide(out, "support asks for refunds:approve", Catalogue.ROLES.get("support"), "refunds:approve",
                "NOTHING ALLOWS IT");
        decide(out, "an admin with deny secrets:* asks for secrets:read",
                Policy.of("allow *", "deny secrets:*"), "secrets:read", "A DENY BEATS ANY ALLOW");
    }

    /** One request, then every statement checked against it, then the decision. */
    private static void decide(StringBuilder out, String request, Policy policy, String permission, String note) {
        line(out, "  THE REQUEST: " + request);
        line(out, "  THE CHECK, STATEMENT BY STATEMENT:");
        for (Policy.Statement statement : policy.statements()) {
            String text = (statement.allow() ? "allow " : "deny ") + statement.pattern();
            String verdict = !Policy.matches(statement.pattern(), permission) ? "does not match"
                    : statement.allow() ? "matches: allowed, unless a deny matches" : "matches: a deny, which wins";
            line(out, "    " + pad(text, 22) + verdict);
        }
        line(out, "  THE DECISION: " + policy.decide(permission) + "  <- " + note);
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    private static void act2BlastRadius(StringBuilder out) {
        line(out, "TWO. Blast radius: what a stolen credential can reach.");
        Access access = new Access().assign("export-job-as-admin", "admin").assign("export-job", "exporter")
                .assign("careful-admin", "admin").restrict("careful-admin", Policy.of("deny secrets:*"));
        List<String> needed = List.of("orders:read", "reports:read");
        line(out, "  WHAT THE NIGHTLY ORDER EXPORT NEEDS:");
        line(out, "    " + String.join("  ", needed));
        blast(out, "BEFORE THE FIX (the job runs as admin, because it was quick):",
                access.policyOf("export-job-as-admin"), needed, "  <- EVERYTHING");
        blast(out, "AFTER THE FIX (the same job with only the exporter role):",
                access.policyOf("export-job"), needed, "");
        line(out, "  ANOTHER ADMIN, WITH AN EXPLICIT deny secrets:*:");
        line(out, "    a stolen token reaches " + reach(access, "careful-admin") + " of " + TOTAL);
        line(out, "  whatever the job does not need is risk without benefit");
    }

    private static void blast(StringBuilder out, String header, Policy policy, List<String> needed, String note) {
        List<String> reached = policy.reachable(Catalogue.PERMISSIONS);
        long extra = reached.stream().filter(p -> !needed.contains(p)).count();
        line(out, "  " + header);
        line(out, "    a stolen token reaches " + reached.size() + " of " + TOTAL + note);
        line(out, "    " + extra + " of them the job never needs");
    }

    private static int reach(Access access, String user) {
        return access.policyOf(user).reachable(Catalogue.PERMISSIONS).size();
    }

    private static void act3SeparationOfDuties(StringBuilder out) {
        line(out, "THREE. Separation of duties: two people for a refund.");
        Access access = new Access().assign("alice", "support").assign("carol", "finance").assign("dave", "admin");
        RefundDesk desk = new RefundDesk(access);
        line(out, "  THE RULE: approving needs refunds:approve, and not the creator");
        line(out, "  THE REQUESTS (who asks for what):");
        line(out, "    1  alice (support)  creates refund R-1");
        line(out, "    2  alice (support)  approves refund R-1");
        line(out, "    3  dave (admin)     creates refund R-2");
        line(out, "    4  dave (admin)     approves refund R-2");
        line(out, "    5  carol (finance)  approves refund R-1");
        line(out, "  THE RESULTS (what the refund desk says):");
        line(out, "    1  " + desk.create("R-1", "alice", 0));
        line(out, "    2  " + desk.approve("R-1", "alice", 0) + "  support has no refunds:approve");
        line(out, "    3  " + desk.create("R-2", "dave", 0));
        line(out, "    4  " + desk.approve("R-2", "dave", 0) + "  dave created R-2  <- ADMIN POWER DOES NOT HELP");
        line(out, "    5  " + desk.approve("R-1", "carol", 0) + "  R-1 approved: " + desk.isApproved("R-1"));
        line(out, "  nobody can move money alone: one stolen account is not enough");
    }

    private static void act4ExpiringGrants(StringBuilder out) {
        line(out, "FOUR. Access that expires by itself.");
        Access access = new Access().assign("carol", "finance");
        long from = 10;
        long until = 13;
        access.grant("carol", "payments:write", from, until);
        line(out, "  THE GRANT (for a month-end fix):");
        line(out, "    carol (finance) gets payments:write");
        line(out, "    from tick " + from + " until tick " + until + ", the first tick without it");
        line(out, "  THE CHECKS (can carol write payments?):");
        for (long tick : new long[] {9, 10, 12, 13}) {
            String when = tick < from ? "before the grant" : tick < until ? "inside the grant" : "expired by itself";
            line(out, "    tick " + pad(String.valueOf(tick), 4) + pad(String.valueOf(access.can("carol", "payments:write", tick)), 7)
                    + when);
        }
        line(out, "  nobody had to remember to remove it");
    }

    private static void act5CreepAndTheOperatingSystem(StringBuilder out) throws IOException {
        line(out, "FIVE. Privilege creep, and the operating system.");
        List<UsageAudit.Holding> holdings = List.of(
                new UsageAudit.Holding("alice", "orders:read", 198),
                new UsageAudit.Holding("alice", "customers:read", 150),
                new UsageAudit.Holding("bob", "inventory:write", 190),
                new UsageAudit.Holding("bob", "orders:read", -1),
                new UsageAudit.Holding("carol", "payments:read", 100));
        long today = 200;
        long days = 90;
        line(out, "  THE HOLDINGS (" + holdings.size() + " permissions, and when each was last used):");
        for (UsageAudit.Holding holding : holdings) {
            line(out, "    " + pad(holding.user(), 7) + pad(holding.permission(), 17) + lastUsed(holding));
        }
        line(out, "  THE AUDIT (unused for " + days + " days: before day " + today + " - " + days + " = "
                + (today - days) + "):");
        List<UsageAudit.Holding> unused = UsageAudit.unused(holdings, today, days);
        for (UsageAudit.Holding holding : unused) {
            line(out, "    " + pad(holding.user(), 7) + pad(holding.permission(), 17) + pad(lastUsed(holding), 22) + "<- REMOVE");
        }
        line(out, "    " + unused.size() + " of " + holdings.size() + " are candidates for removal");
        Path dir = Files.createTempDirectory("least-privilege-demo");
        Path file = dir.resolve("export.csv");
        try {
            Files.writeString(file, "order,total\nA-1001,40\n");
            line(out, "  BEFORE THE FIX (the export file as first written):");
            line(out, "    export.csv  " + OwnerOnly.openFile(file) + "  readable by others: " + OwnerOnly.readableByOthers(file));
            line(out, "  AFTER THE FIX (owner only):");
            line(out, "    export.csv  " + OwnerOnly.lockFile(file) + "  readable by others: " + OwnerOnly.readableByOthers(file));
            line(out, "    its folder  " + OwnerOnly.lockDirectory(dir));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
        line(out, "  the Java SecurityManager is disabled for good (JDK 24),");
        line(out, "  so the limits that remain are outside the program:");
        line(out, "  separate operating system users, containers, processes");
    }

    private static String lastUsed(UsageAudit.Holding holding) {
        return holding.lastUsedDay() < 0 ? "never used" : "last used on day " + holding.lastUsedDay();
    }
}
