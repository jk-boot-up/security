package com.jk.explore.security.baosecrets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * One OpenBao in dev mode and one PostgreSQL, on a private Docker network, each bound to a free port on the loopback address and removed on close.
 * OpenBao reaches PostgreSQL by its name on that network; the demo reaches both through the loopback ports.
 */
public final class BaoLab implements AutoCloseable {

    static final String BAO_IMAGE = "openbao/openbao:2.7.0";
    static final String POSTGRES_IMAGE = "postgres:18.6-alpine";
    static final String ROOT_TOKEN = "lab-root-token";
    static final String DB_ADMIN_PASSWORD = "lab-db-admin";

    private final String suffix;
    private final String network;
    private final String bao;
    private final String db;
    private final int baoPort;
    private final int dbPort;

    private BaoLab(String suffix, int baoPort, int dbPort) {
        this.suffix = suffix;
        this.network = "bao-net-" + suffix;
        this.bao = "bao-" + suffix;
        this.db = "bao-db-" + suffix;
        this.baoPort = baoPort;
        this.dbPort = dbPort;
    }

    public static BaoLab start() throws IOException, InterruptedException {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        run("docker", "network", "create", "bao-net-" + suffix);
        try {
            run("docker", "run", "-d", "--rm", "--name", "bao-db-" + suffix, "--network", "bao-net-" + suffix, "--network-alias", "db", "-p", "127.0.0.1::5432",
                    "-e", "POSTGRES_PASSWORD=" + DB_ADMIN_PASSWORD, POSTGRES_IMAGE);
            java.nio.file.Path config = java.nio.file.Files.createTempDirectory("bao-config");
            java.nio.file.Files.writeString(config.resolve("audit.hcl"), "audit \"file\" \"to_file\" {\n  options {\n    file_path = \"/tmp/audit.log\"\n  }\n}\n");
            run("docker", "run", "-d", "--rm", "--name", "bao-" + suffix, "--network", "bao-net-" + suffix, "-p", "127.0.0.1::8200", "--cap-add", "IPC_LOCK",
                    "-v", config.toAbsolutePath() + ":/config:ro", "-e", "BAO_DEV_ROOT_TOKEN_ID=" + ROOT_TOKEN, "-e", "BAO_DEV_LISTEN_ADDRESS=0.0.0.0:8200", BAO_IMAGE, "server", "-dev", "-config=/config/audit.hcl");
            BaoLab lab = new BaoLab(suffix, port("bao-" + suffix, "8200/tcp"), port("bao-db-" + suffix, "5432/tcp"));
            lab.awaitReady();
            return lab;
        } catch (IOException | InterruptedException | RuntimeException e) {
            new BaoLab(suffix, 0, 0).close();
            throw e;
        }
    }

    String baoUrl() {
        return "http://127.0.0.1:" + baoPort;
    }

    /** The JDBC address of the database, from the host. */
    String jdbcUrl() {
        return "jdbc:postgresql://127.0.0.1:" + dbPort + "/postgres";
    }

    private void awaitReady() throws InterruptedException {
        for (int i = 0; i < 120; i++) {
            try (Connection ignored = DriverManager.getConnection(jdbcUrl(), "postgres", DB_ADMIN_PASSWORD)) {
                if (health()) {
                    return;
                }
            } catch (SQLException | IOException e) {
                // not up yet
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("OpenBao and PostgreSQL did not become ready");
    }

    private boolean health() throws IOException, InterruptedException {
        return java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(baoUrl() + "/v1/sys/health")).build(),
                java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
    }

    /** What the file audit device has written inside the container. */
    String auditLog() throws IOException, InterruptedException {
        return run("docker", "exec", bao, "cat", "/tmp/audit.log");
    }

    /** The unseal key that a dev server prints when it starts. */
    String unsealKey() throws IOException, InterruptedException {
        return logs().lines().filter(l -> l.startsWith("Unseal Key:")).map(l -> l.substring("Unseal Key:".length()).strip()).findFirst().orElseThrow();
    }

    /** The audit device writes to the container's standard output; this is what OpenBao has logged since it started. */
    String logs() throws IOException, InterruptedException {
        return run("docker", "logs", bao);
    }

    @Override
    public void close() {
        for (List<String> command : List.of(List.of("docker", "rm", "-f", bao), List.of("docker", "rm", "-f", db), List.of("docker", "network", "rm", network))) {
            try {
                run(command.toArray(String[]::new));
            } catch (IOException | InterruptedException | IllegalStateException e) {
                // already gone
            }
        }
    }

    private static int port(String container, String port) throws IOException, InterruptedException {
        String mapping = run("docker", "port", container, port).lines().filter(l -> l.startsWith("127.0.0.1:")).findFirst().orElseThrow();
        return Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1).trim());
    }

    private static String run(String... command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(180, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }
}
