package com.jk.explore.security.stepca;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** A real step-ca in a folder of its own, and the real step command as its client. */
public final class StepCa implements AutoCloseable {

    /** What one step command printed, and whether it worked. */
    record Outcome(boolean ok, String output) {

        /** The CA's reason, in one short sentence. */
        String reason() {
            if (ok) {
                return "";
            }
            if (output.contains("not allowed") && output.contains("dns name")) {
                return "that name is outside what the CA's policy allows";
            }
            if (output.contains("authorized maximum certificate duration")) {
                return "longer than the longest lifetime the CA allows";
            }
            if (output.contains("will not issue certificates for the identifier")) {
                return "the ACME server will not issue for that name";
            }
            if (output.contains("do not match")) {
                return "the token was issued for another name";
            }
            if (output.contains("remote error: tls")) {
                return "the key does not match the certificate, so the TLS call to the CA fails";
            }
            if (output.contains("lacked necessary authorization")) {
                return "not authorized";
            }
            return "failed";
        }
    }

    static final String PASSWORD = "lab-password-only";

    private final Path home;
    private final int port;
    private final Process process;

    public StepCa() throws IOException, InterruptedException {
        this.home = Files.createTempDirectory("stepca");
        try (ServerSocket s = new ServerSocket(0)) {
            this.port = s.getLocalPort();
        }
        Files.writeString(home.resolve("password"), PASSWORD);
        require(step("ca", "init", "--name", "Shop Internal CA", "--dns", "localhost", "--address", "127.0.0.1:" + port, "--provisioner", "tokens",
                "--password-file", home.resolve("password").toString(), "--provisioner-password-file", home.resolve("password").toString(), "--deployment-type", "standalone"));
        require(step("ca", "provisioner", "add", "acme", "--type", "ACME"));
        configure();
        this.process = new ProcessBuilder("step-ca", home.resolve("config/ca.json").toString(), "--password-file", home.resolve("password").toString())
                .redirectErrorStream(true).redirectOutput(home.resolve("ca.log").toFile()).start();
        waitUntilHealthy();
    }

    /** A CRL on, and a policy: only localhost and names under shop.localhost may be certified. */
    private void configure() throws IOException {
        Path file = home.resolve("config/ca.json");
        ObjectMapper json = new ObjectMapper();
        ObjectNode ca = (ObjectNode) json.readTree(file.toFile());
        ObjectNode crl = ca.putObject("crl");
        crl.put("enabled", true).put("generateOnRevoke", true).put("cacheDuration", "5m").put("idpURL", "https://localhost:" + port + "/1.0/crl");
        ArrayNode dns = ((ObjectNode) ((ObjectNode) ((ObjectNode) ca.get("authority")).putObject("policy")).putObject("x509").putObject("allow")).putArray("dns");
        dns.add("localhost").add("*.shop.localhost");
        json.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), ca);
    }

    private void waitUntilHealthy() throws IOException, InterruptedException {
        for (int i = 0; i < 100; i++) {
            try {
                if (get("/health").body().contains("ok")) {
                    return;
                }
            } catch (IOException e) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("step-ca did not start: " + Files.readString(home.resolve("ca.log")));
    }

    Path home() {
        return home;
    }

    Path root() {
        return home.resolve("certs/root_ca.crt");
    }

    Path intermediate() {
        return home.resolve("certs/intermediate_ca.crt");
    }

    Path rootKey() {
        return home.resolve("secrets/root_ca_key");
    }

    String url() {
        return "https://localhost:" + port;
    }

    /** A GET that trusts the CA's own root, and nothing else. */
    HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client(Pki.contextTrusting(root())).send(HttpRequest.newBuilder(URI.create(url() + path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<byte[]> getBytes(String path) throws IOException, InterruptedException {
        return client(Pki.contextTrusting(root())).send(HttpRequest.newBuilder(URI.create(url() + path)).build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpClient client(javax.net.ssl.SSLContext context) {
        return HttpClient.newBuilder().sslContext(context).build();
    }

    /** Runs step with this CA's home folder. */
    Outcome step(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("step"));
        command.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        env.put("STEPPATH", home.toString());
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(120, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("step did not finish: " + command);
        }
        return new Outcome(p.exitValue() == 0, out);
    }

    private static void require(Outcome o) {
        if (!o.ok()) {
            throw new IllegalStateException(o.output());
        }
    }

    /** A one-time token that lets the holder ask for a certificate for these names. */
    String token(String... names) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("ca", "token", names[0], "--provisioner", "tokens", "--password-file", home.resolve("password").toString(),
                "--provisioner-password-file", home.resolve("password").toString()));
        for (int i = 1; i < names.length; i++) {
            args.addAll(List.of("--san", names[i]));
        }
        Outcome o = step(args.toArray(String[]::new));
        require(o);
        return o.output().lines().filter(l -> l.startsWith("eyJ")).findFirst().orElseThrow();
    }

    /** Asks for a certificate with a token; the files are {@code name.crt} and {@code name.key} in the given folder. */
    Outcome issueWithToken(Path folder, String file, String token, String subject, String... extra) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("ca", "certificate", subject, folder.resolve(file + ".crt").toString(), folder.resolve(file + ".key").toString(), "--token", token, "--force"));
        args.addAll(List.of(extra));
        return step(args.toArray(String[]::new));
    }

    /** Asks for a certificate through the ACME provisioner; the CA calls the name back on port 80 to check it. */
    Outcome issueWithAcme(Path folder, String file, String subject) throws IOException, InterruptedException {
        return step("ca", "certificate", subject, folder.resolve(file + ".crt").toString(), folder.resolve(file + ".key").toString(), "--provisioner", "acme", "--force");
    }

    Outcome renew(Path crt, Path key) throws IOException, InterruptedException {
        return step("ca", "renew", crt.toString(), key.toString(), "--force");
    }

    Outcome rekey(Path crt, Path key) throws IOException, InterruptedException {
        return step("ca", "rekey", crt.toString(), key.toString(), "--force");
    }

    Outcome revoke(Path crt, Path key) throws IOException, InterruptedException {
        return step("ca", "revoke", "--cert", crt.toString(), "--key", key.toString());
    }

    /** The revocation list the CA publishes, as DER bytes. */
    byte[] crl() throws IOException, InterruptedException {
        return getBytes("/1.0/crl").body();
    }

    @Override
    public void close() {
        process.destroy();
        try {
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
