package com.jk.explore.security.provenance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import org.junit.jupiter.api.Test;

/** Ed25519 against RFC 8032 section 7.1, test 1: a key from its seed, its public key, and the signature of the empty message. */
class Ed25519Test {

    private static final String SEED = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60";
    private static final String PUBLIC = "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a";
    private static final String SIGNATURE = "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b";

    @Test
    void theRfcSeedGivesTheRfcPublicKeyAndSignature() {
        KeyPair pair = Ed25519.fromSeed(Digests.unhex(SEED));
        assertArrayEquals(Digests.unhex(PUBLIC), Ed25519.raw(pair.getPublic()));
        assertArrayEquals(Digests.unhex(SIGNATURE), Ed25519.sign(pair.getPrivate(), new byte[0]));
        assertTrue(Ed25519.verify(pair.getPublic(), new byte[0], Digests.unhex(SIGNATURE)));
    }

    @Test
    void aChangedMessageOrSignatureFails() {
        KeyPair pair = Ed25519.fromSeed(Digests.unhex(SEED));
        byte[] signature = Digests.unhex(SIGNATURE);
        assertFalse(Ed25519.verify(pair.getPublic(), new byte[] {1}, signature));
        signature[0] ^= 1;
        assertFalse(Ed25519.verify(pair.getPublic(), new byte[0], signature));
        assertFalse(Ed25519.verify(pair.getPublic(), new byte[0], new byte[3]));
    }

    @Test
    void signingIsDeterministic() {
        Builder builder = Builder.demo("b", "k", 3);
        byte[] message = {1, 2, 3};
        assertArrayEquals(Ed25519.sign(builder.key().getPrivate(), message), Ed25519.sign(builder.key().getPrivate(), message));
    }
}
