package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** The last step: a directory that stands for production, which takes a jar only when its release verifies. */
final class Deploy {

    private final Path production;
    private final Path trustedPublicKey;
    private final Verifier.Policy policy;
    private final SignatureCheck signatures;

    Deploy(Path production, Path trustedPublicKey, Verifier.Policy policy, SignatureCheck signatures) {
        this.production = production;
        this.trustedPublicKey = trustedPublicKey;
        this.policy = policy;
        this.signatures = signatures;
    }

    /** Accepts the release (signature, provenance and digests all verify) or says which check refused it. */
    Verifier.Verdict deploy(Bundle release) throws IOException {
        Verifier.Verdict verdict = Verifier.verify(release, trustedPublicKey, policy, signatures);
        if (verdict.ok()) {
            Files.createDirectories(production);
            Files.copy(release.jar(), production.resolve(Bundle.JAR), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return verdict;
    }

    int deployed() throws IOException {
        if (!Files.isDirectory(production)) {
            return 0;
        }
        try (var list = Files.list(production)) {
            return (int) list.count();
        }
    }
}
