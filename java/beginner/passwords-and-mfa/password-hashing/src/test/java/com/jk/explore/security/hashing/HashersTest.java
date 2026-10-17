package com.jk.explore.security.hashing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HashersTest {

    private final WorkMeter meter = new WorkMeter();

    @Test
    void sha256MatchesTheKnownTestVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Hex.sha256("abc"));
    }

    @Test
    void plainTextStoresThePasswordItself() {
        PlainText plain = new PlainText(meter);
        assertEquals("welcome1", plain.hash("welcome1"));
        assertTrue(plain.verify("welcome1", "welcome1"));
        assertFalse(plain.verify("guess", "welcome1"));
    }

    @Test
    void anUnsaltedHashIsTheSameForTheSamePassword() {
        UnsaltedSha256 hasher = new UnsaltedSha256(meter);
        assertEquals(hasher.hash("welcome1"), hasher.hash("welcome1"));
        assertTrue(hasher.verify("welcome1", hasher.hash("welcome1")));
        assertFalse(hasher.verify("guess", hasher.hash("welcome1")));
    }

    @Test
    void aSaltedHashDiffersForTheSamePasswordButStillVerifies() {
        SaltedSha256 hasher = new SaltedSha256(meter, new CountingSalts());
        String first = hasher.hash("welcome1");
        String second = hasher.hash("welcome1");
        assertNotEquals(first, second);
        assertTrue(hasher.verify("welcome1", first));
        assertTrue(hasher.verify("welcome1", second));
        assertFalse(hasher.verify("welcome2", first));
    }

    @Test
    void pbkdf2StoresRoundsSaltAndHashAndVerifies() {
        Pbkdf2Hasher hasher = new Pbkdf2Hasher(meter, new CountingSalts(), 1000);
        String stored = hasher.hash("welcome1");
        String[] parts = stored.split("\\$");
        assertEquals(4, parts.length);
        assertEquals("pbkdf2-sha256", parts[0]);
        assertEquals("1000", parts[1]);
        assertEquals(32, parts[2].length());
        assertEquals(64, parts[3].length());
        assertTrue(hasher.verify("welcome1", stored));
        assertFalse(hasher.verify("guess", stored));
    }

    @Test
    void pbkdf2ChecksWithTheRoundsStoredNotTheCurrentOnes() {
        Pbkdf2Hasher hasher = new Pbkdf2Hasher(meter, new CountingSalts(), 2000);
        String old = hasher.hashWithRounds("letmein", 500);
        assertEquals(500, Pbkdf2Hasher.iterationsOf(old));
        assertTrue(hasher.verify("letmein", old));
        assertTrue(hasher.needsUpgrade(old));
        assertFalse(hasher.needsUpgrade(hasher.hash("letmein")));
    }

    @Test
    void workIsCountedInStepsNotSeconds() {
        new UnsaltedSha256(meter).hash("x");
        assertEquals(1, meter.steps());
        new Pbkdf2Hasher(meter, new CountingSalts(), 1000).hash("x");
        assertEquals(1001, meter.steps());
        meter.reset();
        assertEquals(0, meter.steps());
    }

    @Test
    void countingSaltsAreDifferentEachTimeButRepeatable() {
        CountingSalts a = new CountingSalts();
        CountingSalts b = new CountingSalts();
        byte[] first = a.next();
        assertNotEquals(java.util.Arrays.toString(first), java.util.Arrays.toString(a.next()));
        assertEquals(java.util.Arrays.toString(first), java.util.Arrays.toString(b.next()));
    }

    @Test
    void secureSaltsAreSixteenRandomBytes() {
        SecureSalts salts = new SecureSalts();
        byte[] one = salts.next();
        assertEquals(16, one.length);
        assertNotEquals(java.util.Arrays.toString(one), java.util.Arrays.toString(salts.next()));
    }
}
