package com.jk.explore.security.cia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class SealStepsTest {

    @Test
    void theStepsByHandGiveTheSameSealAsJava() {
        SealSteps.Steps s = SealSteps.of("test-secret", CiaDemo.MAYA.sealedText());
        assertEquals(new Seal("test-secret").of(CiaDemo.MAYA), HexFormat.of().formatHex(s.seal()));
    }

    @Test
    void theKeyIsPaddedWithZerosAndXoredWith36And5c() {
        SealSteps.Steps s = SealSteps.of("ab", "x");
        assertEquals(2, s.keyLength());
        assertEquals(64, s.paddedKey().length);
        assertEquals(0, s.paddedKey()[63]);
        assertEquals((byte) ('a' ^ 0x36), s.innerKey()[0]);
        assertEquals((byte) 0x36, s.innerKey()[63]);
        assertEquals((byte) 0x5c, s.outerKey()[63]);
    }
}
