package com.jk.explore.security.ransomres;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;

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
            line(out, "== the store");
            line(out, "image: rustfs/rustfs (S3 API with versioning, object lock and policies); a second, separate instance plays the air-gapped vault");
            line(out, "ledger backups: " + N + " snapshots of " + Ledger.ROWS_PER_SNAPSHOT + " rows each, " + snapshotBytes + " bytes, SHA-256 manifest kept outside the buckets");

            primary.createUser("svc-backup", "demo-only-svc-secret-1", "svc-everything", Policies.everything());
            S3Client svc = primary.client("svc-backup", "demo-only-svc-secret-1");

            // ---- act 1
            line(out, "== 1. plain bucket, no versioning, no lock");
            Bucket.plain(root, "ledger-plain");
            for (String k : keys) {
                Bucket.put(svc, "ledger-plain", k, snapshots.get(k));
            }
            line(out, "before the attack: " + of(Restore.countGood(svc, "ledger-plain", keys, manifest, false), N) + " backups restore with a matching SHA-256");
            List<String> overwrite = new ArrayList<>();
            for (String k : keys) {
                byte[] noise = new byte[snapshots.get(k).length];
                random.nextBytes(noise);
                overwrite.add(Bucket.attempt(() -> Bucket.put(svc, "ledger-plain", k, noise)));
            }
            line(out, "attacker with the backup credentials overwrites every backup with random bytes: accepted " + of(Bucket.count(overwrite, "ok"), N)
                    + ", versions kept per backup: " + Bucket.versions(svc, "ledger-plain").size() / N);
            line(out, "backups that restore: " + of(Restore.countGood(svc, "ledger-plain", keys, manifest, false), N) + " (lost: " + of(N - Restore.countGood(svc, "ledger-plain", keys, manifest, false), N) + ")");
            List<String> deletes = new ArrayList<>();
            for (String k : keys) {
                deletes.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-plain").key(k))));
            }
            line(out, "attacker deletes them as well: accepted " + of(Bucket.count(deletes, "ok"), N) + ", objects left: " + Bucket.versions(svc, "ledger-plain").size());

            // ---- act 2
            line(out, "== 2. versioning only");
            Bucket.versioned(root, "ledger-versioned");
            for (String k : keys) {
                Bucket.put(svc, "ledger-versioned", k, snapshots.get(k));
            }
            overwrite.clear();
            for (String k : keys) {
                byte[] noise = new byte[snapshots.get(k).length];
                random.nextBytes(noise);
                overwrite.add(Bucket.attempt(() -> Bucket.put(svc, "ledger-versioned", k, noise)));
            }
            line(out, "overwrite of all " + N + " accepted " + of(Bucket.count(overwrite, "ok"), N) + ", versions in the bucket: " + Bucket.versions(svc, "ledger-versioned").size());
            line(out, "restore of the latest versions: " + of(Restore.countGood(svc, "ledger-versioned", keys, manifest, false), N) + " good; restore that looks through the versions: "
                    + of(Restore.countGood(svc, "ledger-versioned", keys, manifest, true), N) + " good");
            deletes.clear();
            for (String k : keys) {
                deletes.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-versioned").key(k))));
            }
            line(out, "delete of every key: accepted " + of(Bucket.count(deletes, "ok"), N) + " as " + Bucket.deleteMarkers(svc, "ledger-versioned") + " delete markers, versions still there: " + Bucket.versions(svc, "ledger-versioned").size());
            List<String> permanent = new ArrayList<>();
            for (ObjectVersion v : Bucket.versions(svc, "ledger-versioned")) {
                permanent.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-versioned").key(v.key()).versionId(v.versionId()))));
            }
            line(out, "permanent delete of every version by its version id: accepted " + of(Bucket.count(permanent, "ok"), permanent.size()) + ", versions left: " + Bucket.versions(svc, "ledger-versioned").size()
                    + ", backups that restore: " + of(Restore.countGood(svc, "ledger-versioned", keys, manifest, true), N));

            // ---- act 3
            line(out, "== 3. object lock in COMPLIANCE mode (1 day here, 30 days or more in production)");
            Bucket.locked(root, "ledger-locked", 1);
            for (String k : keys) {
                Bucket.put(svc, "ledger-locked", k, snapshots.get(k));
            }
            ObjectVersion first = Bucket.versions(svc, "ledger-locked", "ledger-00.csv").get(0);
            line(out, "retention mode of a stored backup: "
                    + root.getObjectRetention(b -> b.bucket("ledger-locked").key(first.key()).versionId(first.versionId())).retention().modeAsString());
            List<ObjectVersion> originals = Bucket.versions(svc, "ledger-locked");
            List<String> r = new ArrayList<>();
            for (ObjectVersion v : originals) {
                r.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-locked").key(v.key()).versionId(v.versionId()))));
            }
            line(out, "backup credentials delete a version by id: refused " + of(Bucket.count(r, "AccessDenied"), originals.size()) + " with AccessDenied");
            r.clear();
            for (ObjectVersion v : originals) {
                r.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-locked").key(v.key()).versionId(v.versionId()).bypassGovernanceRetention(true))));
            }
            line(out, "the same with the governance-bypass header: refused " + of(Bucket.count(r, "AccessDenied"), originals.size()) + " with AccessDenied");
            r.clear();
            for (ObjectVersion v : originals) {
                r.add(Bucket.attempt(() -> root.deleteObject(b -> b.bucket("ledger-locked").key(v.key()).versionId(v.versionId()).bypassGovernanceRetention(true))));
            }
            line(out, "the same with the root administrator credentials: refused " + of(Bucket.count(r, "AccessDenied"), originals.size()) + " with AccessDenied");
            overwrite.clear();
            for (String k : keys) {
                byte[] noise = new byte[snapshots.get(k).length];
                random.nextBytes(noise);
                overwrite.add(Bucket.attempt(() -> Bucket.put(svc, "ledger-locked", k, noise)));
            }
            line(out, "overwrite by key: accepted " + of(Bucket.count(overwrite, "ok"), N) + " as new versions, versions in the bucket: " + Bucket.versions(svc, "ledger-locked").size());
            line(out, "restore of the latest versions: " + of(Restore.countGood(svc, "ledger-locked", keys, manifest, false), N) + " good; restore by version: " + of(Restore.countGood(svc, "ledger-locked", keys, manifest, true), N) + " good");
            List<ObjectVersion> all = Bucket.versions(svc, "ledger-locked");
            r.clear();
            for (ObjectVersion v : all) {
                r.add(Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-locked").key(v.key()).versionId(v.versionId()))));
            }
            line(out, "delete of the attacker's own garbage versions as well: refused " + of(Bucket.count(r, "AccessDenied"), all.size()) + " (the default retention locks every new version)");
            String shorten = Bucket.attempt(() -> root.putObjectRetention(b -> b.bucket("ledger-locked").key(first.key()).versionId(first.versionId())
                    .retention(x -> x.mode(ObjectLockRetentionMode.COMPLIANCE).retainUntilDate(Instant.now().plus(1, ChronoUnit.HOURS)))));
            String toGovernance = Bucket.attempt(() -> root.putObjectRetention(b -> b.bucket("ledger-locked").key(first.key()).versionId(first.versionId())
                    .retention(x -> x.mode(ObjectLockRetentionMode.GOVERNANCE).retainUntilDate(Instant.now().plus(5, ChronoUnit.DAYS))).bypassGovernanceRetention(true)));
            String suspend = Bucket.attempt(() -> root.putBucketVersioning(b -> b.bucket("ledger-locked").versioningConfiguration(v -> v.status(BucketVersioningStatus.SUSPENDED))));
            String dropBucket = Bucket.attempt(() -> root.deleteBucket(b -> b.bucket("ledger-locked")));
            String extend = Bucket.attempt(() -> root.putObjectRetention(b -> b.bucket("ledger-locked").key(first.key()).versionId(first.versionId())
                    .retention(x -> x.mode(ObjectLockRetentionMode.COMPLIANCE).retainUntilDate(Instant.now().plus(3, ChronoUnit.DAYS)))));
            line(out, "root shortens the retention: " + shorten + "; changes COMPLIANCE to GOVERNANCE: " + toGovernance + "; suspends versioning: " + suspend + "; deletes the bucket: " + dropBucket);
            line(out, "root extends the retention: " + extend + " (a lock can only get longer)");
            String dropConfig = Bucket.attempt(() -> root.putObjectLockConfiguration(b -> b.bucket("ledger-locked").objectLockConfiguration(o -> o.objectLockEnabled(ObjectLockEnabled.ENABLED))));
            r.clear();
            for (ObjectVersion v : originals) {
                r.add(Bucket.attempt(() -> root.deleteObject(b -> b.bucket("ledger-locked").key(v.key()).versionId(v.versionId()))));
            }
            line(out, "root removes the default retention rule: " + dropConfig + "; the " + originals.size() + " stored versions are still refused: " + of(Bucket.count(r, "AccessDenied"), originals.size()));
            Bucket.put(svc, "ledger-locked", "after-the-rule.csv", Ledger.snapshot(90));
            ObjectVersion late = Bucket.versions(svc, "ledger-locked", "after-the-rule.csv").get(0);
            String lateDelete = Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-locked").key(late.key()).versionId(late.versionId())));
            svc.putObject(b -> b.bucket("ledger-locked").key("with-own-lock.csv").objectLockMode(ObjectLockMode.COMPLIANCE).objectLockRetainUntilDate(Instant.now().plus(2, ChronoUnit.DAYS)),
                    RequestBody.fromBytes(Ledger.snapshot(91)));
            ObjectVersion own = Bucket.versions(svc, "ledger-locked", "with-own-lock.csv").get(0);
            String ownDelete = Bucket.attempt(() -> svc.deleteObject(b -> b.bucket("ledger-locked").key(own.key()).versionId(own.versionId())));
            line(out, "a backup written after the rule was removed can be deleted: " + lateDelete + "; one written with its own retention header cannot: " + ownDelete);

            // ---- act 4
            line(out, "== 4. isolation: separate credentials, and a copy in a second store");
            Bucket.locked(root, "ledger-vault", 1);
            primary.createUser("backup-writer", "demo-only-writer-secret-1", "writer-only", Policies.writerOnly("ledger-vault"));
            primary.createUser("recovery", "demo-only-recovery-secret-1", "reader-only", Policies.readerOnly("ledger-vault"));
            S3Client writer = primary.client("backup-writer", "demo-only-writer-secret-1");
            S3Client reader = primary.client("recovery", "demo-only-recovery-secret-1");
            for (String k : keys) {
                Bucket.put(writer, "ledger-vault", k, snapshots.get(k));
            }
            ObjectVersion v0 = root.listObjectVersions(b -> b.bucket("ledger-vault").prefix("ledger-00.csv")).versions().get(0);
            line(out, "backup-writer stores " + N + " backups: ok");
            Map<String, String> attempts = new java.util.LinkedHashMap<>();
            attempts.put("writer deletes a version", Bucket.attempt(() -> writer.deleteObject(b -> b.bucket("ledger-vault").key(v0.key()).versionId(v0.versionId()))));
            attempts.put("writer deletes a key", Bucket.attempt(() -> writer.deleteObject(b -> b.bucket("ledger-vault").key(v0.key()))));
            attempts.put("writer reads a backup", Bucket.attempt(() -> writer.getObjectAsBytes(b -> b.bucket("ledger-vault").key(v0.key()))));
            attempts.put("writer changes a retention", Bucket.attempt(() -> writer.putObjectRetention(b -> b.bucket("ledger-vault").key(v0.key()).versionId(v0.versionId())
                    .retention(x -> x.mode(ObjectLockRetentionMode.COMPLIANCE).retainUntilDate(Instant.now().plus(1, ChronoUnit.HOURS))))));
            attempts.put("writer places a legal hold", Bucket.attempt(() -> writer.putObjectLegalHold(b -> b.bucket("ledger-vault").key(v0.key()).legalHold(h -> h.status(ObjectLockLegalHoldStatus.OFF)))));
            attempts.put("writer deletes the bucket", Bucket.attempt(() -> writer.deleteBucket(b -> b.bucket("ledger-vault"))));
            attempts.put("writer touches another bucket", Bucket.attempt(() -> Bucket.put(writer, "ledger-plain", "x", new byte[] {1})));
            attempts.put("recovery reads a backup", Bucket.attempt(() -> reader.getObjectAsBytes(b -> b.bucket("ledger-vault").key(v0.key()).versionId(v0.versionId()))));
            attempts.put("recovery lists the versions", Bucket.attempt(() -> reader.listObjectVersions(b -> b.bucket("ledger-vault"))));
            attempts.put("recovery writes a backup", Bucket.attempt(() -> Bucket.put(reader, "ledger-vault", "x", new byte[] {1})));
            attempts.put("recovery deletes a version", Bucket.attempt(() -> reader.deleteObject(b -> b.bucket("ledger-vault").key(v0.key()).versionId(v0.versionId()))));
            attempts.forEach((what, code) -> line(out, String.format("%-30s %s", what, code)));
            String overwriteByWriter = Bucket.attempt(() -> Bucket.put(writer, "ledger-vault", "probe-overwrite.csv", snapshots.get("ledger-01.csv")));
            String overwriteAgain = Bucket.attempt(() -> Bucket.put(writer, "ledger-vault", "probe-overwrite.csv", new byte[64]));
            line(out, "writer overwrites a key it wrote: " + overwriteByWriter + " then " + overwriteAgain + " (a policy cannot stop it; versions kept: " + Bucket.versions(root, "ledger-vault", "probe-overwrite.csv").size() + ")");
            line(out, "backups in the vault that restore (by version): " + of(Restore.countGood(reader, "ledger-vault", keys, manifest, true), N));

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
            line(out, "air-gap copy: " + of(copied, N) + " backups checked against the manifest and copied; restorable from the second store: " + of(Restore.countGood(reader2, "airgap", keys, manifest, true), N));
            S3Client intruder = second.client(ROOT, ROOT_SECRET);
            S3Client intruder2 = second.client("backup-writer", "demo-only-writer-secret-1");
            line(out, "attacker with the first store's root credentials asks the second store: " + Bucket.attempt(() -> intruder.listBuckets()) + "; with the backup-writer credentials: " + Bucket.attempt(() -> intruder2.listBuckets()));
            second.disconnect();
            S3Client impatient = second.impatientClient(VAULT_ROOT, VAULT_ROOT_SECRET);
            line(out, "second store disconnected between copies: even its own root credentials get: " + Bucket.attempt(() -> impatient.listBuckets()));
            second.reconnect();

            // ---- act 5
            line(out, "== 5. ransomware in the data directory, and the poisoned backup");
            primary.createUser("run-writer", "demo-only-runwriter-secret-1", "run-writer", Policies.writerOnly("runs-*"));
            primary.createUser("run-reader", "demo-only-runreader-secret-1", "run-reader", Policies.readerOnly("runs-*"));
            S3Client runWriter = primary.client("run-writer", "demo-only-runwriter-secret-1");
            S3Client runReader = primary.client("run-reader", "demo-only-runreader-secret-1");
            Guard guard = new Guard(Ledger.canary());
            line(out, "detectors: canary file changed; entropy of a file above " + Entropy.LIMIT + " bits per byte; entropy of a 4 KiB block above " + Entropy.LIMIT);
            Path lab = WORK.resolve("lab");
            record Variant(String name, boolean full, boolean skipCanary) {
            }
            for (Variant v : List.of(new Variant("healthy directory", false, false), new Variant("AES, all files, canary included", true, false), new Variant("AES, all files, canary skipped by name", true, true),
                    new Variant("AES on the first half of each file, canary skipped", false, true))) {
                Path dir = lab.resolve("v" + Math.abs(v.name().hashCode()));
                fill(dir);
                int touched = 0;
                if (!v.name().startsWith("healthy")) {
                    Set<String> skip = v.skipCanary() ? Set.of(Guard.CANARY_NAME) : Set.of();
                    touched = v.full() ? Ransomware.encryptAndDelete(dir, skip) : Ransomware.encryptHalfInPlace(dir, skip);
                }
                Guard.Verdict verdict = guard.check(dir);
                line(out, String.format("%-52s files encrypted=%d  %s  backup allowed=%s", v.name(), touched, verdict.detectors(), Guard.yesNo(verdict.clean())));
            }
            Path gz = lab.resolve("gz");
            fill(gz);
            ByteArrayOutputStream zipped = new ByteArrayOutputStream();
            try (GZIPOutputStream z = new GZIPOutputStream(zipped)) {
                z.write(Ledger.dayFile(40));
            }
            Files.write(gz.resolve("export-2026-10-09.csv.gz"), zipped.toByteArray());
            Guard.Verdict zv = guard.check(gz);
            line(out, String.format("%-52s files encrypted=0  %s  backup allowed=%s", "healthy directory with a compressed export", zv.detectors(), Guard.yesNo(zv.clean())));

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
            line(out, "hourly backups 06:00 to 09:00 of " + before.size() + " files: " + goodRuns.length + " runs in each bucket, " + written + " objects in the unguarded one");
            int infection = 9 * 60 + 20;
            int encrypted = Ransomware.encryptAndDelete(data, Set.of());
            line(out, Recovery.clock(infection) + " ransomware encrypts " + encrypted + " files and deletes the originals; files now in the directory: " + Ransomware.files(data).size());
            BackupJob.Result poisoned = unguarded.run("run-1000", data, false);
            BackupJob.Result refused = guarded.run("run-1000", data, true);
            line(out, "10:00 backup without the guard: wrote " + poisoned.objects() + " objects; with the guard: " + (refused.refused() ? "refused" : "wrote") + ", wrote " + refused.objects() + " objects (" + refused.verdict().detectors() + ")");
            Map<String, String> poisonedManifest = Restore.manifest(runReader, "runs-unguarded", "run-1000");
            int sumOk = 0;
            int contentOk = 0;
            for (Map.Entry<String, String> e : poisonedManifest.entrySet()) {
                byte[] bytes = Bucket.getLatest(runReader, "runs-unguarded", "run-1000/" + e.getKey());
                sumOk += Sha.hex(bytes).equals(e.getValue()) ? 1 : 0;
                contentOk += Guard.looksLikeLedger(bytes) ? 1 : 0;
            }
            line(out, "the poisoned backup against its own manifest: SHA-256 matches " + of(sumOk, poisonedManifest.size()) + ", looks like a ledger " + of(contentOk, poisonedManifest.size()) + " (a checksum proves a backup is intact, not that it is good)");
            r.clear();
            for (ObjectVersion v : Bucket.versions(root, "runs-unguarded")) {
                if (v.key().startsWith("run-1000/")) {
                    r.add(Bucket.attempt(() -> root.deleteObject(b -> b.bucket("runs-unguarded").key(v.key()).versionId(v.versionId()))));
                }
            }
            line(out, "deleting the poisoned objects to save space: refused " + of(Bucket.count(r, "AccessDenied"), r.size()) + " (the lock keeps bad backups as well as good ones)");
            Restore.RunChoice cu = Restore.newestGoodRun(runReader, "runs-unguarded", WORK.resolve("restore-unguarded"));
            Restore.RunChoice cg = Restore.newestGoodRun(runReader, "runs-guarded", WORK.resolve("restore-guarded"));
            line(out, "restore from the unguarded bucket: runs seen " + cu.runsSeen() + ", failed checksum " + cu.failedChecksum() + ", failed content " + cu.failedContent() + ", chosen " + cu.runId() + ", files " + cu.files());
            line(out, "restore from the guarded bucket:   runs seen " + cg.runsSeen() + ", failed checksum " + cg.failedChecksum() + ", failed content " + cg.failedContent() + ", chosen " + cg.runId() + ", files " + cg.files());
            Map<String, String> restoredHashes = hashes(WORK.resolve("restore-unguarded"));
            restoredHashes.remove("MANIFEST");
            int same = (int) before.entrySet().stream().filter(e -> e.getValue().equals(restoredHashes.get(e.getKey()))).count();
            line(out, "restored files identical to the directory at 09:00: " + of(same, before.size()));
            int cutOff = 10 * 60 + 40;
            int lastGood = Integer.parseInt(cu.runId().substring(4, 6)) * 60 + Integer.parseInt(cu.runId().substring(6, 8));
            line(out, "recovery point: last good backup " + Recovery.clock(lastGood) + ", systems cut off at " + Recovery.clock(cutOff) + ", transactions lost: " + Recovery.lostTransactions(lastGood, cutOff) + " at " + Recovery.TRANSACTIONS_PER_HOUR + " an hour");
            for (int every : new int[] {1440, 240, 60, 15}) {
                int last = Recovery.lastGoodBackup(infection, every);
                line(out, String.format("  schedule every %4d minutes: last good backup %s, transactions lost %d", every, Recovery.clock(last), Recovery.lostTransactions(last, cutOff)));
            }

            // ---- act 6
            line(out, "== 6. the restore exercise");
            Path fresh = WORK.resolve("restore-primary");
            Restore.Outcome a = Restore.toDirectory(reader, "ledger-vault", keys, manifest, fresh);
            line(out, "restore 1, from the vault bucket of the first store into a fresh directory: " + of(a.good(), N) + " backups good, " + a.bytes() + " bytes, " + a.rows() + " rows, ledger total matches: "
                    + Guard.yesNo(a.ledgerTotal() == expectedTotal && a.rows() == expectedRows) + ", steps " + a.steps());
            report(out, "RTO", a, 400);
            primary.close();
            S3Client gone = primary.impatientClient(ROOT, ROOT_SECRET);
            line(out, "the first store is destroyed (host lost): asking it gives " + Bucket.attempt(() -> gone.listBuckets()));
            Restore.Outcome b = Restore.toDirectory(reader2, "airgap", keys, manifest, WORK.resolve("restore-airgap"));
            line(out, "restore 2, from the air-gapped copy: " + of(b.good(), N) + " backups good, " + b.bytes() + " bytes, " + b.rows() + " rows, ledger total matches: "
                    + Guard.yesNo(b.ledgerTotal() == expectedTotal && b.rows() == expectedRows) + ", steps " + b.steps());
            report(out, "RTO", b, 100);
            line(out, "Not covered by the lock: root on the host of a store (restore 2 shows why a second store exists), a retention that is shorter than the time an attacker stays unnoticed, and data that was already encrypted when the oldest backup was made.");
        } finally {
            deleteTree(WORK.resolve("lab"));
        }
        return out.toString();
    }

    private static void report(StringBuilder out, String label, Restore.Outcome o, long mibPerSecond) {
        long seconds = Recovery.rtoSeconds(o.good(), o.steps(), mibPerSecond);
        line(out, "  " + label + " model: " + Recovery.DECLARE_SECONDS / 60 + " min to declare + " + Recovery.PROVISION_SECONDS / 60 + " min to build + " + o.steps() + " steps x " + Recovery.SECONDS_PER_STEP + " s + "
                + o.good() + " x " + Recovery.MODELLED_BYTES_PER_BACKUP / (1024L * 1024 * 1024) + " GiB at " + mibPerSecond + " MiB/s = " + Recovery.minutes(seconds)
                + "; target " + Recovery.TARGET_RTO_SECONDS / 60 + " min met: " + Guard.yesNo(seconds <= Recovery.TARGET_RTO_SECONDS));
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
