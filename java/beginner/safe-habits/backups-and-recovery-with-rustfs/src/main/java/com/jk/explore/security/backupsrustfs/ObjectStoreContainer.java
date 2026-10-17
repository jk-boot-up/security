package com.jk.explore.security.backupsrustfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A local S3-compatible object store in Docker, and the admin tool that creates users and policies in it.
 *
 * <p>MinIO's community edition stopped publishing container images, so this project uses RustFS, an open-source store that
 * speaks the same S3 API and the same MinIO-style admin API. Everything here is about the S3 features, so it applies to
 * MinIO, RustFS and to AWS S3 alike.
 */
public class ObjectStoreContainer implements AutoCloseable {

    /** Both images are pinned to exact versions, so every run uses the same software. */
    public static final String STORE_IMAGE = "rustfs/rustfs:1.0.2";
    public static final String ADMIN_IMAGE = "rustfs/rc:v0.1.36";

    /** The store's root account. These are demo values for a store that exists only for the length of a run. */
    public static final String ROOT_USER = "demo-root";
    public static final String ROOT_SECRET = "demo-root-secret-1";

    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private final String network = "backups-net-" + id;
    private final String name = "backups-store-" + id;
    private final Path policyDir;
    private int port;

    private ObjectStoreContainer() throws IOException {
        this.policyDir = Files.createTempDirectory("backups-policies");
    }

    public int port() {
        return port;
    }

    public String endpoint() {
        return "http://127.0.0.1:" + port;
    }

    /** Starts the store on a free local port and waits until it answers. */
    public static ObjectStoreContainer start() throws IOException, InterruptedException {
        ObjectStoreContainer store = new ObjectStoreContainer();
        try {
            run(List.of("docker", "network", "create", store.network));
            run(List.of("docker", "run", "-d", "--rm", "--name", store.name, "--network", store.network,
                    "-p", "127.0.0.1::9000",
                    "-e", "RUSTFS_ACCESS_KEY=" + ROOT_USER, "-e", "RUSTFS_SECRET_KEY=" + ROOT_SECRET,
                    "--tmpfs", "/data:rw,mode=1777", "--tmpfs", "/logs:rw,mode=1777", STORE_IMAGE));
            String mapping = run(List.of("docker", "port", store.name, "9000/tcp")).lines().findFirst().orElseThrow().trim();
            store.port = Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1));
            store.awaitReady();
            return store;
        } catch (IOException | InterruptedException | RuntimeException e) {
            store.close();
            throw e;
        }
    }

    private void awaitReady() throws InterruptedException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        for (int i = 0; i < 300; i++) {
            try {
                var response = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(endpoint() + "/health")).build(),
                        java.net.http.HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("the object store did not start");
    }

    /** Runs the admin tool against this store, as the root account, and returns its output. */
    public String admin(String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("docker", "run", "--rm", "--network", network,
                "-v", policyDir + ":/policies:ro",
                "-e", "RC_HOST_local=http://" + ROOT_USER + ":" + ROOT_SECRET + "@" + name + ":9000", ADMIN_IMAGE));
        command.addAll(List.of(arguments));
        return run(command);
    }

    /** Creates a user who may only add objects to the given bucket: the policy has one action, PutObject. */
    public void createWriteOnlyUser(String user, String secret, String bucket) throws IOException, InterruptedException {
        Files.writeString(policyDir.resolve(user + ".json"), writeOnlyPolicy(bucket));
        admin("admin", "user", "add", "local", user, secret);
        admin("admin", "policy", "create", "local", user + "-policy", "/policies/" + user + ".json");
        admin("admin", "policy", "attach", "local", user + "-policy", "--user", user);
    }

    /** The IAM policy of the backup job: one action, on the objects of one bucket, and nothing else. */
    static String writeOnlyPolicy(String bucket) {
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"s3:PutObject\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
    }

    static String run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        if (!process.waitFor(120, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + output);
        }
        return output;
    }

    @Override
    public void close() {
        try {
            run(List.of("docker", "stop", "-t", "1", name));
        } catch (IOException | InterruptedException | RuntimeException e) {
            // the container was started with --rm, so it goes away on its own once stopped
        }
        try {
            run(List.of("docker", "network", "rm", network));
        } catch (IOException | InterruptedException | RuntimeException e) {
            // already gone
        }
        try (var files = Files.list(policyDir)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(policyDir);
        } catch (IOException e) {
            // a leftover temporary file does no harm
        }
    }
}
