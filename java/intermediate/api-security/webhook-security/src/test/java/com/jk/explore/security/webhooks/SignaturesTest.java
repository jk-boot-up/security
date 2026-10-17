package com.jk.explore.security.webhooks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class SignaturesTest {

    private static final byte[] SECRET = "test-secret".getBytes(StandardCharsets.UTF_8);
    private static final byte[] BODY = "{\"id\":\"evt_1\"}".getBytes(StandardCharsets.UTF_8);

    @Test
    void hmacMatchesRfc4231TestCaseTwo() {
        byte[] mac = Signatures.hmacSha256("Jefe".getBytes(StandardCharsets.US_ASCII),
                "what do ya want for nothing?".getBytes(StandardCharsets.US_ASCII));
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", HexFormat.of().formatHex(mac));
    }

    @Test
    void headerIsTheHmacOfTimeDotBodyComputedIndependently() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
        String expected = HexFormat.of().formatHex(mac.doFinal("1800000000.{\"id\":\"evt_1\"}".getBytes(StandardCharsets.UTF_8)));
        assertEquals("t=1800000000,v1=" + expected, Signatures.header(SECRET, 1_800_000_000L, BODY));
    }

    @Test
    void parseReadsTheTwoPartsAndRejectsAnythingElse() {
        Signatures.Parsed parsed = Signatures.parse(Signatures.header(SECRET, 42, BODY)).orElseThrow();
        assertEquals(42, parsed.timestamp());
        assertArrayEquals(Signatures.hmacSha256(SECRET, Signatures.signedContent(42, BODY)), parsed.mac());
        assertTrue(Signatures.parse(null).isEmpty());
        assertTrue(Signatures.parse("").isEmpty());
        assertTrue(Signatures.parse("t=42").isEmpty());
        assertTrue(Signatures.parse("t=-1,v1=" + "0".repeat(64)).isEmpty());
        assertTrue(Signatures.parse("t=42,v1=XYZ").isEmpty());
        assertTrue(Signatures.parse("t=42,v1=" + "0".repeat(63)).isEmpty());
        assertTrue(Signatures.parse("garbage").isEmpty());
    }

    @Test
    void matchesUnderAnyListedSecretOnly() {
        Signatures.Parsed parsed = Signatures.parse(Signatures.header(SECRET, 7, BODY)).orElseThrow();
        byte[] other = "other".getBytes(StandardCharsets.UTF_8);
        assertTrue(Signatures.matchesAny(List.of(other, SECRET), parsed, BODY));
        assertFalse(Signatures.matchesAny(List.of(other), parsed, BODY));
        assertFalse(Signatures.matchesAny(List.of(), parsed, BODY));
        byte[] changed = BODY.clone();
        changed[changed.length - 2] ^= 1;
        assertFalse(Signatures.matchesAny(List.of(SECRET), parsed, changed));
    }
}
