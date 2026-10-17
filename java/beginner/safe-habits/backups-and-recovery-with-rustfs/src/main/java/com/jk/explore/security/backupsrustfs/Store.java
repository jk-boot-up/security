package com.jk.explore.security.backupsrustfs;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.NoncurrentVersionExpiration;
import software.amazon.awssdk.services.s3.model.ObjectLockRetention;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRetentionRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

/** A thin S3 client for one identity: every call in the demo is made as a particular user, so what each user may do is visible. */
public class Store implements AutoCloseable {

    private final S3Client s3;

    public Store(String endpoint, String user, String secret) {
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(user, secret)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    public static Store root(ObjectStoreContainer container) {
        return new Store(container.endpoint(), ObjectStoreContainer.ROOT_USER, ObjectStoreContainer.ROOT_SECRET);
    }

    public void createBucket(String bucket, boolean objectLock) {
        s3.createBucket(CreateBucketRequest.builder().bucket(bucket).objectLockEnabledForBucket(objectLock).build());
    }

    public void enableVersioning(String bucket) {
        s3.putBucketVersioning(PutBucketVersioningRequest.builder().bucket(bucket)
                .versioningConfiguration(VersioningConfiguration.builder().status(BucketVersioningStatus.ENABLED).build()).build());
    }

    /** Stores the bytes with a SHA-256 checksum computed here; the store must agree with it. Returns the version id. */
    public String put(String bucket, String key, byte[] data) {
        return s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                .checksumSHA256(sha256Base64(data)).build(),
                RequestBody.fromBytes(data)).versionId();
    }

    /** Stores the bytes but claims a checksum for other bytes, as a transfer damaged on the way would. */
    public String putWithChecksumOf(String bucket, String key, byte[] data, byte[] claimedChecksumOf) {
        return s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                .checksumSHA256(sha256Base64(claimedChecksumOf)).build(),
                RequestBody.fromBytes(data)).versionId();
    }

    public byte[] get(String bucket, String key) {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
    }

    public byte[] getVersion(String bucket, String key, String versionId) {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).versionId(versionId).build()).asByteArray();
    }

    /** Deletes the key: on a versioned bucket this only adds a delete marker. */
    public void delete(String bucket, String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public void deleteVersion(String bucket, String key, String versionId, boolean bypassGovernance) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).versionId(versionId)
                .bypassGovernanceRetention(bypassGovernance).build());
    }

    public void retain(String bucket, String key, String versionId, ObjectLockRetentionMode mode, Instant until, boolean bypassGovernance) {
        s3.putObjectRetention(PutObjectRetentionRequest.builder().bucket(bucket).key(key).versionId(versionId)
                .retention(ObjectLockRetention.builder().mode(mode).retainUntilDate(until).build())
                .bypassGovernanceRetention(bypassGovernance).build());
    }

    /** The version ids of a key, newest first, with delete markers counted as versions. */
    public List<String> versionIds(String bucket, String key) {
        var response = s3.listObjectVersions(ListObjectVersionsRequest.builder().bucket(bucket).prefix(key).build());
        List<String> ids = new java.util.ArrayList<>();
        response.versions().stream().filter(v -> v.key().equals(key)).forEach(v -> ids.add(v.versionId()));
        response.deleteMarkers().stream().filter(v -> v.key().equals(key)).forEach(v -> ids.add(v.versionId()));
        return ids;
    }

    public int deleteMarkerCount(String bucket, String key) {
        return (int) s3.listObjectVersions(ListObjectVersionsRequest.builder().bucket(bucket).prefix(key).build())
                .deleteMarkers().stream().filter(v -> v.key().equals(key)).count();
    }

    public void expireOldVersionsAfter(String bucket, int days) {
        LifecycleRule rule = LifecycleRule.builder().id("expire-old-versions").status(ExpirationStatus.ENABLED)
                .filter(LifecycleRuleFilter.builder().prefix("").build())
                .noncurrentVersionExpiration(NoncurrentVersionExpiration.builder().noncurrentDays(days).build()).build();
        s3.putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest.builder().bucket(bucket)
                .lifecycleConfiguration(BucketLifecycleConfiguration.builder().rules(rule).build()).build());
    }

    public List<LifecycleRule> lifecycleRules(String bucket) {
        return s3.getBucketLifecycleConfiguration(b -> b.bucket(bucket)).rules();
    }

    public static String sha256Base64(byte[] data) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** "allowed", or "refused" with the store's HTTP status: the demo prints these so each identity's powers are plain. */
    public static String outcome(Runnable call) {
        try {
            call.run();
            return "allowed";
        } catch (S3Exception e) {
            return "refused (" + e.statusCode() + " " + e.awsErrorDetails().errorCode() + ")";
        }
    }

    public static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        s3.close();
    }
}
