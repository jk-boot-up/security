package com.jk.explore.security.signing;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;

/** A signing key with an id and the release channel it may sign for. In the demo it is made from a fixed seed; really it lives in a hardware module. */
record SigningKey(String keyId, String channel, KeyPair pair) {

    static SigningKey demo(String keyId, String channel) {
        return demo(keyId, channel, keyId);
    }

    /** A key whose seed is named separately from its id: how a forger makes a key that claims someone else's id. */
    static SigningKey demo(String keyId, String channel, String seedName) {
        byte[] seed = Digests.sha256(("demo signing seed, not a real key: " + seedName).getBytes(StandardCharsets.UTF_8));
        return new SigningKey(keyId, channel, Ed25519.fromSeed(seed));
    }

    PublicKey publicKey() {
        return pair.getPublic();
    }

    SignedRelease sign(Statement statement) {
        byte[] bytes = statement.bytes();
        return new SignedRelease(bytes, keyId, Ed25519.sign(pair.getPrivate(), bytes));
    }
}
