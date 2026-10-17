package com.jk.explore.security.tlsopenssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Finds a real OpenSSL 3 and runs it. Processes are always destroyed. */
final class OpenSsl {

    record Result(int exit, String output) {
    }

    private static final String[] CANDIDATES = {"openssl", "/opt/homebrew/opt/openssl@3/bin/openssl", "/usr/local/opt/openssl@3/bin/openssl"};

    private OpenSsl() {
    }

    /** The command of an OpenSSL 3 (not LibreSSL, not 1.x), or null. */
    static String find() {
        for (String candidate : CANDIDATES) {
            try {
                Process process = new ProcessBuilder(candidate, "version").redirectErrorStream(true).start();
                try {
                    String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    if (process.waitFor(20, TimeUnit.SECONDS) && text.startsWith("OpenSSL 3")) {
                        return candidate;
                    }
                } finally {
                    process.destroyForcibly();
                }
            } catch (IOException | InterruptedException e) {
                // try the next candidate
            }
        }
        return null;
    }

    static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    /** Runs openssl with the given arguments and standard input, and waits for it to end (at most 30 seconds). */
    static Result run(String command, String stdin, String... args) throws IOException, InterruptedException {
        List<String> line = new ArrayList<>();
        line.add(command);
        line.addAll(List.of(args));
        Process process = new ProcessBuilder(line).redirectErrorStream(true).start();
        try {
            process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().flush();
            byte[][] output = new byte[1][];
            Thread collect = Thread.ofPlatform().daemon().start(() -> {
                try {
                    output[0] = process.getInputStream().readAllBytes();
                } catch (IOException e) {
                    output[0] = new byte[0];
                }
            });
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                collect.join(5000);
                return new Result(-1, "timed out: " + (output[0] == null ? "" : new String(output[0], StandardCharsets.UTF_8)));
            }
            collect.join(5000);
            return new Result(process.exitValue(), new String(output[0], StandardCharsets.UTF_8));
        } finally {
            process.destroyForcibly();
        }
    }

    /** A running s_server that is stopped on close. */
    static final class Server implements AutoCloseable {

        final int port;
        private final Process process;

        Server(String command, Path cert, Path key, String... extra) throws IOException, InterruptedException {
            port = freePort();
            List<String> line = new ArrayList<>(List.of(command, "s_server", "-accept", "127.0.0.1:" + port, "-cert", cert.toString(), "-key", key.toString(), "-www"));
            line.addAll(List.of(extra));
            process = new ProcessBuilder(line).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            long deadline = System.currentTimeMillis() + 15000;
            while (true) {
                try (Socket probe = new Socket()) {
                    probe.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 500);
                    break;
                } catch (IOException e) {
                    if (!process.isAlive() || System.currentTimeMillis() > deadline) {
                        close();
                        throw new IOException("s_server did not start");
                    }
                    Thread.sleep(50);
                }
            }
        }

        @Override
        public void close() {
            process.destroyForcibly();
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static void writeCertAndKey(Path dir, String keystore, String name) throws Exception {
        Pem.writeCertificate(dir.resolve(name + "-cert.pem"), TestCertificates.certificate(keystore));
        Pem.writePrivateKey(dir.resolve(name + "-key.pem"), TestCertificates.privateKey(keystore));
    }

    static String read(Path file) throws IOException {
        return Files.readString(file);
    }
}
