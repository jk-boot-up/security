package com.jk.explore.security.sessionredis;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A throwaway Redis in a Docker container, bound to the loopback address on a free port, protected by a password and removed on close.
 * Needs Docker and the {@value #IMAGE} image (pulled on first use).
 */
public final class RedisLab implements AutoCloseable {

    static final String IMAGE = "redis:8.10.2-alpine";
    static final String PASSWORD = "lab-redis-secret";

    private final String container;
    private final int port;

    private RedisLab(String container, int port) {
        this.container = container;
        this.port = port;
    }

    public static RedisLab start() {
        String name = "session-redis-" + ProcessHandle.current().pid() + "-" + System.nanoTime();
        run("docker", "run", "-d", "--rm", "--name", name, "-p", "127.0.0.1::6379", IMAGE, "redis-server", "--requirepass", PASSWORD, "--save", "", "--appendonly", "no");
        RedisLab lab = null;
        try {
            String mapping = run("docker", "port", name, "6379/tcp").lines().filter(l -> l.startsWith("127.0.0.1:")).findFirst().orElseThrow();
            lab = new RedisLab(name, Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1)));
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

    public int port() {
        return port;
    }

    /** Runs redis-cli inside the container, with the password unless {@code authenticated} is false. */
    String cli(boolean authenticated, String... args) {
        List<String> command = new java.util.ArrayList<>(List.of("docker", "exec", container, "redis-cli"));
        if (authenticated) {
            command.addAll(List.of("-a", PASSWORD, "--no-auth-warning"));
        }
        command.addAll(List.of(args));
        return run(command.toArray(String[]::new)).strip();
    }

    /** Freezes the container, which looks to the application like a Redis that has stopped answering. */
    void pause() {
        run("docker", "pause", container);
    }

    void unpause() {
        run("docker", "unpause", container);
    }

    private void waitUntilReady() {
        for (int attempt = 0; attempt < 60; attempt++) {
            try {
                if (cli(true, "ping").equals("PONG")) {
                    return;
                }
            } catch (IllegalStateException e) {
                // not up yet
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Redis did not become ready");
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
