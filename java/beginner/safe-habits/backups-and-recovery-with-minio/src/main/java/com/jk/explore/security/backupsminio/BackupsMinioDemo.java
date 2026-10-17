package com.jk.explore.security.backupsminio;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;

/** Runs the online store story in five numbered acts against a real object store in Docker, and prints exact, repeatable results. */
public class BackupsMinioDemo {

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

    private static void act1Versioning(StringBuilder out, Store root) {
        line(out, "ONE. Versioning: an overwrite does not destroy the old data.");
        root.createBucket(ORDERS, false);
        root.enableVersioning(ORDERS);
        byte[] original = Store.bytes("order A-1001, order A-1002, order A-1003");
        String good = root.put(ORDERS, "orders.csv", original);
        byte[] scrambled = Store.bytes("\u0000ENCRYPTED BY RANSOMWARE\u0000");
        String bad = root.put(ORDERS, "orders.csv", scrambled);
        line(out, "  the shop stores orders.csv, then ransomware overwrites it with scrambled bytes");
        line(out, "  versions kept for orders.csv: " + root.versionIds(ORDERS, "orders.csv").size());
        line(out, "  what a normal read returns now is the scrambled file: " + Arrays.equals(root.get(ORDERS, "orders.csv"), scrambled));
        line(out, "  the original is still there as an older version: " + Arrays.equals(root.getVersion(ORDERS, "orders.csv", good), original));
        root.put(ORDERS, "orders.csv", root.getVersion(ORDERS, "orders.csv", good));
        line(out, "  restore = write the older version back as the newest: " + Arrays.equals(root.get(ORDERS, "orders.csv"), original));
        line(out, "  the scrambled version is still in the history as evidence: " + root.versionIds(ORDERS, "orders.csv").contains(bad));
        root.delete(ORDERS, "orders.csv");
        line(out, "  a plain delete only adds a delete marker (" + root.deleteMarkerCount(ORDERS, "orders.csv") + "): reading the key now fails, the data is not gone: "
                + Arrays.equals(root.getVersion(ORDERS, "orders.csv", good), original));
        line(out, "  but an attacker with delete rights on versions can still remove every version, which is what the next act stops");
    }

    private static void act2ObjectLock(StringBuilder out, Store root) {
        line(out, "TWO. Object Lock: nobody can delete a locked version, not even the root account.");
        root.createBucket(BACKUPS, true);
        Instant until = Instant.now().plus(1, ChronoUnit.DAYS);
        String locked = root.put(BACKUPS, "backup-full.tar", Store.bytes("full backup of the shop"));
        root.retain(BACKUPS, "backup-full.tar", locked, ObjectLockRetentionMode.COMPLIANCE, until, false);
        line(out, "  a backup is stored with COMPLIANCE retention for a day");
        line(out, "  delete that version: " + Store.outcome(() -> root.deleteVersion(BACKUPS, "backup-full.tar", locked, false)));
        line(out, "  delete it asking to bypass governance: " + Store.outcome(() -> root.deleteVersion(BACKUPS, "backup-full.tar", locked, true)));
        line(out, "  shorten the retention: " + Store.outcome(() -> root.retain(BACKUPS, "backup-full.tar", locked,
                ObjectLockRetentionMode.COMPLIANCE, Instant.now().plus(1, ChronoUnit.HOURS), true)));
        line(out, "  a plain delete is allowed but only adds a marker: " + Store.outcome(() -> root.delete(BACKUPS, "backup-full.tar")));
        line(out, "  the locked version is still readable: " + (root.getVersion(BACKUPS, "backup-full.tar", locked).length > 0));
        String soft = root.put(BACKUPS, "backup-soft.tar", Store.bytes("a backup with the softer lock"));
        root.retain(BACKUPS, "backup-soft.tar", soft, ObjectLockRetentionMode.GOVERNANCE, until, false);
        line(out, "  GOVERNANCE is softer. Delete that version: " + Store.outcome(() -> root.deleteVersion(BACKUPS, "backup-soft.tar", soft, false)));
        line(out, "  with the special bypass permission it can be lifted: " + Store.outcome(() -> root.deleteVersion(BACKUPS, "backup-soft.tar", soft, true)));
        line(out, "  COMPLIANCE cannot be lifted by anyone until the date, so choose it with care");
    }

