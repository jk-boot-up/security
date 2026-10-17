package com.jk.explore.security.signaturesbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Published test vectors: RFC 8032 (Ed25519, Ed448), RFC 6979 (ECDSA P-256) and RFC 8037 (JWS). */
class VectorsTest {

    @ParameterizedTest
    @CsvSource(value = {
        "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60, d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a, '', "
            + "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
        "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb, 3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c, 72, "
            + "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00"
    })
    void ed25519MatchesRfc8032(String seed, String publicKey, String message, String signature) {
        byte[] msg = message.isEmpty() ? new byte[0] : Hex.parse(message);
        assertEquals(publicKey, Hex.of(EdDsa.public25519(Hex.parse(seed))));
        assertEquals(signature, Hex.of(EdDsa.sign25519(Hex.parse(seed), msg)));
        assertTrue(EdDsa.verify25519(Hex.parse(publicKey), msg, Hex.parse(signature)));
    }

    @Test
    void ed448MatchesRfc8032() {
        byte[] seed = Hex.parse(SignaturesBcDemo.ED448_SEED);
        assertEquals("5fd7449b59b461fd2ce787ec616ad46a1da1342485a70e1f8a0ea75d80e96778edf124769b46c7061bd6783df1e50f6cd1fa1abeafe8256180",
                Hex.of(EdDsa.public448(seed)));
        byte[] signature = EdDsa.sign448(seed, new byte[0]);
        assertEquals(SignaturesBcDemo.ED448_SIGNATURE, Hex.of(signature));
        assertEquals(114, signature.length);
        assertTrue(EdDsa.verify448(EdDsa.public448(seed), new byte[0], signature));
        signature[0] ^= 1;
        assertFalse(EdDsa.verify448(EdDsa.public448(seed), new byte[0], signature));
    }

    @Test
    void ecdsaDeterministicMatchesRfc6979() {
        BigInteger d = new BigInteger(SignaturesBcDemo.P256_KEY, 16);
        Ecdsa.Signature signature = Ecdsa.signDeterministic(d, Hex.bytes("sample"));
        assertEquals(new BigInteger(SignaturesBcDemo.P256_R, 16), signature.r());
        assertEquals(new BigInteger(SignaturesBcDemo.P256_S, 16), signature.s());
        assertTrue(Ecdsa.verify(Ecdsa.publicFor(d), Hex.bytes("sample"), signature));
        assertFalse(Ecdsa.verify(Ecdsa.publicFor(d), Hex.bytes("sampl3"), signature));
    }

    @Test
    void ecdsaRandomizedDiffersEachTimeAndVerifies() {
        BigInteger d = new BigInteger(SignaturesBcDemo.P256_KEY, 16);
        Ecdsa.Signature a = Ecdsa.signRandomized(d, Hex.bytes("sample"));
        Ecdsa.Signature b = Ecdsa.signRandomized(d, Hex.bytes("sample"));
        assertNotEquals(a, b);
        assertTrue(Ecdsa.verify(Ecdsa.publicFor(d), Hex.bytes("sample"), a));
        assertTrue(Ecdsa.verifyDer(Ecdsa.publicFor(d), Hex.bytes("sample"), b.der()));
    }

    @Test
    void aReusedNonceRevealsThePrivateKey() {
        BigInteger d = new BigInteger(SignaturesBcDemo.P256_KEY, 16);
        BigInteger k = new BigInteger("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789", 16);
        var m1 = Hex.bytes("one");
        var m2 = Hex.bytes("two");
        var s1 = Ecdsa.signWithNonce(d, m1, k);
        var s2 = Ecdsa.signWithNonce(d, m2, k);
        assertEquals(s1.r(), s2.r());
        assertEquals(d, Ecdsa.recoverKeyFromReusedNonce(m1, s1, m2, s2));
    }

    @Test
    void differentNoncesDoNotRevealTheKey() {
        BigInteger d = new BigInteger(SignaturesBcDemo.P256_KEY, 16);
        var m1 = Hex.bytes("one");
        var m2 = Hex.bytes("two");
        var s1 = Ecdsa.signDeterministic(d, m1);
        var s2 = Ecdsa.signDeterministic(d, m2);
        assertNotEquals(d, Ecdsa.recoverKeyFromReusedNonce(m1, s1, m2, s2));
    }

    @Test
    void jwsMatchesRfc8037() {
        byte[] seed = Hex.parse(SignaturesBcDemo.RFC8037_SEED);
        byte[] pub = EdDsa.public25519(seed);
        assertEquals(SignaturesBcDemo.RFC8037_TOKEN, Jws.sign(seed, Hex.bytes(SignaturesBcDemo.RFC8037_PAYLOAD)));
        assertTrue(Jws.verify(pub, SignaturesBcDemo.RFC8037_TOKEN, null));
        assertEquals(SignaturesBcDemo.RFC8037_PAYLOAD, Hex.text(Jws.payloadOf(SignaturesBcDemo.RFC8037_TOKEN)));
    }
}
