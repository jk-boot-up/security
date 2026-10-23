package com.jk.explore.security.pqc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SizesTest {

    @Test
    void mlKem768MatchesFips203Lengths() throws Exception {
        Sizes.Exchange k = Sizes.exchange(Sizes.exchanges(), "ML-KEM-768");
        assertEquals(1088, k.reply());
        assertEquals(32, k.secret());
        // FIPS 203: encapsulation key 1184 bytes, plus a 22-byte X.509 header.
        assertEquals(1184 + 22, k.publicKey());
    }

    @Test
    void mlDsa65MatchesFips204Lengths() throws Exception {
        Sizes.Signing s = Sizes.signing(Sizes.signings(), "ML-DSA-65");
        assertEquals(3309, s.signature());
        assertEquals(1952 + 22, s.publicKey());
    }

    @Test
    void classicalSizesAreTheKnownOnes() throws Exception {
        assertEquals(44, Sizes.exchange(Sizes.exchanges(), "X25519").publicKey());
        assertEquals(384, Sizes.signing(Sizes.signings(), "RSA-3072 PKCS#1").signature());
        assertEquals(64, Sizes.signing(Sizes.signings(), "ECDSA P-256").signature());
    }

    @Test
    void ratioIsFormattedWithOneDecimal() {
        assertEquals("27.4", Sizes.times(1206, 44));
        assertEquals("2.0", Sizes.times(2, 1));
    }
}
