package com.jk.explore.security.totp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** The step-by-step code must be exactly the code Totp makes, with the intermediate values of RFC 4226. */
class CodeStepsTest {

    private static final byte[] KEY = DemoSecrets.RFC_KEY;

    @Test
    void givesTheSameCodeAsTotpForManyTimes() {
        for (long t = 0; t < 30 * 200; t += 7) {
            assertEquals(Totp.codeAt(KEY, t), CodeSteps.at(KEY, t).code(), "time " + t);
        }
    }

    @Test
    void intermediateValuesMatchRfc4226ForCounterOne() {
        CodeSteps.Steps s = CodeSteps.at(KEY, 59);
        assertEquals(1, s.counter());
        assertEquals("0000000000000001", HexFormat.of().formatHex(s.counterBytes()));
        assertEquals("75a48a19d4cbe100644e8ac1397eea747a2d33ab", HexFormat.of().formatHex(s.hmac()));
        assertEquals(11, s.offset());
        assertEquals("c1397eea", HexFormat.of().formatHex(s.picked()));
        assertEquals(1094287082, s.number());
        assertEquals("287082", s.code());
    }
}
