package com.jk.explore.security.tlslog;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TlsLogTest {

    @Test
    void hkdfMatchesRfc5869TestCase1() {
        byte[] ikm = new byte[22];
        java.util.Arrays.fill(ikm, (byte) 0x0b);
        byte[] salt = Hex.parse("000102030405060708090a0b0c");
        byte[] info = Hex.parse("f0f1f2f3f4f5f6f7f8f9");
        byte[] prk = Hkdf.extract(salt, ikm);
        assertEquals("077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5", Hex.of(prk));
        assertEquals("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865", Hex.of(Hkdf.expand(prk, info, 42)));
    }

    @Test
    void recordsRoundTripAndRefuseTheWrongKeyNumberOrChanges() {
        byte[] secret = Hkdf.sha256(new byte[] {1});
        byte[] rec = Records.seal(secret, 5, "hello");
        assertEquals("hello", Records.open(secret, 5, rec));
        assertNull(Records.open(secret, 6, rec), "the record number is part of the nonce");
        assertNull(Records.open(Hkdf.sha256(new byte[] {2}), 5, rec));
        rec[rec.length - 1] ^= 1;
        assertNull(Records.open(secret, 5, rec));
        assertEquals(0x17, Records.seal(secret, 0, "x")[0]);
    }

    @Test
    void theWireNeverShowsThePlaintextOrTheContentType() {
        Capture c = Connection.ecdhe("t1", TlsLogDemo.LOGIN, TlsLogDemo.LOGIN_REPLY).capture();
        for (Packet p : c.packets()) {
            String s = new String(p.bytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
            assertFalse(s.contains("password") || s.contains("POST") || s.contains("hunter2"));
            assertEquals(0x17, p.bytes()[0]);
        }
        assertEquals(List.of(), Decryptor.withoutKeys(c));
    }

    @Test
    void bothSidesDeriveTheSameSecretsAndTheyDifferByDirection() {
        var e = Connection.ecdhe("t2", List.of("a"), List.of("b"));
        assertNotEquals(Hex.of(e.secrets().client()), Hex.of(e.secrets().server()));
        assertEquals(32, e.secrets().client().length);
        assertEquals(Hex.of(Connection.ecdhe("t2", List.of("a"), List.of("b")).secrets().client()), Hex.of(e.secrets().client()));
        assertNotEquals(Hex.of(Connection.ecdhe("t3", List.of("a"), List.of("b")).secrets().client()), Hex.of(e.secrets().client()));
    }

    @Test
    void aKeyLogDecryptsItsOwnConnectionOnly() {
        var one = Connection.ecdhe("k1", List.of("one"), List.of("uno"));
        var two = Connection.ecdhe("k2", List.of("two"), List.of("dos"));
        String log = KeyLog.lines(one.capture(), one.secrets());
        assertEquals(List.of("client: one", "server: uno"), Decryptor.withKeyLog(one.capture(), log));
        assertEquals(List.of(), Decryptor.withKeyLog(two.capture(), log));
        assertEquals(List.of("client: one"), Decryptor.withKeyLog(one.capture(), log.lines().filter(l -> l.startsWith("CLIENT")).findFirst().orElseThrow()));
        assertEquals(List.of("client: two", "server: dos"), Decryptor.withKeyLog(two.capture(), log + KeyLog.lines(two.capture(), two.secrets())));
    }

    @Test
    void keyLogLinesUseTheNssFormat() {
        var e = Connection.ecdhe("k3", List.of("x"), List.of("y"));
        String[] first = KeyLog.lines(e.capture(), e.secrets()).split("\n")[0].split(" ");
        assertEquals("CLIENT_TRAFFIC_SECRET_0", first[0]);
        assertEquals(64, first[1].length());
        assertEquals(64, first[2].length());
        assertEquals(Optional.empty(), KeyLog.find("garbage\nCLIENT_TRAFFIC_SECRET_0 zz", "CLIENT_TRAFFIC_SECRET_0", e.capture().clientRandom()));
    }

    @Test
    void theServerLongTermKeyOpensStaticRsaButNotEphemeralConnections() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair server = g.generateKeyPair();
        var rsa = Connection.staticRsa("old", server.getPublic(), List.of("secret request"), List.of("secret reply"));
        assertEquals(List.of("client: secret request", "server: secret reply"), Decryptor.withServerLongTermKey(rsa.capture(), server.getPrivate()));
        var dh = Connection.ecdhe("new", List.of("secret request"), List.of("secret reply"));
        assertEquals(List.of(), Decryptor.withServerLongTermKey(dh.capture(), server.getPrivate()));
        KeyPair other = g.generateKeyPair();
        assertEquals(List.of(), Decryptor.withServerLongTermKey(rsa.capture(), other.getPrivate()));
    }

    @Test
    void theKeyLogVariableIsRefusedInProduction() {
        assertEquals(Optional.empty(), KeyLog.pathFor(Map.of()));
        assertEquals(Optional.empty(), KeyLog.pathFor(Map.of("SSLKEYLOGFILE", " ")));
        assertEquals(Optional.of("/tmp/k"), KeyLog.pathFor(Map.of("SSLKEYLOGFILE", "/tmp/k", "APP_ENV", "staging")));
        assertThrows(IllegalStateException.class, () -> KeyLog.pathFor(Map.of("SSLKEYLOGFILE", "/tmp/k", "APP_ENV", "Production")));
    }

    @Test
    void x25519PublicKeysSurviveTheRawEncoding() throws Exception {
        var e = Connection.ecdhe("raw", List.of("a"), List.of("b"));
        byte[] raw = e.capture().serverKeyShare();
        assertEquals(32, raw.length);
        assertArrayEquals(raw, Connection.raw(Connection.publicKeyFromRaw(raw)));
    }

    @Test
    void labelledRandomIsRepeatable() {
        byte[] a = new byte[40];
        byte[] b = new byte[40];
        new LabelledRandom("x").nextBytes(a);
        new LabelledRandom("x").nextBytes(b);
        assertArrayEquals(a, b);
        assertTrue(a[39] != 0 || a[38] != 0 || a[37] != 0);
    }
}
