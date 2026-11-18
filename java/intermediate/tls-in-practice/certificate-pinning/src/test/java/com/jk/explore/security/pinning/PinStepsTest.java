package com.jk.explore.security.pinning;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.KeyPair;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** The pin worked out by hand gives exactly the pin the library makes. */
class PinStepsTest {

    @Test
    void theStepsGiveTheSamePinAsSpki() {
        KeyPair key = Pki.newKey();
        PinSteps steps = PinSteps.of(key.getPublic());
        assertEquals(Spki.pin(key.getPublic()), steps.pin());
        assertEquals(91, steps.spki().length, "an EC P-256 SubjectPublicKeyInfo");
        assertEquals(26, steps.headerBytes());
        assertEquals(0x04, steps.spki()[26], "the point starts with 04: x and y follow");
        assertEquals(44, steps.pin().length());
    }

    @Test
    void base64ByHandMatchesTheJdkForEveryLength() {
        for (int n = 0; n < 40; n++) {
            byte[] bytes = new byte[n];
            for (int i = 0; i < n; i++) {
                bytes[i] = (byte) (i * 37 + n);
            }
            assertEquals(Base64.getEncoder().encodeToString(bytes), PinSteps.base64(bytes), "length " + n);
        }
    }

    @Test
    void theFirstGroupIsFourSixBitValues() {
        PinSteps steps = new PinSteps(new byte[0], new byte[] {(byte) 0x2f, (byte) 0x3a, (byte) 0xbf}, "Lzq/");
        assertArrayEquals(new int[] {11, 51, 42, 63}, steps.firstGroup());
        assertEquals("Lzq/", PinSteps.base64(steps.hash()));
    }

    @Test
    void aSeededGeneratorRepeatsItsKeys() {
        Pki.seed(7);
        String first = Spki.pin(Pki.newKey().getPublic());
        Pki.seed(7);
        assertEquals(first, Spki.pin(Pki.newKey().getPublic()));
    }
}
