package com.jk.explore.security.tlscapture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A root made with the step command, and the certificates it signs, good and bad. */
final class LocalCa {

    private final Path dir;

    LocalCa(Path dir) throws IOException, InterruptedException {
        this.dir = Files.createDirectories(dir);
        Procs.run(List.of("step", "certificate", "create", "Shop Lab Root", root().toString(), key("root").toString(), "--profile", "root-ca", "--no-password", "--insecure", "--force"));
    }

    Path root() {
        return dir.resolve("root.crt");
    }

    Path key(String name) {
        return dir.resolve(name + ".key");
    }

    Path cert(String name) {
        return dir.resolve(name + ".crt");
    }

    /**
     * A server certificate named {@code names}, signed by the root. {@code notBefore} and {@code notAfter} are durations from now (for example -72h and 2160h);
     * {@code rsa} makes an RSA 2048 key, otherwise the key is P-256.
     */
    void issue(String file, String notBefore, String notAfter, boolean rsa, String... names) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("step", "certificate", "create", names[0], cert(file).toString(), key(file).toString(), "--profile", "leaf", "--ca", root().toString(),
                "--ca-key", key("root").toString(), "--no-password", "--insecure", "--not-before", notBefore, "--not-after", notAfter, "--force"));
        if (rsa) {
            command.addAll(List.of("--kty", "RSA", "--size", "2048"));
        }
        for (String name : names) {
            command.addAll(List.of("--san", name));
        }
        Procs.run(command);
    }

    /** A PKCS#12 file for the JDK. */
    Path pkcs12(String file) throws IOException, InterruptedException {
        Path p12 = dir.resolve(file + ".p12");
        Procs.run(List.of("openssl", "pkcs12", "-export", "-in", cert(file).toString(), "-inkey", key(file).toString(), "-out", p12.toString(), "-passout", "pass:changeit"));
        return p12;
    }
}
