package com.jk.explore.security.leastprivilege;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        line(out, "  the online store has " + TOTAL + " things that can be done, such as orders:read and refunds:approve");
        for (String role : List.of("admin", "support", "warehouse", "finance")) {
            line(out, "  the " + role + " role allows " + Catalogue.ROLES.get(role).reachable(Catalogue.PERMISSIONS).size() + " of " + TOTAL);
        }
        Policy support = Catalogue.ROLES.get("support");
        line(out, "  support asks for refunds:approve: " + support.decide("refunds:approve") + "  <- NOTHING ALLOWS IT, SO IT IS DENIED");
        Policy denied = Policy.of("allow *", "deny secrets:*");
        line(out, "  an admin with an explicit deny on secrets asks for secrets:read: " + denied.decide("secrets:read") + "  <- A DENY BEATS ANY ALLOW");
    }

    private static void act2BlastRadius(StringBuilder out) {
        line(out, "TWO. Blast radius: what a stolen credential can reach.");
        Access access = new Access().assign("export-job-as-admin", "admin").assign("export-job", "exporter")
                .assign("careful-admin", "admin").restrict("careful-admin", Policy.of("deny secrets:*"));
        line(out, "  the nightly order export runs as an admin, because that was quick: a stolen token reaches "
                + reach(access, "export-job-as-admin") + " of " + TOTAL + "  <- EVERYTHING");
        line(out, "  the same job with only the exporter role: a stolen token reaches " + reach(access, "export-job") + " of " + TOTAL);
        line(out, "  an admin account with an explicit deny on secrets reaches " + reach(access, "careful-admin") + " of " + TOTAL);
        line(out, "  the job needs only orders:read and reports:read, so everything else it was given is risk without benefit");
    }

    private static int reach(Access access, String user) {
        return access.policyOf(user).reachable(Catalogue.PERMISSIONS).size();
    }

    private static void act3SeparationOfDuties(StringBuilder out) {
        line(out, "THREE. Separation of duties: two people for a refund.");
        Access access = new Access().assign("alice", "support").assign("carol", "finance").assign("dave", "admin");
        RefundDesk desk = new RefundDesk(access);
        line(out, "  alice (support) creates refund R-1: " + desk.create("R-1", "alice", 0));
        line(out, "  alice tries to approve it: " + desk.approve("R-1", "alice", 0) + "  (support cannot approve)");
        line(out, "  dave (admin) creates refund R-2 and tries to approve it himself: " + desk.create("R-2", "dave", 0) + ", then "
                + desk.approve("R-2", "dave", 0) + "  <- ADMIN POWER DOES NOT REMOVE THE RULE");
        line(out, "  carol (finance) approves R-1: " + desk.approve("R-1", "carol", 0) + ", approved: " + desk.isApproved("R-1"));
        line(out, "  nobody could move money alone, so one stolen account is not enough");
    }

    private static void act4ExpiringGrants(StringBuilder out) {
        line(out, "FOUR. Access that expires by itself.");
        Access access = new Access().assign("carol", "finance");
        access.grant("carol", "payments:write", 10, 13);
        line(out, "  carol (finance) is granted payments:write for ticks 10 to 12, for a month-end fix");
        for (long tick : new long[] {9, 10, 12, 13}) {
            line(out, "  at tick " + tick + " she can write payments: " + access.can("carol", "payments:write", tick));
        }
        line(out, "  at tick 13 the grant is gone, and nobody had to remember to remove it");
    }

    private static void act5CreepAndTheOperatingSystem(StringBuilder out) throws IOException {
        line(out, "FIVE. Privilege creep, and the operating system.");
        List<UsageAudit.Holding> holdings = List.of(
                new UsageAudit.Holding("alice", "orders:read", 198),
                new UsageAudit.Holding("alice", "customers:read", 150),
                new UsageAudit.Holding("bob", "inventory:write", 190),
                new UsageAudit.Holding("bob", "orders:read", -1),
                new UsageAudit.Holding("carol", "payments:read", 100));
        List<UsageAudit.Holding> unused = UsageAudit.unused(holdings, 200, 90);
        line(out, "  on day 200, of " + holdings.size() + " permissions held, " + unused.size() + " were not used in the last 90 days:");
        for (UsageAudit.Holding holding : unused) {
            line(out, "    " + holding.user() + " " + holding.permission() + (holding.lastUsedDay() < 0 ? ", never used" : ", last used on day " + holding.lastUsedDay()));
        }
        Path dir = Files.createTempDirectory("least-privilege-demo");
        Path file = dir.resolve("export.csv");
        try {
            Files.writeString(file, "order,total\nA-1001,40\n");
            line(out, "  an export file with permissions " + OwnerOnly.openFile(file) + " is readable by others: " + OwnerOnly.readableByOthers(file));
            line(out, "  with permissions " + OwnerOnly.lockFile(file) + " it is readable by others: " + OwnerOnly.readableByOthers(file)
                    + ", and its folder is " + OwnerOnly.lockDirectory(dir));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
        line(out, "  the Java SecurityManager that once limited code inside one program has been removed from the JDK");
        line(out, "  the limits that remain are outside the program: separate operating system users, containers and separate processes");
    }
}
