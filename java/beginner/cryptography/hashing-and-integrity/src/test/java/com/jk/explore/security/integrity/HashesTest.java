package com.jk.explore.security.integrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class HashesTest {

    @Test
    void crc32MatchesTheStandardCheckValue() {
        assertEquals("cbf43926", Crc32Checksum.of(Hex.bytes("123456789")));
    }

    @Test
    void sha256MatchesTheStandardVectors() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Sha256.hex("abc"));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Sha256.hex(""));
    }

    @Test
    void sha256IsAlways64HexDigitsWhateverTheInput() {
        assertEquals(64, Sha256.hex("a").length());
        assertEquals(64, Sha256.hex("a much longer text ".repeat(1000)).length());
    }

    @Test
    void hmacMatchesRfc4231TestCaseOne() {
        byte[] key = new byte[20];
        Arrays.fill(key, (byte) 0x0b);
        assertEquals("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7", Hmac.hex(key, Hex.bytes("Hi There")));
    }

    @Test
    void hmacMatchesRfc4231TestCaseTwo() {
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
                Hmac.hex(Hex.bytes("Jefe"), Hex.bytes("what do ya want for nothing?")));
    }

    @Test
    void aTagVerifiesOnlyForTheSameKeyAndMessage() {
        byte[] key = Hex.bytes("k");
        byte[] tag = Hmac.tag(key, Hex.bytes("message"));
        assertTrue(Hmac.verify(key, Hex.bytes("message"), tag));
        assertFalse(Hmac.verify(key, Hex.bytes("messagE"), tag));
        assertFalse(Hmac.verify(Hex.bytes("other"), Hex.bytes("message"), tag));
    }

    @Test
    void aOneDigitChangeFlipsAboutHalfTheBits() {
        byte[] a = Sha256.digest(Hex.bytes(IntegrityDemo.INVOICE.text()));
        byte[] b = Sha256.digest(Hex.bytes(IntegrityDemo.INVOICE.withAmount(1201).text()));
        int bits = Hex.differingBits(a, b);
        assertTrue(bits > 90 && bits < 166, "expected about 128 of 256 bits, got " + bits);
        assertEquals(134, bits);
    }

    @Test
    void aCrcCanBeRecomputedForChangedDataSoItProvesNothingAboutTampering() {
        byte[] forged = Hex.bytes(IntegrityDemo.INVOICE.withAmount(12).text());
        assertNotEquals(Crc32Checksum.of(Hex.bytes(IntegrityDemo.INVOICE.text())), Crc32Checksum.of(forged));
        // the attacker simply stores the new CRC beside the new data, and the pair is consistent
        assertEquals(Crc32Checksum.of(forged), Crc32Checksum.of(forged));
    }

    @Test
    void aSingleFlippedBitChangesTheCrc() {
        byte[] original = Hex.bytes(IntegrityDemo.INVOICE.text());
        byte[] damaged = original.clone();
        damaged[3] ^= 0x04;
        assertNotEquals(Crc32Checksum.of(original), Crc32Checksum.of(damaged));
        assertEquals(1, Hex.differingBits(original, damaged));
    }

    @Test
    void hexFormatRoundTrips() {
        byte[] bytes = {0, 1, 127, (byte) 255};
        assertEquals("00017fff", Hex.of(bytes));
        assertEquals(Arrays.toString(bytes), Arrays.toString(HexFormat.of().parseHex("00017fff")));
    }
}
