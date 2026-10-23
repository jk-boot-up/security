package com.jk.explore.security.ransomres;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/** An S3-compatible object store (RustFS) in a Docker container, started for the demo and removed at the end. */
public final class Store implements AutoCloseable {

    static final String IMAGE = "rustfs/rustfs:latest";
    static final Region REGION = Region.US_EAST_1;
    private static final HexFormat HEX = HexFormat.of();

    private final String name;
    private final String root;
    private final String rootSecret;
    private final String endpoint;

    public Store(String label, String root, String rootSecret) throws IOException, InterruptedException {
        this.name = "rr-" + label + "-" + UUID.randomUUID().toString().substring(0, 6);
        this.root = root;
        this.rootSecret = rootSecret;
        Sh.must(List.of("docker", "run", "-d", "--rm", "--name", name, "-e", "RUSTFS_ACCESS_KEY=" + root, "-e", "RUSTFS_SECRET_KEY=" + rootSecret, "-p", "127.0.0.1::9000", IMAGE));
        String hostPort = Sh.must(List.of("docker", "port", name, "9000/tcp")).lines().findFirst().orElseThrow().strip();
        this.endpoint = "http://127.0.0.1:" + hostPort.substring(hostPort.lastIndexOf(':') + 1);
        S3Client probe = client(root, rootSecret);
        RuntimeException last = null;
        for (int i = 0; i < 60; i++) {
            try {
                probe.listBuckets();
                last = null;
                break;
            } catch (RuntimeException e) {
                last = e;
                Thread.sleep(500);
            }
        }
        probe.close();
        if (last != null) {
            close();
            throw last;
        }
    }

    String endpoint() {
        return endpoint;
    }

    /** An S3 client that signs with the given credentials. */
    public S3Client client(String accessKey, String secretKey) {
        return S3Client.builder().endpointOverride(URI.create(endpoint)).region(REGION)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .httpClient(Apache5HttpClient.create()).build();
    }

    /** A client that gives up after 3 seconds and does not retry: for asking a store that may be switched off. */
    public S3Client impatientClient(String accessKey, String secretKey) {
        return S3Client.builder().endpointOverride(URI.create(endpoint)).region(REGION)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .overrideConfiguration(o -> o.apiCallTimeout(Duration.ofSeconds(3)).retryStrategy(AwsRetryStrategy.doNotRetry()))
                .httpClient(Apache5HttpClient.create()).build();
    }

    public S3Client admin() {
        return client(root, rootSecret);
    }

    String rootKey() {
        return root;
    }

    String rootSecret() {
        return rootSecret;
    }

    /** Creates a user and attaches a policy to it, through the store's admin API (signed with the root credentials, Signature Version 4). */
    public void createUser(String accessKey, String secretKey, String policyName, String policyJson) throws Exception {
        adminCall("PUT", "/rustfs/admin/v3/add-user", "accessKey=" + accessKey, "{\"secretKey\":\"" + secretKey + "\",\"status\":\"enabled\"}");
        adminCall("PUT", "/rustfs/admin/v3/add-canned-policy", "name=" + policyName, policyJson);
        adminCall("PUT", "/rustfs/admin/v3/set-user-or-group-policy", "isGroup=false&policyName=" + policyName + "&userOrGroup=" + accessKey, "");
    }

    private void adminCall(String method, String path, String query, String body) throws Exception {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        String payloadHash = HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        URI uri = URI.create(endpoint + path + "?" + query);
        String host = uri.getHost() + ":" + uri.getPort();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String day = amzDate.substring(0, 8);
        String canonical = method + "\n" + path + "\n" + query + "\nhost:" + host + "\nx-amz-content-sha256:" + payloadHash + "\nx-amz-date:" + amzDate
                + "\n\nhost;x-amz-content-sha256;x-amz-date\n" + payloadHash;
        String scope = day + "/us-east-1/s3/aws4_request";
        String toSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + scope + "\n" + HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        byte[] k = hmac(("AWS4" + rootSecret).getBytes(StandardCharsets.UTF_8), day);
        k = hmac(k, "us-east-1");
        k = hmac(k, "s3");
        k = hmac(k, "aws4_request");
        String signature = HEX.formatHex(hmac(k, toSign));
        HttpRequest request = HttpRequest.newBuilder(uri).method(method, HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("x-amz-content-sha256", payloadHash).header("x-amz-date", amzDate)
                .header("Authorization", "AWS4-HMAC-SHA256 Credential=" + root + "/" + scope + ", SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=" + signature).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(path + " answered " + response.statusCode() + ": " + response.body());
        }
    }

    private static byte[] hmac(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        try {
            Sh.run("docker", "rm", "-f", name);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Freezes the container: the store is unreachable, as if its cable were pulled, and keeps its data. */
    void disconnect() throws IOException, InterruptedException {
        Sh.must(List.of("docker", "pause", name));
    }

    void reconnect() throws IOException, InterruptedException {
        Sh.must(List.of("docker", "unpause", name));
    }
}
