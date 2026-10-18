package com.jk.explore.security.signing;

import java.util.List;

/**
 * The point of use: the step that decides whether an artifact runs. The trusting gate deploys whatever arrives. The checking gate
 * verifies the signature and, when told to, also requires proof that the signature is in the public log, and takes the log's day as
 * the signing day for revocation, because the signer can write any day it likes into its own statement.
 */
final class DeployGate {

    private final Verifier verifier;
    private final TransparencyLog log;
    private final KeyRing keys;

    DeployGate(KeyRing keys, TransparencyLog log) {
        this.verifier = new Verifier(keys);
        this.log = log;
        this.keys = keys;
    }

    static Verdict trusting(Artifact artifact) {
        return Verdict.accept("deployed " + artifact.fileName() + " without checking it");
    }

    Verdict check(Artifact artifact, SignedRelease release, String name, String channel) {
        return verifier.verify(artifact, release, name, channel);
    }

    /** Signature checks, then inclusion in the log by a proof against the log's signed head, then revocation by the log's day. */
    Verdict checkWithLog(Artifact artifact, SignedRelease release, String name, String channel) {
        Verdict signature = verifier.verify(artifact, release, name, channel);
        if (!signature.accepted()) {
            return signature;
        }
        int index = log.indexOf(release);
        if (index < 0) {
            return Verdict.refuse("the signature is not in the public log");
        }
        TransparencyLog.TreeHead head = log.head();
        if (!TransparencyLog.headIsSigned(head, log.publicKey())) {
            return Verdict.refuse("the log's tree head is not signed by the log");
        }
        List<byte[]> proof = log.inclusionProof(index, head.size());
        if (!MerkleTree.verifyInclusion(MerkleTree.leafHash(release.logEntry()), index, head.size(), proof, head.root())) {
            return Verdict.refuse("the log's inclusion proof does not check");
        }
        KeyRing.TrustedKey key = keys.find(release.keyId()).orElseThrow();
        int loggedDay = log.dayOf(index);
        if (loggedDay >= key.revokedFromDay()) {
            return Verdict.refuse("logged on day " + loggedDay + ", after key " + key.keyId() + " was revoked on day " + key.revokedFromDay());
        }
        return Verdict.accept(signature.reason() + ", logged as entry " + index);
    }
}
