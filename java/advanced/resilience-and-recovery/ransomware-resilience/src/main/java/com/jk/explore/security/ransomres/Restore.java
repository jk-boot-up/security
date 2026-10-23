package com.jk.explore.security.ransomres;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Getting the backups back: which versions are good, written to a fresh directory, with every step counted. */
final class Restore {

    /** The result of a restore into a directory. */
    record Outcome(int good, int notGood, long bytes, int steps, int rows, long ledgerTotal) {
    }

    /** The result of looking for the newest run of a backup that is good. */
    record RunChoice(String runId, int runsSeen, int failedChecksum, int failedContent, int files, int steps) {
    }

    private Restore() {
    }

    /**
     * How many of these keys have a version whose SHA-256 equals the manifest. A restore that takes only the latest version
     * (versionAware false) is what happens when nobody looks at the versions.
     */
    static int countGood(S3Client c, String bucket, List<String> keys, Map<String, String> manifest, boolean versionAware) {
        int good = 0;
        for (String key : keys) {
            try {
                if (versionAware) {
                    for (ObjectVersion v : Bucket.versions(c, bucket, key)) {
                        if (Sha.hex(Bucket.get(c, bucket, key, v.versionId())).equals(manifest.get(key))) {
                            good++;
                            break;
                        }
                    }
                } else if (Sha.hex(Bucket.getLatest(c, bucket, key)).equals(manifest.get(key))) {
                    good++;
                }
            } catch (S3Exception e) {
                // a key that is gone is not good
            }
        }
        return good;
    }

    /** Restores every key into the directory from the version that matches the manifest. Each list, download, write, hash and comparison is a step. */
    static Outcome toDirectory(S3Client c, String bucket, List<String> keys, Map<String, String> manifest, Path dir) throws IOException {
        Files.createDirectories(dir);
        int steps = 0;
        int good = 0;
        int notGood = 0;
        long bytes = 0;
        int rows = 0;
        long total = 0;
        for (String key : keys) {
            List<ObjectVersion> versions = Bucket.versions(c, bucket, key);
            steps++;
            byte[] chosen = null;
            for (ObjectVersion v : versions) {
                byte[] data = Bucket.get(c, bucket, key, v.versionId());
                steps++;
                Files.write(dir.resolve(key), data);
                steps++;
                String sum = Sha.hex(data);
                steps++;
                steps++;
                if (sum.equals(manifest.get(key))) {
                    chosen = data;
                    break;
                }
            }
            if (chosen == null) {
                notGood++;
                Files.deleteIfExists(dir.resolve(key));
            } else {
                good++;
                bytes += chosen.length;
                rows += Ledger.rowCount(chosen);
                total += Ledger.total(chosen);
            }
        }
        return new Outcome(good, notGood, bytes, steps, rows, total);
    }

    /**
     * Looks through the runs of a backup bucket from the newest, and restores the first one in which every file matches its manifest
     * and still looks like a ledger.
     */
    static RunChoice newestGoodRun(S3Client c, String bucket, Path dir) throws IOException {
        TreeSet<String> runs = new TreeSet<>(Comparator.reverseOrder());
        Bucket.versions(c, bucket).forEach(v -> runs.add(v.key().substring(0, v.key().indexOf('/'))));
        int seen = 0;
        int badSum = 0;
        int badContent = 0;
        int steps = 1;
        for (String run : runs) {
            seen++;
            Map<String, String> manifest = manifest(c, bucket, run);
            steps++;
            Map<String, byte[]> files = new TreeMap<>();
            boolean sumsOk = true;
            boolean contentOk = true;
            for (Map.Entry<String, String> e : manifest.entrySet()) {
                byte[] data;
                try {
                    data = Bucket.getLatest(c, bucket, run + "/" + e.getKey());
                } catch (S3Exception ex) {
                    sumsOk = false;
                    break;
                }
                steps += 2;
                sumsOk &= Sha.hex(data).equals(e.getValue());
                contentOk &= Guard.looksLikeLedger(data) && !e.getKey().endsWith(".locked");
                files.put(e.getKey(), data);
            }
            if (!sumsOk) {
                badSum++;
            } else if (!contentOk) {
                badContent++;
            } else {
                Files.createDirectories(dir);
                for (Map.Entry<String, byte[]> e : files.entrySet()) {
                    Files.write(dir.resolve(e.getKey()), e.getValue());
                    steps++;
                }
                return new RunChoice(run, seen, badSum, badContent, files.size(), steps);
            }
        }
        return new RunChoice(null, seen, badSum, badContent, 0, steps);
    }

    static Map<String, String> manifest(S3Client c, String bucket, String run) {
        Map<String, String> m = new TreeMap<>();
        try {
            String text = new String(Bucket.getLatest(c, bucket, run + "/MANIFEST"), StandardCharsets.UTF_8);
            for (String line : text.split("\n")) {
                int sep = line.indexOf("  ");
                if (sep > 0) {
                    m.put(line.substring(sep + 2), line.substring(0, sep));
                }
            }
        } catch (S3Exception e) {
            return new TreeMap<>();
        }
        return m;
    }

    static List<String> snapshotKeys(int n) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            keys.add(String.format("ledger-%02d.csv", i));
        }
        return keys;
    }
}
