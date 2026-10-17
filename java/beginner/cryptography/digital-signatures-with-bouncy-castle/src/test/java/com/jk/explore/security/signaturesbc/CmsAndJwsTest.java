package com.jk.explore.security.signaturesbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CmsAndJwsTest {

    private static KeyPair keys;
    private static X509Certificate cert;
    private static final byte[] DATA = Hex.bytes("Statement ANA-1: balance 1200");
    private static final Instant TIME = Instant.parse("2026-03-01T09:30:00Z");

    @BeforeAll
    static void make() {
        keys = Certificates.newEcKeyPair();
        cert = Certificates.selfSigned("Statements", keys);
    }

    @Test
    void attachedAndDetachedVerify() {
        assertTrue(Cms.verify(Cms.sign(cert, keys.getPrivate(), DATA, true, TIME), null, cert));
        assertTrue(Cms.verify(Cms.sign(cert, keys.getPrivate(), DATA, false, TIME), DATA, cert));
    }

    @Test
    void detachedMessageDoesNotContainTheContent() {
        byte[] detached = Cms.sign(cert, keys.getPrivate(), DATA, false, TIME);
        byte[] attached = Cms.sign(cert, keys.getPrivate(), DATA, true, TIME);
        assertTrue(attached.length > detached.length);
        assertFalse(contains(detached, DATA));
        assertTrue(contains(attached, DATA));
    }

    @Test
    void changedContentIsRefused() {
        byte[] detached = Cms.sign(cert, keys.getPrivate(), DATA, false, TIME);
        assertFalse(Cms.verify(detached, Hex.bytes("Statement ANA-1: balance 9200"), cert));
        byte[] attached = Cms.sign(cert, keys.getPrivate(), DATA, true, TIME);
        int at = indexOf(attached, DATA);
        attached[at] ^= 1;
        assertFalse(Cms.verify(attached, null, cert));
    }

    @Test
    void anotherCertificateIsRefused() {
        X509Certificate other = Certificates.selfSigned("Other", Certificates.newEcKeyPair());
        assertFalse(Cms.verify(Cms.sign(cert, keys.getPrivate(), DATA, true, TIME), null, other));
    }

    @Test
    void garbageIsRefusedNotAnError() {
        assertFalse(Cms.verify(new byte[] {1, 2, 3}, null, cert));
    }

    @Test
    void theSigningTimeIsTheOneGivenAndIsProtected() {
        byte[] signed = Cms.sign(cert, keys.getPrivate(), DATA, true, TIME);
        assertEquals(TIME, Cms.claimedSigningTime(signed));
        assertEquals(TIME, Cms.claimedSigningTime(Cms.sign(cert, keys.getPrivate(), DATA, false, TIME)));
    }

    @Test
    void pemRoundTripsAndRejectsTheWrongType() {
        byte[] signed = Cms.sign(cert, keys.getPrivate(), DATA, false, TIME);
        String pem = PemSignatures.write("CMS", signed);
        assertTrue(pem.startsWith("-----BEGIN CMS-----"));
        assertTrue(Arrays.equals(signed, PemSignatures.read("CMS", pem)));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> PemSignatures.read("CERTIFICATE", pem));
    }

    @Test
    void jwsRefusesNoneAndOtherChanges() {
        byte[] seed = EdDsa.newSeed25519();
        byte[] pub = EdDsa.public25519(seed);
        byte[] payload = Hex.bytes("{\"order\":1}");
        String token = Jws.sign(seed, payload);
        assertTrue(Jws.verify(pub, token, null));
        String[] parts = token.split("\\.");
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        assertFalse(Jws.verify(pub, enc.encodeToString(Hex.bytes("{\"alg\":\"none\"}")) + "." + parts[1] + ".", null));
        assertFalse(Jws.verify(pub, parts[0] + "." + enc.encodeToString(Hex.bytes("{\"order\":2}")) + "." + parts[2], null));
        assertFalse(Jws.verify(EdDsa.public25519(EdDsa.newSeed25519()), token, null));
        assertFalse(Jws.verify(pub, "not a token", null));
        assertFalse(Jws.verify(pub, token, payload));
    }

    @Test
    void jwsDetachedNeedsThePayload() {
        byte[] seed = EdDsa.newSeed25519();
        byte[] pub = EdDsa.public25519(seed);
        byte[] payload = Hex.bytes("abc");
        String token = Jws.signDetached(seed, payload);
        assertTrue(Jws.verify(pub, token, payload));
        assertFalse(Jws.verify(pub, token, Hex.bytes("abd")));
    }

    private static boolean contains(byte[] data, byte[] pattern) {
        try {
            indexOf(data, pattern);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        throw new IllegalStateException("not found");
    }
}
