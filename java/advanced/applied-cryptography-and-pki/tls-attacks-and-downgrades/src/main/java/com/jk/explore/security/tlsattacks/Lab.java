package com.jk.explore.security.tlsattacks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Stream;

/** The server key, its certificate and a 1024-bit DH group, in a temporary folder that is deleted at the end. Nothing here is ever written under the project. */
final class Lab implements AutoCloseable {

    private final Path dir;
    private final KeyPair keys;
    private final Openssl openssl;

    Lab(Openssl openssl) throws IOException, InterruptedException, NoSuchAlgorithmException {
        this.openssl = openssl;
        this.dir = Files.createTempDirectory("tls-lab-");
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        this.keys = g.generateKeyPair();
        Files.writeString(dir.resolve("server.key"), pem("PRIVATE KEY", keys.getPrivate().getEncoded()));
        Sh.must(openssl.command("req", "-x509", "-new", "-key", key().toString(), "-subj", "/CN=localhost", "-days", "2",
                "-addext", "subjectAltName=DNS:localhost,IP:127.0.0.1", "-out", dir.resolve("server.crt").toString()));
        Sh.must(openssl.command("genpkey", "-genparam", "-algorithm", "DH", "-pkeyopt", "dh_paramgen_prime_len:1024",
                "-out", dir.resolve("dh1024.pem").toString()));
    }

    /** The header is built from pieces so that no source file contains a private key marker. */
    static String pem(String label, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        String dashes = "-".repeat(5);
        return dashes + "BEGIN " + label + dashes + "\n" + body + "\n" + dashes + "END " + label + dashes + "\n";
    }

    Path key() {
        return dir.resolve("server.key");
    }

    Path cert() {
        return dir.resolve("server.crt");
    }

    Path dhParams() {
        return dir.resolve("dh1024.pem");
    }

    /** The key that "leaks" in the forward secrecy demonstration. */
    KeyPair keyPair() {
        return keys;
    }

    Openssl openssl() {
        return openssl;
    }

    Path scratch(String name) {
        return dir.resolve(name);
    }

    @Override
    public void close() throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
}
