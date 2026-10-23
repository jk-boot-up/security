package com.jk.explore.security.ransomres;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import software.amazon.awssdk.services.s3.S3Client;

/** The nightly (here hourly) job: copy a data directory to a bucket under a prefix for the run, with a manifest of SHA-256 sums. */
final class BackupJob {

    /** What a run did: how many objects it wrote, and whether the guard stopped it. */
    record Result(boolean refused, int objects, Guard.Verdict verdict) {
    }

    private final S3Client client;
    private final String bucket;
    private final Guard guard;

    BackupJob(S3Client client, String bucket, Guard guard) {
        this.client = client;
        this.bucket = bucket;
        this.guard = guard;
    }

    /** Runs one backup. A job with a guard looks first and writes nothing when the data looks encrypted. */
    Result run(String runId, Path dir, boolean guarded) throws IOException {
        Guard.Verdict verdict = guard.check(dir);
        if (guarded && !verdict.clean()) {
            return new Result(true, 0, verdict);
        }
        TreeMap<String, String> manifest = new TreeMap<>();
        int objects = 0;
        for (Path file : Ransomware.files(dir)) {
            byte[] data = Files.readAllBytes(file);
            Bucket.put(client, bucket, runId + "/" + file.getFileName(), data);
            manifest.put(file.getFileName().toString(), Sha.hex(data));
            objects++;
        }
        StringBuilder text = new StringBuilder();
        manifest.forEach((name, sum) -> text.append(sum).append("  ").append(name).append('\n'));
        Bucket.put(client, bucket, runId + "/MANIFEST", text.toString().getBytes(StandardCharsets.UTF_8));
        return new Result(false, objects + 1, verdict);
    }
}
