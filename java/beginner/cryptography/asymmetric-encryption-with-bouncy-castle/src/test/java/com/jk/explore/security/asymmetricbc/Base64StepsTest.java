package com.jk.explore.security.asymmetricbc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class Base64StepsTest {

    @Test
    void threeBytesBecomeFourSixBitGroups() {
        assertArrayEquals(new int[] {12, 8, 8, 1}, Base64Steps.groups((byte) 0x30, (byte) 0x82, (byte) 0x01));
        assertEquals("MIIB", Base64Steps.encode(new byte[] {0x30, (byte) 0x82, 0x01}));
    }

    @Test
    void shortEndingsArePaddedWithEquals() {
        for (String text : List.of("", "a", "ab", "abc", "abcd", "account=ANA-1")) {
            byte[] bytes = Hex.bytes(text);
            assertEquals(Base64.getEncoder().encodeToString(bytes), Base64Steps.encode(bytes));
        }
    }

    @Test
    void byHandGivesTheLibrarysPemTextLineByLine() {
        KeyPair ana = BouncyCastleDemo.newKeyPair();
        List<String> pem = Pem.publicKey(ana.getPublic()).lines().toList();
        List<String> byHand = Base64Steps.lines(Base64Steps.encode(ana.getPublic().getEncoded()));
        assertEquals(pem.subList(1, pem.size() - 1), byHand);
        assertEquals(7, byHand.size());
    }

    @Test
    void theGoldenOutputShowsTheSameSteps() throws Exception {
        String golden = Files.readString(Path.of("docs", "expected-output.txt"));
        assertTrue(golden.contains("    6-bit groups 001100   001000   001000   000001\n"));
        assertTrue(golden.contains("    letters      M        I        I        B\n"));
        assertEquals("00110000", Base64Steps.bits(0x30, 8));
    }
}
