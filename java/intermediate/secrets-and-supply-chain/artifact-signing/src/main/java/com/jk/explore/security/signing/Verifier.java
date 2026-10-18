package com.jk.explore.security.signing;

/**
 * Checks a release before it is deployed, in this order: the statement is well formed; it is signed by a known key for this channel,
 * valid on the day it says it was signed; the signature matches; and the artifact's bytes match the signed digest and the expected name.
 */
final class Verifier {

    private final KeyRing keys;

    Verifier(KeyRing keys) {
        this.keys = keys;
    }

    Verdict verify(Artifact artifact, SignedRelease release, String expectedName, String expectedChannel) {
        if (release == null) {
            return Verdict.refuse("no signature");
        }
        Statement statement;
        try {
            statement = Statement.parse(release.statement());
        } catch (IllegalArgumentException e) {
            return Verdict.refuse("the statement is not a release statement");
        }
        KeyRing.TrustedKey key = keys.find(release.keyId()).orElse(null);
        if (key == null) {
            return Verdict.refuse("unknown key " + release.keyId());
        }
        if (!Ed25519.verify(key.publicKey(), release.statement(), release.signature())) {
            return Verdict.refuse("the signature does not match");
        }
        if (!key.channel().equals(expectedChannel) || !statement.channel().equals(expectedChannel)) {
            return Verdict.refuse("key " + key.keyId() + " signs for " + key.channel() + ", not " + expectedChannel);
        }
        if (statement.signedDay() >= key.revokedFromDay()) {
            return Verdict.refuse("signed on day " + statement.signedDay() + ", after key " + key.keyId() + " was revoked on day " + key.revokedFromDay());
        }
        if (statement.signedDay() < key.validFromDay()) {
            return Verdict.refuse("signed on day " + statement.signedDay() + ", before key " + key.keyId() + " was valid");
        }
        if (!statement.name().equals(expectedName)) {
            return Verdict.refuse("signed for " + statement.name() + ", not " + expectedName);
        }
        if (!Digests.same(Digests.unhex(statement.sha256()), artifact.sha256())) {
            return Verdict.refuse("the artifact does not match the signed digest");
        }
        return Verdict.accept("signed by " + key.keyId() + " for " + statement.name() + " " + statement.version());
    }
}
