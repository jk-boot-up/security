package com.jk.explore.security.ransomres;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.ObjectVersion;

/** A bank's ledger backups against ransomware: what an attacker with the backup credentials can destroy, and what an immutable, isolated copy survives. */
public class RansomwareResilienceDemo {

    static final int N = 24;
    static final String ROOT = "bank-root";
    static final String ROOT_SECRET = "demo-only-root-secret-1";
    static final String VAULT_ROOT = "vault-root";
    static final String VAULT_ROOT_SECRET = "demo-only-vault-secret-1";
    static final Path WORK = Path.of("build", "work");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A labelled value, four spaces in, the label padded so that the values line up. */
    private static void row(StringBuilder out, int width, String label, String value) {
        line(out, String.format("    %-" + width + "s %s", label, value).stripTrailing());
    }

    private static String of(int n, int total) {
        return n + " of " + total;
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        deleteTree(WORK);
        Files.createDirectories(WORK);
        SecureRandom random = new SecureRandom();
        Map<String, byte[]> snapshots = new TreeMap<>();
        Map<String, String> manifest = new TreeMap<>();
        for (int i = 0; i < N; i++) {
            String key = String.format("ledger-%02d.csv", i);
            snapshots.put(key, Ledger.snapshot(i));
            manifest.put(key, Sha.hex(snapshots.get(key)));
        }
        List<String> keys = new ArrayList<>(snapshots.keySet());
        long snapshotBytes = snapshots.values().stream().mapToLong(b -> b.length).sum();
        long expectedTotal = snapshots.values().stream().mapToLong(Ledger::total).sum();
        int expectedRows = snapshots.values().stream().mapToInt(Ledger::rowCount).sum();

        try (Store primary = new Store("primary", ROOT, ROOT_SECRET); Store second = new Store("vault", VAULT_ROOT, VAULT_ROOT_SECRET)) {
            S3Client root = primary.admin();
            primary.createUser("svc-backup", "demo-only-svc-secret-1", "svc-everything", Policies.everything());
            S3Client svc = primary.client("svc-backup", "demo-only-svc-secret-1");

            // ---- act 1
            line(out, "ONE. A plain bucket: no versioning, no lock.");
            line(out, "  THE SETUP:");
            row(out, 9, "store", Store.IMAGE + " in Docker (an S3 API)");
            row(out, 9, "backups", N + " ledger snapshots, " + Ledger.ROWS_PER_SNAPSHOT + " rows each, " + snapshotBytes + " bytes");
            row(out, 9, "manifest", "one SHA-256 per backup, kept outside the store");
            row(out, 9, "example", "ledger-00.csv " + snapshots.get("ledger-00.csv").length + " bytes, SHA-256 " + manifest.get("ledger-00.csv").substring(0, 12) + "...");
            row(out, 9, "account", "svc-backup, allowed s3:* on every bucket");
            Bucket.plain(root, "ledger-plain");
            for (String k : keys) {
                Bucket.put(svc, "ledger-plain", k, snapshots.get(k));
            }
            line(out, "  BEFORE THE ATTACK:");
            row(out, 26, "backups that restore", of(Restore.countGood(svc, "ledger-plain", keys, manifest, false), N) + " (SHA-256 matches)");
            List<String> overwrite = new ArrayList<>();
            for (String k : keys) {
                byte[] garbage = randomBytes(random, snapshots.get(k).length);
                overwrite.add(Bucket.attempt(() -> Bucket.put(svc, "ledger-plain", k, garbage)));
            }
            line(out, "  THE ATTACK, WITH THE BACKUP CREDENTIALS:");
            row(out, 26, "overwrite, random bytes", "accepted " + of(Bucket.count(overwrite, "ok"), N));
            line(out, "  AFTER THE OVERWRITE:");
            row(out, 26, "versions kept per backup", String.valueOf(Bucket.versions(svc, "ledger-plain").size() / N));
            int plainGood = Restore.countGood(svc, "ledger-plain", keys, manifest, false);
            row(out, 26, "backups that restore", of(plainGood, N) + "  <- LOST: " + of(N - plainGood, N));
            List<String> deletes = new ArrayList<>();
            for (String k : keys) {
                deletes.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-plain").key(k))));
            }
            line(out, "  THE DELETE, WITH THE SAME CREDENTIALS:");
            row(out, 26, "delete every key", "accepted " + of(Bucket.count(deletes, "ok"), N));
            row(out, 26, "objects left", String.valueOf(Bucket.versions(svc, "ledger-plain").size()));

            // ---- act 2
            line(out, "TWO. Versioning only.");
            line(out, "  THE SETUP: versioning on, the same account (s3:* everywhere)");
            Bucket.versioned(root, "ledger-versioned");
            for (String k : keys) {
                Bucket.put(svc, "ledger-versioned", k, snapshots.get(k));
            }
            overwrite.clear();
            for (String k : keys) {
                byte[] garbage = randomBytes(random, snapshots.get(k).length);
                overwrite.add(Bucket.attempt(() -> Bucket.put(svc, "ledger-versioned", k, garbage)));
            }
            line(out, "  THE OVERWRITE:");
            int afterOverwrite = Bucket.versions(svc, "ledger-versioned").size();
            row(out, 26, "overwrite every key", "accepted " + of(Bucket.count(overwrite, "ok"), N));
            row(out, 26, "versions in the bucket", afterOverwrite + " (" + N + " good + " + (afterOverwrite - N) + " garbage)");
            line(out, "  THE RESTORE:");
            row(out, 26, "take the latest version", of(Restore.countGood(svc, "ledger-versioned", keys, manifest, false), N) + " good");
            row(out, 26, "look through the versions", of(Restore.countGood(svc, "ledger-versioned", keys, manifest, true), N) + " good  <- VERSIONS SAVE IT");
            deletes.clear();
            for (String k : keys) {
                deletes.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-versioned").key(k))));
            }
            line(out, "  THE DELETE BY KEY:");
            row(out, 26, "delete every key", "accepted " + of(Bucket.count(deletes, "ok"), N));
            row(out, 26, "delete markers added", String.valueOf(Bucket.deleteMarkers(svc, "ledger-versioned")));
            row(out, 26, "versions still there", String.valueOf(Bucket.versions(svc, "ledger-versioned").size()));
            List<String> permanent = new ArrayList<>();
            for (ObjectVersion v : Bucket.versions(svc, "ledger-versioned")) {
                permanent.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-versioned").key(v.key()).versionId(v.versionId()))));
            }
            line(out, "  THE DELETE BY VERSION ID (permanent):");
            row(out, 26, "delete every version", "accepted " + of(Bucket.count(permanent, "ok"), permanent.size()));
            row(out, 26, "versions left", String.valueOf(Bucket.versions(svc, "ledger-versioned").size()));
            row(out, 26, "backups that restore", of(Restore.countGood(svc, "ledger-versioned", keys, manifest, true), N) + "  <- ALL LOST");

            // ---- act 3
            line(out, "THREE. Object lock in COMPLIANCE mode.");
            Bucket.locked(root, "ledger-locked", 1);
            for (String k : keys) {
                Bucket.put(svc, "ledger-locked", k, snapshots.get(k));
            }
            ObjectVersion first = Bucket.versions(svc, "ledger-locked", "ledger-00.csv").get(0);
            line(out, "  THE SETUP:");
            row(out, 24, "default retention", "COMPLIANCE, 1 day (30+ in production)");
            row(out, 24, "mode of a stored backup",
                    root.getObjectRetention(b -> b.bucket("ledger-locked").key(first.key()).versionId(first.versionId())).retention().modeAsString());
            List<ObjectVersion> originals = Bucket.versions(svc, "ledger-locked");
            List<String> r = new ArrayList<>();
            for (ObjectVersion v : originals) {
                r.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-locked").key(v.key()).versionId(v.versionId()))));
            }
            line(out, "  THE ATTACK: delete each of the " + originals.size() + " versions by its id");
            row(out, 26, "backup credentials", "refused " + of(Bucket.count(r, "AccessDenied"), originals.size()) + "  AccessDenied");
            r.clear();
            for (ObjectVersion v : originals) {
                r.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-locked").key(v.key()).versionId(v.versionId()).bypassGovernanceRetention(true))));
            }
            row(out, 26, "with governance bypass", "refused " + of(Bucket.count(r, "AccessDenied"), originals.size()) + "  AccessDenied");
            r.clear();
            for (ObjectVersion v : originals) {
                r.add(Bucket.attempt(() -> root.deleteObject(b -> b.bucket("ledger-locked").key(v.key()).versionId(v.versionId()).bypassGovernanceRetention(true))));
            }
            row(out, 26, "root administrator", "refused " + of(Bucket.count(r, "AccessDenied"), originals.size()) + "  AccessDenied");
            overwrite.clear();
            for (String k : keys) {
                byte[] garbage = randomBytes(random, snapshots.get(k).length);
                overwrite.add(Bucket.attempt(() -> Bucket.put(svc, "ledger-locked", k, garbage)));
            }
            line(out, "  THE OVERWRITE BY KEY:");
            row(out, 26, "overwrite every key", "accepted " + of(Bucket.count(overwrite, "ok"), N) + " as new versions");
            List<ObjectVersion> all = Bucket.versions(svc, "ledger-locked");
            r.clear();
            for (ObjectVersion v : all) {
                r.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-locked").key(v.key()).versionId(v.versionId()))));
            }
            row(out, 26, "delete all " + all.size() + " versions", "refused " + of(Bucket.count(r, "AccessDenied"), all.size()) + " (garbage too)");
            line(out, "  THE RESTORE:");
            row(out, 26, "take the latest version", of(Restore.countGood(svc, "ledger-locked", keys, manifest, false), N) + " good");
            row(out, 26, "look through the versions", of(Restore.countGood(svc, "ledger-locked", keys, manifest, true), N) + " good  <- STILL THERE");
            String shorten = Bucket.attempt(() -> root.putObjectRetention(b -> b.bucket("ledger-locked").key(first.key()).versionId(first.versionId())
                    .retention(x -> x.mode(ObjectLockRetentionMode.COMPLIANCE).retainUntilDate(Instant.now().plus(1, ChronoUnit.HOURS)))));
            String toGovernance = Bucket.attempt(() -> root.putObjectRetention(b -> b.bucket("ledger-locked").key(first.key()).versionId(first.versionId())
                    .retention(x -> x.mode(ObjectLockRetentionMode.GOVERNANCE).retainUntilDate(Instant.now().plus(5, ChronoUnit.DAYS))).bypassGovernanceRetention(true)));
            String suspend = Bucket.attempt(() -> root.putBucketVersioning(b -> b.bucket("ledger-locked").versioningConfiguration(v -> v.status(BucketVersioningStatus.SUSPENDED))));
            String dropBucket = Bucket.attempt(() -> root.deleteBucket(b -> b.bucket("ledger-locked")));
            String extend = Bucket.attempt(() -> root.putObjectRetention(b -> b.bucket("ledger-locked").key(first.key()).versionId(first.versionId())
                    .retention(x -> x.mode(ObjectLockRetentionMode.COMPLIANCE).retainUntilDate(Instant.now().plus(3, ChronoUnit.DAYS)))));
            line(out, "  ROOT TRIES TO LOOSEN THE LOCK:");
            row(out, 26, "shorten the retention", shorten);
            row(out, 26, "COMPLIANCE to GOVERNANCE", toGovernance);
            row(out, 26, "suspend versioning", suspend);
            row(out, 26, "delete the bucket", dropBucket);
            row(out, 26, "extend the retention", extend + " (a lock can only get longer)");
            String dropConfig = Bucket.attempt(() -> root.putObjectLockConfiguration(b -> b.bucket("ledger-locked").objectLockConfiguration(o -> o.objectLockEnabled(ObjectLockEnabled.ENABLED))));
            r.clear();
            for (ObjectVersion v : originals) {
                r.add(Bucket.attempt(() -> root.deleteObject(b -> b.bucket("ledger-locked").key(v.key()).versionId(v.versionId()))));
            }
            Bucket.put(svc, "ledger-locked", "after-the-rule.csv", Ledger.snapshot(90));
            ObjectVersion late = Bucket.versions(svc, "ledger-locked", "after-the-rule.csv").get(0);
            String lateDelete = Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-locked").key(late.key()).versionId(late.versionId())));
            svc.putObject(b -> b.bucket("ledger-locked").key("with-own-lock.csv").objectLockMode(ObjectLockMode.COMPLIANCE).objectLockRetainUntilDate(Instant.now().plus(2, ChronoUnit.DAYS)),
                    RequestBody.fromBytes(Ledger.snapshot(91)));
            ObjectVersion own = Bucket.versions(svc, "ledger-locked", "with-own-lock.csv").get(0);
            String ownDelete = Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-locked").key(own.key()).versionId(own.versionId())));
            line(out, "  THE GAP: root removes the default retention rule: " + dropConfig);
            row(out, 26, "the " + originals.size() + " stored versions", "still refused " + of(Bucket.count(r, "AccessDenied"), originals.size()));
            row(out, 26, "a backup written after it", "delete: " + lateDelete + "  <- NOT LOCKED");
            row(out, 26, "one with its own lock", "delete: " + ownDelete);

            // ---- act 4
            line(out, "FOUR. Isolation: separate credentials, and a second store.");
            Bucket.locked(root, "ledger-vault", 1);
            primary.createUser("backup-writer", "demo-only-writer-secret-1", "writer-only", Policies.writerOnly("ledger-vault"));
            primary.createUser("recovery", "demo-only-recovery-secret-1", "reader-only", Policies.readerOnly("ledger-vault"));
            S3Client writer = primary.client("backup-writer", "demo-only-writer-secret-1");
            S3Client reader = primary.client("recovery", "demo-only-recovery-secret-1");
            line(out, "  THE POLICIES ON ledger-vault (locked, COMPLIANCE):");
            row(out, 13, "backup-writer", String.join(" ", Policies.WRITER));
            row(out, 13, "recovery", String.join(" ", Policies.READER.subList(0, 2)));
            row(out, 13, "", String.join(" ", Policies.READER.subList(2, Policies.READER.size())));
            for (String k : keys) {
                Bucket.put(writer, "ledger-vault", k, snapshots.get(k));
            }
            ObjectVersion v0 = root.listObjectVersions(b -> b.bucket("ledger-vault").prefix("ledger-00.csv")).versions().get(0);
            Map<String, String> writerTries = new LinkedHashMap<>();
            writerTries.put("delete a version", Bucket.attempt(() -> writer.deleteObject(b -> b.bucket("ledger-vault").key(v0.key()).versionId(v0.versionId()))));
            writerTries.put("delete a key", Bucket.attempt(() -> writer.deleteObject(b -> b.bucket("ledger-vault").key(v0.key()))));
            writerTries.put("read a backup", Bucket.attempt(() -> writer.getObjectAsBytes(b -> b.bucket("ledger-vault").key(v0.key()))));
            writerTries.put("change a retention", Bucket.attempt(() -> writer.putObjectRetention(b -> b.bucket("ledger-vault").key(v0.key()).versionId(v0.versionId())
                    .retention(x -> x.mode(ObjectLockRetentionMode.COMPLIANCE).retainUntilDate(Instant.now().plus(1, ChronoUnit.HOURS))))));
            writerTries.put("place a legal hold", Bucket.attempt(() -> writer.putObjectLegalHold(b -> b.bucket("ledger-vault").key(v0.key()).legalHold(h -> h.status(ObjectLockLegalHoldStatus.OFF)))));
            writerTries.put("delete the bucket", Bucket.attempt(() -> writer.deleteBucket(b -> b.bucket("ledger-vault"))));
            writerTries.put("touch another bucket", Bucket.attempt(() -> Bucket.put(writer, "ledger-plain", "x", new byte[] {1})));
            Map<String, String> recoveryTries = new LinkedHashMap<>();
            recoveryTries.put("read a backup", Bucket.attempt(() -> reader.getObjectAsBytes(b -> b.bucket("ledger-vault").key(v0.key()).versionId(v0.versionId()))));
            recoveryTries.put("list the versions", Bucket.attempt(() -> reader.listObjectVersions(b -> b.bucket("ledger-vault"))));
            recoveryTries.put("write a backup", Bucket.attempt(() -> Bucket.put(reader, "ledger-vault", "x", new byte[] {1})));
            recoveryTries.put("delete a version", Bucket.attempt(() -> reader.deleteObject(b -> b.bucket("ledger-vault").key(v0.key()).versionId(v0.versionId()))));
            int writerRefused = (int) writerTries.values().stream().filter("AccessDenied"::equals).count();
            line(out, "  THE WRITER TRIES (it stored " + N + " backups: ok):");
            writerTries.forEach((what, code) -> row(out, 22, what, code));
            row(out, 22, "refused", of(writerRefused, writerTries.size()) + "  <- IT CAN ONLY ADD");
            line(out, "  THE RECOVERY ACCOUNT TRIES:");
            recoveryTries.forEach((what, code) -> row(out, 22, what, code));
            String overwriteByWriter = Bucket.attempt(() -> Bucket.put(writer, "ledger-vault", "probe-overwrite.csv", snapshots.get("ledger-01.csv")));
            String overwriteAgain = Bucket.attempt(() -> Bucket.put(writer, "ledger-vault", "probe-overwrite.csv", new byte[64]));
            line(out, "  WHAT A POLICY CANNOT STOP:");
            row(out, 22, "overwrite its own key", overwriteByWriter + ", then " + overwriteAgain + "; versions kept: "
                    + Bucket.versions(root, "ledger-vault", "probe-overwrite.csv").size());
            row(out, 22, "vault backups restore", of(Restore.countGood(reader, "ledger-vault", keys, manifest, true), N) + " by version");

            second.createUser("copier", "demo-only-copier-secret-1", "copy-in", Policies.writerOnly("airgap"));
            second.createUser("recovery2", "demo-only-recovery-secret-2", "read-out", Policies.readerOnly("airgap"));
            Bucket.locked(second.admin(), "airgap", 1);
            S3Client copier = second.client("copier", "demo-only-copier-secret-1");
            int copied = 0;
            for (String k : keys) {
                for (ObjectVersion v : Bucket.versions(reader, "ledger-vault", k)) {
                    byte[] data = Bucket.get(reader, "ledger-vault", k, v.versionId());
                    if (Sha.hex(data).equals(manifest.get(k))) {
                        Bucket.put(copier, "airgap", k, data);
                        copied++;
                        break;
                    }
                }
            }
            S3Client reader2 = second.client("recovery2", "demo-only-recovery-secret-2");
            line(out, "  THE SECOND STORE (the air-gapped copy, its own credentials):");
            row(out, 22, "checked and copied", of(copied, N) + " (SHA-256 against the manifest)");
            row(out, 22, "restorable from it", of(Restore.countGood(reader2, "airgap", keys, manifest, true), N));
            S3Client intruder = second.client(ROOT, ROOT_SECRET);
            S3Client intruder2 = second.client("backup-writer", "demo-only-writer-secret-1");
            String rootThere = Bucket.attempt(() -> intruder.listBuckets());
            String writerThere = Bucket.attempt(() -> intruder2.listBuckets());
            second.disconnect();
            S3Client impatient = second.impatientClient(VAULT_ROOT, VAULT_ROOT_SECRET);
            String whileOff = Bucket.attempt(() -> impatient.listBuckets());
            second.reconnect();
            line(out, "  THE ATTACKER ASKS THE SECOND STORE:");
            row(out, 22, "with the first root", rootThere);
            row(out, 22, "with the writer keys", writerThere);
            row(out, 22, "while disconnected", whileOff + " (even for its own root)");

            // ---- act 5
            line(out, "FIVE. The guard: a canary file and entropy.");
            Guard guard = new Guard(Ledger.canary());
            byte[] textBlock = Arrays.copyOf(Ledger.dayFile(1), Entropy.BLOCK);
            EntropySteps.Worked plainText = EntropySteps.of(textBlock);
            EntropySteps.Worked cipherText = EntropySteps.of(EntropySteps.encryptedForExample(textBlock));
            line(out, "  HOW ENTROPY IS WORKED OUT (one block of " + Entropy.BLOCK + " bytes):");
            line(out, "    H = sum over the byte values of -p x log2(p), p = count / " + Entropy.BLOCK);
            entropy(out, "THE LEDGER TEXT, BEFORE ENCRYPTION:", plainText);
            entropy(out, "THE SAME BLOCK, AFTER AES ENCRYPTION:", cipherText);
            line(out, "  THE DETECTORS (each one alone stops the backup):");
            row(out, 7, "canary", "the SHA-256 of " + Guard.CANARY_NAME + " changed");
            row(out, 7, "file", "the entropy of a whole file is above " + Entropy.LIMIT);
            row(out, 7, "block", "the entropy of any 4 KiB block is above " + Entropy.LIMIT);
            Path lab = WORK.resolve("lab");
            record Variant(String name, boolean full, boolean skipCanary) {
            }
            line(out, "  THE DIRECTORIES TESTED (9 files: 8 days + the canary):");
            line(out, String.format("    %-28s %3s  %-6s %-4s %-5s %s", "", "enc", "canary", "file", "block", "backup").stripTrailing());
            for (Variant v : List.of(new Variant("healthy", false, false), new Variant("AES, every file", true, false), new Variant("AES, canary skipped", true, true),
                    new Variant("AES on half, canary skipped", false, true))) {
                Path dir = lab.resolve("v" + Math.abs(v.name().hashCode()));
                fill(dir);
                int touched = 0;
                if (!v.name().startsWith("healthy")) {
                    Set<String> skip = v.skipCanary() ? Set.of(Guard.CANARY_NAME) : Set.of();
                    touched = v.full() ? Ransomware.encryptAndDelete(dir, skip) : Ransomware.encryptHalfInPlace(dir, skip);
                }
                verdictRow(out, v.name(), touched, guard.check(dir), "");
            }
            Path gz = lab.resolve("gz");
            fill(gz);
            ByteArrayOutputStream zipped = new ByteArrayOutputStream();
            try (GZIPOutputStream z = new GZIPOutputStream(zipped)) {
                z.write(Ledger.dayFile(40));
            }
            Files.write(gz.resolve("export-2026-10-09.csv.gz"), zipped.toByteArray());
            verdictRow(out, "healthy + compressed export", 0, guard.check(gz), "");
            line(out, "    a false alarm: compressed data looks like ciphertext");

            // ---- act 6
            line(out, "SIX. Ransomware and the poisoned backup.");
            primary.createUser("run-writer", "demo-only-runwriter-secret-1", "run-writer", Policies.writerOnly("runs-*"));
            primary.createUser("run-reader", "demo-only-runreader-secret-1", "run-reader", Policies.readerOnly("runs-*"));
            S3Client runWriter = primary.client("run-writer", "demo-only-runwriter-secret-1");
            S3Client runReader = primary.client("run-reader", "demo-only-runreader-secret-1");
            Bucket.locked(root, "runs-unguarded", 1);
            Bucket.locked(root, "runs-guarded", 1);
            Path data = WORK.resolve("ledger-data");
            fill(data);
            Map<String, String> before = hashes(data);
            BackupJob unguarded = new BackupJob(runWriter, "runs-unguarded", guard);
            BackupJob guarded = new BackupJob(runWriter, "runs-guarded", guard);
            int[] goodRuns = {6 * 60, 7 * 60, 8 * 60, 9 * 60};
            int written = 0;
            for (int minute : goodRuns) {
                String id = "run-" + Recovery.clock(minute).replace(":", "");
                written += unguarded.run(id, data, false).objects();
                guarded.run(id, data, true);
            }
            line(out, "  BEFORE THE INFECTION: hourly backups, two locked buckets");
            row(out, 16, "runs", Recovery.clock(goodRuns[0]) + " to " + Recovery.clock(goodRuns[goodRuns.length - 1]) + ", " + goodRuns.length + " in each bucket");
            row(out, 16, "objects written", written + " = " + goodRuns.length + " runs x (" + before.size() + " files + 1 MANIFEST)");
            int infection = 9 * 60 + 20;
            int encrypted = Ransomware.encryptAndDelete(data, Set.of());
            line(out, "  THE INFECTION AT " + Recovery.clock(infection) + ":");
            row(out, 16, "encrypted", encrypted + " files with AES-256-GCM, originals deleted");
            row(out, 16, "files now", Ransomware.files(data).size() + " (" + encrypted + " .locked + " + Ransomware.NOTE + ")");
            BackupJob.Result poisoned = unguarded.run("run-1000", data, false);
            BackupJob.Result refused = guarded.run("run-1000", data, true);
            line(out, "  THE 10:00 BACKUP:");
            row(out, 16, "without guard", "wrote " + poisoned.objects() + " objects (" + (poisoned.objects() - 1) + " files + 1 MANIFEST)");
            row(out, 16, "with the guard", (refused.refused() ? "refused" : "wrote") + ", wrote " + refused.objects() + " objects");
            row(out, 16, "", refused.verdict().detectors());
            Map<String, String> poisonedManifest = Restore.manifest(runReader, "runs-unguarded", "run-1000");
            int sumOk = 0;
            int contentOk = 0;
            for (Map.Entry<String, String> e : poisonedManifest.entrySet()) {
                byte[] bytes = Bucket.getLatest(runReader, "runs-unguarded", "run-1000/" + e.getKey());
                sumOk += Sha.hex(bytes).equals(e.getValue()) ? 1 : 0;
                contentOk += Guard.looksLikeLedger(bytes) ? 1 : 0;
            }
            r.clear();
            for (ObjectVersion v : Bucket.versions(root, "runs-unguarded")) {
                if (v.key().startsWith("run-1000/")) {
                    r.add(Bucket.attempt(() -> root.deleteObject(b -> b.bucket("runs-unguarded").key(v.key()).versionId(v.versionId()))));
                }
            }
            line(out, "  THE POISONED BACKUP, CHECKED:");
            row(out, 20, "SHA-256 vs manifest", "matches " + of(sumOk, poisonedManifest.size()) + "  <- LOOKS FINE");
            row(out, 20, "looks like a ledger", of(contentOk, poisonedManifest.size()) + " (only the short ransom note)");
            row(out, 20, "why", "the manifest was made from encrypted files");
            row(out, 20, "delete to save space", "refused " + of(Bucket.count(r, "AccessDenied"), r.size()) + " (the lock keeps it too)");
            Restore.RunChoice cu = Restore.newestGoodRun(runReader, "runs-unguarded", WORK.resolve("restore-unguarded"));
            Restore.RunChoice cg = Restore.newestGoodRun(runReader, "runs-guarded", WORK.resolve("restore-guarded"));
            Map<String, String> restoredHashes = hashes(WORK.resolve("restore-unguarded"));
            restoredHashes.remove("MANIFEST");
            int same = (int) before.entrySet().stream().filter(e -> e.getValue().equals(restoredHashes.get(e.getKey()))).count();
            line(out, "  THE RESTORE: the newest run that passes both checks");
            choice(out, "unguarded", cu);
            choice(out, "guarded", cg);
            row(out, 10, "result", "identical to the directory at 09:00: " + of(same, before.size()) + " files");
            int cutOff = 10 * 60 + 40;
            int lastGood = Integer.parseInt(cu.runId().substring(4, 6)) * 60 + Integer.parseInt(cu.runId().substring(6, 8));
            line(out, "  HOW THE DATA LOST IS WORKED OUT (" + Recovery.TRANSACTIONS_PER_HOUR + " transactions an hour):");
            line(out, "    last good backup " + Recovery.clock(lastGood) + ", systems cut off " + Recovery.clock(cutOff));
            line(out, "    " + (cutOff - lastGood) + " min x " + Recovery.TRANSACTIONS_PER_HOUR + " / 60 = " + Recovery.lostTransactions(lastGood, cutOff) + " transactions lost");
            for (int every : new int[] {1440, 240, 60, 15}) {
                int last = Recovery.lastGoodBackup(infection, every);
                line(out, String.format("    backup every %4d min  last good %s  lost %5d", every, Recovery.clock(last), Recovery.lostTransactions(last, cutOff)));
            }

            // ---- act 7
            line(out, "SEVEN. The restore exercise.");
            Path fresh = WORK.resolve("restore-primary");
            Restore.Outcome a = Restore.toDirectory(reader, "ledger-vault", keys, manifest, fresh);
            line(out, "  RESTORE 1, FROM THE FIRST STORE INTO A FRESH DIRECTORY:");
            outcome(out, a, expectedTotal, expectedRows);
            report(out, a, 400);
            primary.close();
            S3Client gone = primary.impatientClient(ROOT, ROOT_SECRET);
            line(out, "  THE FIRST STORE IS DESTROYED (host lost): " + Bucket.attempt(() -> gone.listBuckets()));
            Restore.Outcome b = Restore.toDirectory(reader2, "airgap", keys, manifest, WORK.resolve("restore-airgap"));
            line(out, "  RESTORE 2, FROM THE AIR-GAPPED COPY:");
            outcome(out, b, expectedTotal, expectedRows);
            report(out, b, 100);
            line(out, "  NOT COVERED BY THE LOCK:");
            line(out, "    root on the host of a store (why a second store exists)");
            line(out, "    a retention shorter than the time an attacker stays unseen");
            line(out, "    data already encrypted when the oldest backup was made");
        } finally {
            deleteTree(WORK.resolve("lab"));
        }
        return out.toString();
    }

