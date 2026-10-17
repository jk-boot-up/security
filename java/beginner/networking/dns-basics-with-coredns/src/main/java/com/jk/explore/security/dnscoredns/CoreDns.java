package com.jk.explore.security.dnscoredns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** One CoreDNS container with a given zone, started for a demo and stopped afterwards. It needs a running Docker. */
public class CoreDns implements AutoCloseable {

    /** The image is pinned to an exact version, so every run uses the same CoreDNS. */
    public static final String IMAGE = "coredns/coredns:1.14.7";

    private final String name;
    private final Path dir;
    private final int udpPort;
    private final int tcpPort;

    private CoreDns(String name, Path dir, int udpPort, int tcpPort) {
        this.name = name;
        this.dir = dir;
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;
    }

    public int udpPort() {
        return udpPort;
    }

    public int tcpPort() {
        return tcpPort;
    }

    /** Starts a container serving this zone on free local ports (UDP and TCP, loopback only) and waits until it answers. */
    public static CoreDns start(String corefile, String zoneFile) throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory("coredns-demo");
        Files.writeString(dir.resolve("Corefile"), corefile);
        Files.writeString(dir.resolve(ShopZone.ZONE + ".db"), zoneFile);
        String name = "dns-coredns-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            run(List.of("docker", "run", "-d", "--rm", "--name", name, "-p", "127.0.0.1::53/udp", "-p", "127.0.0.1::53/tcp",
                    "-v", dir + ":/etc/coredns:ro", IMAGE, "-conf", "/etc/coredns/Corefile"));
            CoreDns server = new CoreDns(name, dir, port(name, "53/udp"), port(name, "53/tcp"));
            try {
                server.awaitReady();
            } catch (IOException | InterruptedException | RuntimeException e) {
                server.close();
                throw e;
            }
            return server;
        } catch (IOException | InterruptedException | RuntimeException e) {
            new CoreDns(name, dir, 0, 0).close();
            throw e;
        }
    }

    private static int port(String name, String mapping) throws IOException, InterruptedException {
        String text = run(List.of("docker", "port", name, mapping)).lines().findFirst().orElseThrow().trim();
        return Integer.parseInt(text.substring(text.lastIndexOf(':') + 1));
    }

    private void awaitReady() throws IOException, InterruptedException {
        for (int i = 0; i < 100; i++) {
            try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), tcpPort)) {
                socket.setSoTimeout(500);
                if (Client.tcpAnswers(tcpPort)) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("CoreDNS did not start");
    }

    private static String run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(120, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }

    @Override
    public void close() {
        try {
            run(List.of("docker", "stop", "-t", "1", name));
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // the container was started with --rm, so it goes away on its own
        }
        try {
            try (var files = Files.list(dir)) {
                for (Path file : files.toList()) {
                    Files.deleteIfExists(file);
                }
            }
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            // a leftover temporary file does no harm
        }
    }
}