    private static void act3WriteOnlyIdentity(StringBuilder out, Store root, ObjectStoreContainer container) throws IOException, InterruptedException {
        line(out, "THREE. The backup job gets its own identity that can only add objects.");
        container.createWriteOnlyUser(WRITER, WRITER_SECRET, BACKUPS);
        try (Store writer = new Store(container.endpoint(), WRITER, WRITER_SECRET)) {
            line(out, "  the backup job writes a backup: " + Store.outcome(() -> writer.put(BACKUPS, "nightly.tar", Store.bytes("nightly"))));
            line(out, "  it tries to read one back: " + Store.outcome(() -> writer.get(BACKUPS, "nightly.tar")));
            line(out, "  it tries to list versions: " + Store.outcome(() -> writer.versionIds(BACKUPS, "nightly.tar")));
            line(out, "  it tries to delete: " + Store.outcome(() -> writer.delete(BACKUPS, "nightly.tar")));
            String version = root.versionIds(BACKUPS, "nightly.tar").get(0);
            line(out, "  it tries to delete a version: " + Store.outcome(() -> writer.deleteVersion(BACKUPS, "nightly.tar", version, false)));
            line(out, "  it tries to write to the orders bucket: " + Store.outcome(() -> writer.put(ORDERS, "x", Store.bytes("x"))));
        }
        line(out, "  so a thief who steals the backup job's key can add data but cannot read, change history or destroy backups");
        line(out, "  it can still add junk, so the quota and the alerts on unusual uploads matter");
    }

    private static void act4Checksums(StringBuilder out, Store root) {
        line(out, "FOUR. A checksum catches a backup damaged on the way.");
        byte[] data = Store.bytes("backup bytes that must arrive intact");
        line(out, "  upload with the SHA-256 of the data: " + Store.outcome(() -> root.put(BACKUPS, "checked.tar", data)));
        byte[] other = Store.bytes("different bytes");
        line(out, "  upload bytes whose SHA-256 does not match the one sent: " + Store.outcome(() -> root.putWithChecksumOf(BACKUPS, "damaged.tar", data, other)));
        line(out, "  what was stored for the good upload hashes to the same value: "
                + Store.sha256Base64(root.get(BACKUPS, "checked.tar")).equals(Store.sha256Base64(data)));
        line(out, "  the damaged upload left nothing behind: " + root.versionIds(BACKUPS, "damaged.tar").isEmpty());
        line(out, "  the store checks bytes in transit and at upload; a restore test is still needed to know the backup is usable");
    }

    private static void act5LifecycleAndCost(StringBuilder out, Store root) {
        line(out, "FIVE. Keeping history without keeping it for ever, and the cost.");
        root.expireOldVersionsAfter(ORDERS, 30);
        var rules = root.lifecycleRules(ORDERS);
        line(out, "  a lifecycle rule on the orders bucket: " + rules.size() + " rule, " + rules.get(0).id()
                + ", removes old versions " + rules.get(0).noncurrentVersionExpiration().noncurrentDays() + " days after they are replaced");
        line(out, "  every version is stored in full, so storage grows with every overwrite until the rule removes them");
        line(out, "  a COMPLIANCE lock cannot be undone, so a mistaken retention date is paid for in storage");
        line(out, "  the root account is the real key: whoever holds it can change policies and users, so keep it out of daily use");
        line(out, "  object lock stops the API from deleting backups; it does not stop someone from deleting the disks, so keep a copy somewhere else too");
    }
}
