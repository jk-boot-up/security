package com.jk.explore.security.certlife;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** A real certificate authority, step-ca, running on this machine and driven with the step command line. It can be stopped and started again. */
final class StepCa implements AutoCloseable {

    private final Path work;
    private final Path home;
    private final Map<String, String> env;
    private final List<String> connect;
    private final Path password;
    private Process process;

    StepCa(Path dir) throws IOException, InterruptedException {
        this.work = Files.createDirectories(dir.toAbsolutePath());
        this.home = work.resolve("home");
        this.env = Map.of("STEPPATH", home.toString());
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        password = work.resolve("password");
        Files.writeString(password, HexFormat.of().formatHex(new SecureRandom().generateSeed(16)));
        Sh.must(work, env, List.of("step", "ca", "init", "--name", "Bank Services CA", "--dns", "localhost", "--address", "127.0.0.1:" + port,
                "--provisioner", "services", "--password-file", password.toString(), "--provisioner-password-file", password.toString(), "--deployment-type", "standalone"));
        tune(home.resolve("config/ca.json"));
        connect = List.of("--ca-url", "https://localhost:" + port, "--root", root().toString());
        start();
    }

    /** Short lifetimes allowed (down to one second) and certificates valid from the moment of issue. */
    private static void tune(Path caJson) throws IOException {
        ObjectMapper mapper = JsonMapper.builder().build();
        JsonNode parsed = mapper.readTree(Files.readString(caJson));
        ObjectNode authority = (ObjectNode) parsed.get("authority");
        authority.put("backdate", "0s");
        ObjectNode claims = authority.putObject("claims");
        claims.put("minTLSCertDuration", "1s");
        claims.put("maxTLSCertDuration", "24h");
        claims.put("defaultTLSCertDuration", "1h");
        Files.writeString(caJson, mapper.writeValueAsString(parsed));
    }

    /** Starts step-ca and waits until it answers its health check. */
    void start() throws IOException, InterruptedException {
        process = new ProcessBuilder("step-ca", home.resolve("config/ca.json").toString(), "--password-file", password.toString())
                .directory(work.toFile()).redirectErrorStream(true).redirectOutput(work.resolve("ca.log").toFile()).start();
        for (int i = 0; ; i++) {
            if (Sh.run(work, env, "", join(List.of("step", "ca", "health"), connect)).output().trim().equals("ok")) {
                return;
            }
            if (i > 100) {
                throw new IllegalStateException("step-ca did not start");
            }
            Thread.sleep(100);
        }
    }

    /** Stops step-ca: from now on every request to it fails to connect. */
    void stop() throws InterruptedException {
        process.destroy();
        process.waitFor();
    }

    private static List<String> join(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    Path file(String name) {
        return work.resolve(name);
    }

    Path root() {
        return home.resolve("certs/root_ca.crt");
    }

    X509Certificate rootCertificate() throws IOException, GeneralSecurityException {
        return Pki.readCertificates(root()).getFirst();
    }

    /** A new key and a certificate for the name, valid for the given seconds; the key is written as PKCS#8. */
    Credential issue(String base, String name, int seconds) throws IOException, InterruptedException, GeneralSecurityException {
        Sh.must(work, env, join(List.of("step", "ca", "certificate", name, base + ".crt", base + ".key", "--provisioner", "services",
                "--provisioner-password-file", "password", "--not-after", seconds + "s", "--force"), connect));
        Sh.must(work, env, List.of("step", "crypto", "key", "format", "--pkcs8", "--no-password", "--insecure", "--force", "--out", base + ".p8", base + ".key"));
        return Credential.load(work.resolve(base + ".crt"), work.resolve(base + ".p8"));
    }

    /** Asks the CA for a new certificate for the same key, authenticating with the certificate we hold (it must not have expired). */
    Credential renew(String from, String to) throws IOException, InterruptedException, GeneralSecurityException {
        Files.copy(work.resolve(from + ".p8"), work.resolve(to + ".p8"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.copy(work.resolve(from + ".key"), work.resolve(to + ".key"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Sh.must(work, env, join(List.of("step", "ca", "renew", from + ".crt", from + ".key", "--out", to + ".crt", "--force"), connect));
        return Credential.load(work.resolve(to + ".crt"), work.resolve(to + ".p8"));
    }

    /** Whether the CA renews this certificate (the exit code and the CA's answer are the evidence). */
    Sh.Result tryRenew(String from, String to) throws IOException, InterruptedException {
        return Sh.run(work, env, "", join(List.of("step", "ca", "renew", from + ".crt", from + ".key", "--out", to + ".crt", "--force"), connect));
    }

    @Override
    public void close() throws InterruptedException {
        if (process.isAlive()) {
            stop();
        }
    }
}