    private static byte[] randomBytes(SecureRandom random, int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /** The three largest terms of an entropy sum, how many values there are, and the total. */
    private static void entropy(StringBuilder out, String title, EntropySteps.Worked w) {
        line(out, "  " + title);
        for (EntropySteps.Term t : w.terms().subList(0, 3)) {
            line(out, String.format(Locale.ROOT, "    %-6s count %4d  p %.4f  -p x log2(p) %.4f", EntropySteps.show(t.value()), t.count(), t.p(), t.bits()));
        }
        line(out, String.format(Locale.ROOT, "    ... %d byte values in all, H = %.2f bits per byte", w.distinct(), w.total()));
    }

    private static void verdictRow(StringBuilder out, String name, int encrypted, Guard.Verdict v, String note) {
        line(out, String.format("    %-28s %3d  %-6s %-4s %-5s %s%s", name, encrypted, Guard.yesNo(v.canaryChanged()), Guard.yesNo(v.fileEntropyHigh()),
                Guard.yesNo(v.blockEntropyHigh()), v.clean() ? "allowed" : "refused", note));
    }

    private static void choice(StringBuilder out, String bucket, Restore.RunChoice c) {
        row(out, 10, bucket, "runs seen " + c.runsSeen() + ", failed: sum " + c.failedChecksum() + ", content " + c.failedContent() + "; chose " + c.runId());
    }

    private static void outcome(StringBuilder out, Restore.Outcome o, long expectedTotal, int expectedRows) {
        row(out, 14, "backups good", of(o.good(), N) + ", " + o.bytes() + " bytes, " + o.rows() + " rows");
        row(out, 14, "ledger total", "matches: " + Guard.yesNo(o.ledgerTotal() == expectedTotal && o.rows() == expectedRows));
        row(out, 14, "steps", o.steps() + " = " + o.good() + " x 5 (list, get, write, hash, compare)");
    }

    private static String mins(long seconds) {
        return String.format("%9s", Recovery.minutes(seconds));
    }

    /** The recovery time, worked out line by line from the steps and backups that the restore really took. */
    private static void report(StringBuilder out, Restore.Outcome o, long mibPerSecond) {
        long seconds = Recovery.rtoSeconds(o.good(), o.steps(), mibPerSecond);
        long mib = o.good() * (Recovery.MODELLED_BYTES_PER_BACKUP / (1024L * 1024));
        line(out, "  HOW THE RECOVERY TIME IS WORKED OUT (a model, not a stopwatch):");
        row(out, 40, "declare the incident", mins(Recovery.DECLARE_SECONDS));
        row(out, 40, "build a clean environment", mins(Recovery.PROVISION_SECONDS));
        row(out, 40, o.steps() + " steps x " + Recovery.SECONDS_PER_STEP + " s = " + o.steps() * Recovery.SECONDS_PER_STEP + " s",
                mins(o.steps() * Recovery.SECONDS_PER_STEP));
        row(out, 40, o.good() + " x " + Recovery.MODELLED_BYTES_PER_BACKUP / (1024L * 1024 * 1024) + " GiB = " + mib + " MiB", "");
        row(out, 40, "  at " + mibPerSecond + " MiB/s = " + mib / mibPerSecond + " s", mins(mib / mibPerSecond));
        row(out, 40, "total, against a target of " + Recovery.TARGET_RTO_SECONDS / 60 + " min", mins(seconds)
                + (seconds <= Recovery.TARGET_RTO_SECONDS ? "  met" : "  MISSED"));
    }

    /** Fills a directory with eight days of ledger files and the canary. */
    static void fill(Path dir) throws IOException {
        deleteTree(dir);
        Files.createDirectories(dir);
        for (int day = 1; day <= 8; day++) {
            Files.write(dir.resolve(String.format("ledger-2026-10-%02d.csv", day)), Ledger.dayFile(day));
        }
        Files.write(dir.resolve(Guard.CANARY_NAME), Ledger.canary());
    }

    static Map<String, String> hashes(Path dir) throws IOException {
        Map<String, String> m = new TreeMap<>();
        for (Path p : Ransomware.files(dir)) {
            m.put(p.getFileName().toString(), Sha.hex(Files.readAllBytes(p)));
        }
        return m;
    }

    static void deleteTree(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var walk = Files.walk(dir)) {
                for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            }
        }
    }
}
