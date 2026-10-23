package com.jk.explore.security.mtls;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** A real private certificate authority: step-ca running on this machine, driven with the step command line. Also makes a second, unrelated authority with step. */
final class Ca implements AutoCloseable {

    private final Path work;
    private final Path home;
    private final Map<String, String> env;
    private final String url;
    private final List<String> connect;
    private final Process process;

    Ca(Path dir) throws IOException, InterruptedException {
        this.work = Files.createDirectories(dir.toAbsolutePath());
        this.home = this.work.resolve("home");
        this.env = Map.of("STEPPATH", home.toString());
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        url = "https://localhost:" + port;
        Path password = this.work.resolve("password");
        Files.writeString(password, HexFormat.of().formatHex(new SecureRandom().generateSeed(16)));
        Sh.must(this.work, env, List.of("step", "ca", "init", "--name", "Bank Internal CA", "--dns", "localhost", "--address", "127.0.0.1:" + port,
                "--provisioner", "admin", "--password-file", password.toString(), "--provisioner-password-file", password.toString(), "--deployment-type", "standalone"));
        tune(home.resolve("config/ca.json"));
        connect = List.of("--ca-url", url, "--root", root().toString());
        process = new ProcessBuilder("step-ca", home.resolve("config/ca.json").toString(), "--password-file", password.toString())
                .directory(this.work.toFile()).redirectErrorStream(true).redirectOutput(this.work.resolve("ca.log").toFile()).start();
        for (int i = 0; ; i++) {
            if (Sh.run(this.work, env, concat(List.of("step", "ca", "health"), connect)).output().trim().equals("ok")) {
                break;
            }
            if (i > 100) {
                throw new IllegalStateException("step-ca did not start");
            }
            Thread.sleep(100);
        }
    }

    /** Short lifetimes allowed (down to one second), certificates valid from the moment of issue, and the revocation list switched on. */
    private static void tune(Path caJson) throws IOException {
        ObjectMapper mapper = JsonMapper.builder().build();
        JsonNode parsed = mapper.readTree(Files.readString(caJson));
        ObjectNode root = (ObjectNode) parsed;
        ObjectNode authority = (ObjectNode) root.get("authority");
        authority.put("backdate", "0s");
        ObjectNode claims = authority.putObject("claims");
        claims.put("minTLSCertDuration", "1s");
        claims.put("maxTLSCertDuration", "24h");
        claims.put("defaultTLSCertDuration", "1h");
        ObjectNode crl = root.putObject("crl");
        crl.put("enabled", true);
        crl.put("generateOnRevoke", true);
        crl.put("cacheDuration", "1m");
        Files.writeString(caJson, mapper.writeValueAsString(root));
    }

    private static List<String> concat(List<String> a, List<String> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
    }

    Path root() {
        return home.resolve("certs/root_ca.crt");
    }

    private Path intermediate() {
        return home.resolve("certs/intermediate_ca.crt");
    }

    /** A certificate for a name (the subject), with the service identity as a URI in the SAN and optional host names, valid for the given seconds. */
    Credential issue(String base, String subject, List<String> sans, int seconds) throws IOException, InterruptedException, GeneralSecurityException {
        List<String> cmd = new java.util.ArrayList<>(List.of("step", "ca", "certificate", subject, base + ".crt", base + ".key",
                "--provisioner", "admin", "--provisioner-password-file", "password", "--not-after", seconds + "s", "--force"));
        for (String san : sans) {
            cmd.add("--san");
            cmd.add(san);
        }
        Sh.must(work, env, concat(cmd, connect));
        return pkcs8(base);
    }

    /** Asks the CA for a new certificate for the same key, written to another file; the old one stays valid until its own expiry. */
    Credential renew(String from, String to) throws IOException, InterruptedException, GeneralSecurityException {
        Files.copy(work.resolve(from + ".key"), work.resolve(to + ".key"), StandardCopyOption.REPLACE_EXISTING);
        Sh.must(work, env, concat(List.of("step", "ca", "renew", from + ".crt", from + ".key", "--out", to + ".crt", "--force"), connect));
        return pkcs8(to);
    }

    /** Revokes with the CA; the CA only records it. */
    void revoke(String base) throws IOException, InterruptedException {
        Sh.must(work, env, concat(List.of("step", "ca", "revoke", "--cert", base + ".crt", "--key", base + ".key"), connect));
    }

    /** The serial numbers on the CA's revocation list, checked against the intermediate's signature. */
    Set<BigInteger> revokedSerials() throws IOException, InterruptedException, GeneralSecurityException {
        try (HttpClient client = HttpClient.newBuilder().sslContext(Tls.context(null, Credential.readCertificates(root()))).build()) {
            HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(URI.create(url + "/1.0/crl")).build(), HttpResponse.BodyHandlers.ofByteArray());
            X509CRL crl = (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(new java.io.ByteArrayInputStream(response.body()));
            crl.verify(Credential.readCertificates(intermediate()).getFirst().getPublicKey());
            if (crl.getRevokedCertificates() == null) {
                return Set.of();
            }
            return crl.getRevokedCertificates().stream().map(X509CRLEntry::getSerialNumber).collect(Collectors.toSet());
        }
    }

    /** A certificate from a different, unrelated authority (made with step certificate create), claiming the given identity. Also writes its root to otherRoot(). */
    Credential issueFromOtherCa(String base, String subject, String san) throws IOException, InterruptedException, GeneralSecurityException {
        Path caCrt = work.resolve("other-ca.crt");
        Path caKey = work.resolve("other-ca.key");
        if (!Files.exists(caCrt)) {
            Sh.must(work, env, List.of("step", "certificate", "create", "Partner Bank CA", caCrt.toString(), caKey.toString(), "--profile", "root-ca", "--no-password", "--insecure", "--force"));
        }
        Sh.must(work, env, List.of("step", "certificate", "create", subject, base + ".crt", base + ".key", "--profile", "leaf", "--ca", caCrt.toString(), "--ca-key", caKey.toString(),
                "--san", san, "--not-after", "1h", "--no-password", "--insecure", "--force"));
        return pkcs8(base);
    }

    Path otherRoot() {
        return work.resolve("other-ca.crt");
    }

    private Credential pkcs8(String base) throws IOException, InterruptedException, GeneralSecurityException {
        Sh.must(work, env, List.of("step", "crypto", "key", "format", "--pkcs8", "--no-password", "--insecure", "--force", "--out", base + ".p8", base + ".key"));
        return Credential.load(work.resolve(base + ".crt"), work.resolve(base + ".p8"));
    }

    Path file(String name) {
        return work.resolve(name);
    }

    @Override
    public void close() {
        process.destroy();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    X509Certificate rootCertificate() throws IOException, GeneralSecurityException {
        return Credential.readCertificates(root()).getFirst();
    }
}
