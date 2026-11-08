package com.jk.explore.security.backupsrustfs;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;

/** Runs the online store story in five numbered acts against a real object store in Docker, and prints exact, repeatable results. */
public class BackupsRustfsDemo {

    static final String ORDERS = "shop-orders";
    static final String BACKUPS = "shop-backups";
    static final String WRITER = "backup-writer";
    static final String WRITER_SECRET = "backup-writer-secret-1";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the test can compare it with the golden file. It needs Docker. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (ObjectStoreContainer container = ObjectStoreContainer.start(); Store root = Store.root(container)) {
            act1Versioning(out, root);
            act2ObjectLock(out, root);
            act3WriteOnlyIdentity(out, root, container);
            act4Checksums(out, root);
            act5LifecycleAndCost(out, root);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    private static void row(StringBuilder out, String what, String result) {
        line(out, "    " + pad(what, 27) + result);
    }

    private static void act1Versioning(StringBuilder out, Store root) {
        line(out, "ONE. Versioning: an overwrite does not destroy the old data.");
        root.createBucket(ORDERS, false);
        root.enableVersioning(ORDERS);
        line(out, "  THE BUCKET");
        line(out, "    " + ORDERS + ", versioning on");
        byte[] original = Store.bytes("order A-1001, order A-1002, order A-1003");
        byte[] scrambled = Store.bytes("\u0000ENCRYPTED BY RANSOMWARE\u0000");
        String good = root.put(ORDERS, "orders.csv", original);
        String bad = root.put(ORDERS, "orders.csv", scrambled);
        line(out, "  THE WRITES TO orders.csv");
        line(out, "    1  the shop's orders, " + original.length + " bytes");
        line(out, "    2  ransomware's scrambled bytes, " + scrambled.length + " bytes");
        line(out, "  THE HISTORY");
        row(out, "versions kept", String.valueOf(root.versionIds(ORDERS, "orders.csv").size()));
        row(out, "a normal read is 2", String.valueOf(Arrays.equals(root.get(ORDERS, "orders.csv"), scrambled)));
        row(out, "version 1 is original",
                String.valueOf(Arrays.equals(root.getVersion(ORDERS, "orders.csv", good), original)));
        root.put(ORDERS, "orders.csv", root.getVersion(ORDERS, "orders.csv", good));
        line(out, "  THE RESTORE: COPY VERSION 1 BACK AS VERSION 3");
        row(out, "a normal read is original", String.valueOf(Arrays.equals(root.get(ORDERS, "orders.csv"), original)));
        row(out, "versions kept", String.valueOf(root.versionIds(ORDERS, "orders.csv").size()));
        row(out, "2 kept as evidence", String.valueOf(root.versionIds(ORDERS, "orders.csv").contains(bad)));
        root.delete(ORDERS, "orders.csv");
        line(out, "  A PLAIN DELETE");
        row(out, "delete markers added", String.valueOf(root.deleteMarkerCount(ORDERS, "orders.csv")));
        row(out, "a normal read", Store.outcome(() -> root.get(ORDERS, "orders.csv")));
        row(out, "version 1 still there",
                String.valueOf(Arrays.equals(root.getVersion(ORDERS, "orders.csv", good), original)));
        line(out, "    <- WHOEVER CAN DELETE VERSIONS CAN STILL ERASE ALL");
    }

    private static void act2ObjectLock(StringBuilder out, Store root) {
        line(out, "TWO. Object Lock: nobody can delete a locked version.");
        root.createBucket(BACKUPS, true);
        Instant until = Instant.now().plus(1, ChronoUnit.DAYS);
        String locked = root.put(BACKUPS, "backup-full.tar", Store.bytes("full backup of the shop"));
        root.retain(BACKUPS, "backup-full.tar", locked, ObjectLockRetentionMode.COMPLIANCE, until, false);
        String soft = root.put(BACKUPS, "backup-soft.tar", Store.bytes("a backup with the softer lock"));
        root.retain(BACKUPS, "backup-soft.tar", soft, ObjectLockRetentionMode.GOVERNANCE, until, false);
        line(out, "  THE BUCKET");
        line(out, "    " + BACKUPS + ", object lock on");
        line(out, "  THE LOCKS");
        line(out, "    backup-full.tar  COMPLIANCE  until now + 1 day");
        line(out, "    backup-soft.tar  GOVERNANCE  until now + 1 day");
        line(out, "  THE ROOT ACCOUNT TRIES THE COMPLIANCE VERSION");
        row(out, "delete the version",
                Store.outcome(() -> root.deleteVersion(BACKUPS, "backup-full.tar", locked, false)));
        row(out, "delete, bypass governance",
                Store.outcome(() -> root.deleteVersion(BACKUPS, "backup-full.tar", locked, true)));
        row(out, "shorten the retention", Store.outcome(() -> root.retain(BACKUPS, "backup-full.tar", locked,
                ObjectLockRetentionMode.COMPLIANCE, Instant.now().plus(1, ChronoUnit.HOURS), true)));
        row(out, "plain delete, a marker", Store.outcome(() -> root.delete(BACKUPS, "backup-full.tar")));
        row(out, "the version still reads",
                String.valueOf(root.getVersion(BACKUPS, "backup-full.tar", locked).length > 0));
        line(out, "  THE ROOT ACCOUNT TRIES THE GOVERNANCE VERSION");
        row(out, "delete the version",
                Store.outcome(() -> root.deleteVersion(BACKUPS, "backup-soft.tar", soft, false)));
        row(out, "delete, bypass governance",
                Store.outcome(() -> root.deleteVersion(BACKUPS, "backup-soft.tar", soft, true)));
        line(out, "    <- COMPLIANCE: NOBODY UNTIL THE DATE. CHOOSE WITH CARE");
    }

    private static void act3WriteOnlyIdentity(StringBuilder out, Store root, ObjectStoreContainer container)
            throws IOException, InterruptedException {
        line(out, "THREE. The backup job gets an identity that can only add objects.");
        container.createWriteOnlyUser(WRITER, WRITER_SECRET, BACKUPS);
        String policy = ObjectStoreContainer.writeOnlyPolicy(BACKUPS);
        line(out, "  THE POLICY OF " + WRITER);
        for (String field : new String[] {"Effect", "Action", "Resource"}) {
            Matcher m = Pattern.compile("\"" + field + "\":(\\S+?)[,}]").matcher(policy);
            if (m.find()) {
                line(out, "    " + pad(field, 10) + m.group(1).replace("[", "").replace("]", "").replace("\"", ""));
            }
        }
        line(out, "  THE REQUESTS AND THE ANSWERS");
        try (Store writer = new Store(container.endpoint(), WRITER, WRITER_SECRET)) {
            row(out, "put nightly.tar",
                    Store.outcome(() -> writer.put(BACKUPS, "nightly.tar", Store.bytes("nightly"))));
            row(out, "get nightly.tar", Store.outcome(() -> writer.get(BACKUPS, "nightly.tar")));
            row(out, "list its versions", Store.outcome(() -> writer.versionIds(BACKUPS, "nightly.tar")));
            row(out, "delete it", Store.outcome(() -> writer.delete(BACKUPS, "nightly.tar")));
            String version = root.versionIds(BACKUPS, "nightly.tar").get(0);
            row(out, "delete a version",
                    Store.outcome(() -> writer.deleteVersion(BACKUPS, "nightly.tar", version, false)));
            row(out, "put into " + ORDERS, Store.outcome(() -> writer.put(ORDERS, "x", Store.bytes("x"))));
        }
        line(out, "    <- A STOLEN KEY CAN ADD, NOT READ OR DESTROY");
        line(out, "    it can add junk: set a quota, alert on odd uploads");
    }

    private static void act4Checksums(StringBuilder out, Store root) {
        line(out, "FOUR. A checksum catches a damaged backup.");
        byte[] data = Store.bytes("backup bytes that must arrive intact");
        byte[] other = Store.bytes("different bytes");
        line(out, "  THE CHECKSUMS (SHA-256 IN BASE64)");
        line(out, "    the backup   " + Store.sha256Base64(data));
        line(out, "    other bytes  " + Store.sha256Base64(other));
        line(out, "  THE UPLOADS");
        line(out, "    checked.tar  the backup, with its own checksum");
        line(out, "    damaged.tar  the backup, with other bytes' checksum");
        line(out, "  THE ANSWERS");
        row(out, "checked.tar", Store.outcome(() -> root.put(BACKUPS, "checked.tar", data)));
        row(out, "damaged.tar",
                Store.outcome(() -> root.putWithChecksumOf(BACKUPS, "damaged.tar", data, other)));
        line(out, "  WHAT IS STORED");
        row(out, "checked.tar, same hash", String.valueOf(
                Store.sha256Base64(root.get(BACKUPS, "checked.tar")).equals(Store.sha256Base64(data))));
        row(out, "damaged.tar, versions", String.valueOf(root.versionIds(BACKUPS, "damaged.tar").size()));
        line(out, "    <- INTACT IS NOT USABLE: STILL RUN A RESTORE TEST");
    }

    private static void act5LifecycleAndCost(StringBuilder out, Store root) {
        line(out, "FIVE. Keeping history without keeping it for ever, and the cost.");
        root.expireOldVersionsAfter(ORDERS, 30);
        var rules = root.lifecycleRules(ORDERS);
        var rule = rules.get(0);
        int days = rule.noncurrentVersionExpiration().noncurrentDays();
        line(out, "  THE LIFECYCLE RULE ON " + ORDERS);
        line(out, "    rules " + rules.size() + ", id " + rule.id() + ", " + rule.statusAsString());
        line(out, "    an old version is removed " + days + " days after it is replaced");
        int megabytes = 2;
        int perDay = 24;
        int kept = perDay * days;
        line(out, "  THE COST, WORKED OUT");
        line(out, "    orders.csv of " + megabytes + " MB, overwritten every hour");
        line(out, "    old versions kept: " + perDay + " x " + days + " days = " + kept);
        line(out, "    storage: " + kept + " x " + megabytes + " MB = " + kept * megabytes + " MB for one " + megabytes + " MB file");
        line(out, "  THE LIMITS");
        line(out, "    a COMPLIANCE date set wrong is paid for in storage");
        line(out, "    the root account can change every policy:");
        line(out, "      keep it out of daily use");
        line(out, "    a lock stops the API, not someone deleting the disks:");
        line(out, "      keep a copy somewhere else too");
    }
}
