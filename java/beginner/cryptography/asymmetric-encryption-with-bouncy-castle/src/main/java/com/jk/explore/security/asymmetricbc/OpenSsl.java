package com.jk.explore.security.asymmetricbc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Calls the openssl command-line tool, a different program written by different people, to open a CMS envelope. If it can read
 * what this code wrote, the envelope really is in the standard format and not just something only this library understands.
 */
public final class OpenSsl {

    private OpenSsl() {
    }

    /** True if an openssl that understands CMS is on the PATH. */
    public static boolean available() {
        try {
            return run(List.of("openssl", "cms", "-help"), null).exit() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /** What openssl printed, and how it ended. */
    public record Result(int exit, byte[] output) {
    }

    /** Asks openssl to decrypt a DER envelope using a private key file and the recipient's certificate file. */
    public static Result decrypt(byte[] envelopeDer, String privateKeyPem, String certificatePem) throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory("cms-demo");
        try {
            Path envelope = dir.resolve("envelope.der");
            Path key = dir.resolve("ana.key.pem");
            Path cert = dir.resolve("ana.crt.pem");
            Files.write(envelope, envelopeDer);
            Files.writeString(key, privateKeyPem);
            Files.writeString(cert, certificatePem);
            return run(List.of("openssl", "cms", "-decrypt", "-inform", "DER", "-in", envelope.toString(),
                    "-inkey", key.toString(), "-recip", cert.toString()), dir);
        } finally {
            try (var files = Files.list(dir)) {
                for (Path p : files.toList()) {
                    Files.deleteIfExists(p);
                }
            }
            Files.deleteIfExists(dir);
        }
    }

    private static Result run(List<String> command, Path dir) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        byte[] out = p.getInputStream().readAllBytes();
        if (!p.waitFor(60, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            return new Result(-1, out);
        }
        return new Result(p.exitValue(), out);
    }
}
