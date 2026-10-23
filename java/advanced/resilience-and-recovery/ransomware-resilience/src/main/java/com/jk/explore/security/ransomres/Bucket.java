package com.jk.explore.security.ransomres;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** The few S3 calls the demo uses over and over, and how an answer is turned into "ok" or an error code. */
final class Bucket {

    private Bucket() {
    }

    /** Runs a call and returns "ok" or the S3 error code, or "unreachable" when the store does not answer. */
    static String attempt(Runnable call) {
        try {
            call.run();
            return "ok";
        } catch (S3Exception e) {
            return e.awsErrorDetails().errorCode();
        } catch (SdkClientException e) {
            return "unreachable";
        }
    }

    static void plain(S3Client c, String name) {
        c.createBucket(b -> b.bucket(name));
    }

    static void versioned(S3Client c, String name) {
        c.createBucket(b -> b.bucket(name));
        c.putBucketVersioning(b -> b.bucket(name).versioningConfiguration(v -> v.status(BucketVersioningStatus.ENABLED)));
    }

    /** A bucket with object lock (which turns versioning on) and a default retention in COMPLIANCE mode. */
    static void locked(S3Client c, String name, int days) {
        c.createBucket(b -> b.bucket(name).objectLockEnabledForBucket(true));
        c.putObjectLockConfiguration(b -> b.bucket(name).objectLockConfiguration(o -> o.objectLockEnabled(ObjectLockEnabled.ENABLED)
                .rule(r -> r.defaultRetention(d -> d.mode(ObjectLockRetentionMode.COMPLIANCE).days(days)))));
    }

    static String put(S3Client c, String bucket, String key, byte[] data) {
        return c.putObject(b -> b.bucket(bucket).key(key), RequestBody.fromBytes(data)).versionId();
    }

    static byte[] get(S3Client c, String bucket, String key, String versionId) {
        return c.getObjectAsBytes(b -> b.bucket(bucket).key(key).versionId(versionId)).asByteArray();
    }

    static byte[] getLatest(S3Client c, String bucket, String key) {
        return c.getObjectAsBytes(b -> b.bucket(bucket).key(key)).asByteArray();
    }

    static List<ObjectVersion> versions(S3Client c, String bucket) {
        return new ArrayList<>(c.listObjectVersionsPaginator(b -> b.bucket(bucket)).versions().stream().toList());
    }

    static List<ObjectVersion> versions(S3Client c, String bucket, String key) {
        return versions(c, bucket).stream().filter(v -> v.key().equals(key)).toList();
    }

    static int deleteMarkers(S3Client c, String bucket) {
        return c.listObjectVersionsPaginator(b -> b.bucket(bucket)).deleteMarkers().stream().toList().size();
    }

    /** Counts the answers that are ok. */
    static int count(List<String> answers, String wanted) {
        return (int) answers.stream().filter(wanted::equals).count();
    }

    static <T> T orElse(Supplier<T> s, T fallback) {
        try {
            return s.get();
        } catch (S3Exception e) {
            return fallback;
        }
    }
}
