package com.jk.explore.security.provenance;

import java.security.KeyPair;

/**
 * A build service: it builds a source into an artifact reproducibly and signs a provenance statement about it with its own key.
 * The key belongs to the build platform, not to the people whose code it builds.
 */
record Builder(String id, String keyId, KeyPair key) {

    record Build(String artifactName, byte[] artifact, String provenance) {
    }

    Build build(Source source, String artifactName, String invocationId) {
        byte[] artifact = Archive.reproducible(source.files());
        byte[] statement = Provenance.statement(artifactName, artifact, source, id, invocationId);
        return new Build(artifactName, artifact, Dsse.envelope(statement, keyId, key.getPrivate()));
    }

    static Builder demo(String id, String keyId, int seedByte) {
        byte[] seed = new byte[32];
        java.util.Arrays.fill(seed, (byte) seedByte);
        return new Builder(id, keyId, Ed25519.fromSeed(seed));
    }
}
