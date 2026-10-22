package com.jk.explore.security.sqlihibernate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A throwaway PostgreSQL in a Docker container, bound to the loopback address on a free port and removed on close.
 * Needs Docker and the {@value #IMAGE} image (pulled on first use).
 */
public final class PostgresLab implements AutoCloseable {

    static final String IMAGE = "postgres:18.6-alpine";
    static final String ADMIN_PASSWORD = "lab-admin-only";

    private final String container;
    private final int port;

    private PostgresLab(String container, int port) {
        this.container = container;
        this.port = port;
    }

    public static PostgresLab start() {
        String name = "sqli-hibernate-" + ProcessHandle.current().pid() + "-" + System.nanoTime();
        run("docker", "run", "-d", "--rm", "--name", name, "-p", "127.0.0.1::5432", "-e", "POSTGRES_PASSWORD=" + ADMIN_PASSWORD, IMAGE);
        PostgresLab lab = null;
        try {
            String mapping = run("docker", "port", name, "5432/tcp").lines().filter(l -> l.startsWith("127.0.0.1:")).findFirst().orElseThrow();
            lab = new PostgresLab(name, Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1)));
            lab.waitUntilReady();
            return lab;
        } catch (RuntimeException e) {
            if (lab != null) {
                lab.close();
            } else {
                run("docker", "rm", "-f", name);
            }
            throw e;
        }
    }

    public String url() {
        return "jdbc:postgresql://127.0.0.1:" + port + "/postgres";
    }

    private void waitUntilReady() {
        for (int attempt = 0; attempt < 60; attempt++) {
            try (Connection ignored = DriverManager.getConnection(url(), "postgres", ADMIN_PASSWORD)) {
                return;
            } catch (SQLException e) {
                sleep();
            }
        }
        throw new IllegalStateException("PostgreSQL did not become ready");
    }

    private static void sleep() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        run("docker", "rm", "-f", container);
    }

    private static String run(String... command) {
        try {
            Process process = new ProcessBuilder(List.of(command)).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            if (!process.waitFor(120, TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new IllegalStateException(String.join(" ", command) + " failed: " + output);
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("could not run " + command[0], e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
