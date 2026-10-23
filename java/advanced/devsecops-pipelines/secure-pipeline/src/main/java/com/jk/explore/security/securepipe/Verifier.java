package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import tools.jackson.databind.JsonNode;

/** Everything that must be true before a release is trusted. The first check that fails is the answer; anything unexpected is a failure too. */
final class Verifier {

    /** What the verifier believes before it looks at the release: the builder it trusts and the commit it wants. */
    record Policy(String builderId, String commit, String tree) {
    }

    /** The answer and, when it is no, the check that said so. */
    record Verdict(boolean ok, String failed) {

        static Verdict yes() {
            return new Verdict(true, "");
        }

        static Verdict no(String check) {
            return new Verdict(false, check);
        }
    }

    private Verifier() {
    }

    static Verdict verify(Bundle b, Path publicKey, Policy policy, SignatureCheck signatures) {
        try {
            for (Path p : new Path[] {b.jar(), b.jarSignature(), b.provenance(), b.provenanceSignature(), publicKey}) {
                if (!Files.isRegularFile(p)) {
                    return Verdict.no("files present");
                }
            }
            if (!signatures.verify(publicKey, b.jar(), b.jarSignature())) {
                return Verdict.no("artifact signature");
            }
            if (!signatures.verify(publicKey, b.provenance(), b.provenanceSignature())) {
                return Verdict.no("provenance signature");
            }
            JsonNode statement = Provenance.read(b.provenance());
            if (!Provenance.subjectDigest(statement).equals(Digest.sha256(b.jar()))) {
                return Verdict.no("artifact digest");
            }
            if (!Provenance.builderId(statement).equals(policy.builderId())) {
                return Verdict.no("builder");
            }
            if (!Provenance.sourceCommit(statement).equals(policy.commit())) {
                return Verdict.no("source commit");
            }
            if (!Provenance.sourceTree(statement).equals(policy.tree())) {
                return Verdict.no("source tree");
            }
            return Verdict.yes();
        } catch (IOException | RuntimeException e) {
            return Verdict.no("readable provenance");
        }
    }
}
