package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** The real cosign command line, with a key pair that lives in a temporary directory and nowhere else. */
final class Cosign implements SignatureCheck {

    /** A key pair in a directory. */
    record Keys(Path dir) {

        Path privateKey() {
            return dir.resolve("cosign.key");
        }

        Path publicKey() {
            return dir.resolve("cosign.pub");
        }
    }

    private static final Map<String, String> ENV = Map.of("COSIGN_PASSWORD", "");

    static Keys generate() throws IOException, InterruptedException {
        Path dir = Scratch.dir("cosignkeys");
        Sh.must(dir, ENV, List.of("cosign", "generate-key-pair"));
        return new Keys(dir);
    }

    static void sign(Keys keys, Path file, Path signature) throws IOException, InterruptedException {
        Sh.must(null, ENV, List.of("cosign", "sign-blob", "--key", keys.privateKey().toString(), "--tlog-upload=false", "--use-signing-config=false",
                "--new-bundle-format=false", "--yes", "--output-signature", signature.toString(), file.toString()));
    }

    @Override
    public boolean verify(Path publicKey, Path file, Path signature) {
        try {
            return Sh.run(null, ENV, List.of("cosign", "verify-blob", "--key", publicKey.toString(), "--signature", signature.toString(),
                    "--insecure-ignore-tlog=true", file.toString())).ok();
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Deletes the key pair. */
    static void destroy(Keys keys) throws IOException {
        Files.deleteIfExists(keys.privateKey());
        Files.deleteIfExists(keys.publicKey());
        Files.deleteIfExists(keys.dir());
    }
}
