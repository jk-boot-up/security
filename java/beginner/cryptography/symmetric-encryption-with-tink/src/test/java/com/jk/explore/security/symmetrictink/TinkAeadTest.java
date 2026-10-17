package com.jk.explore.security.symmetrictink;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class TinkAeadTest {

    private final Bank bank = new Bank(DemoKeysets.aes256Gcm(TinkDemo.KEY_ID, TinkDemo.KEY_TEXT));

    @Test
    void theSameLineSealedTwiceGivesDifferentBytes() {
        assertFalse(Arrays.equals(bank.seal(TinkDemo.ACCOUNT, TinkDemo.LINE), bank.seal(TinkDemo.ACCOUNT, TinkDemo.LINE)));
    }

    @Test
    void aSealedMessageOpensAgain() {
        byte[] sealed = bank.seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        assertArrayEquals(TinkDemo.LINE, bank.openOrNull(TinkDemo.ACCOUNT, sealed));
    }

    @Test
    void theMessageStartsWithVersionOneAndTheKeyId() {
        byte[] sealed = bank.seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        assertEquals("01000003e9", HexFormat.of().formatHex(sealed, 0, 5));
    }

    @Test
    void sealingAddsThirtyThreeBytes() {
        assertEquals(5 + 33, bank.seal(TinkDemo.ACCOUNT, "hello".getBytes(StandardCharsets.UTF_8)).length);
        assertEquals(33, bank.seal(TinkDemo.ACCOUNT, new byte[0]).length);
    }

    @Test
    void aFlippedBitIsRefusedWhereverItIs() {
        byte[] sealed = bank.seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        for (int position : new int[] {10, 20, sealed.length - 1}) {
            byte[] bad = sealed.clone();
            bad[position] ^= 1;
            assertNull(bank.openOrNull(TinkDemo.ACCOUNT, bad), "position " + position);
        }
    }

    @Test
    void differentAssociatedDataIsRefused() {
        byte[] sealed = bank.seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        assertNull(bank.openOrNull("account=MAL-2".getBytes(StandardCharsets.UTF_8), sealed));
        assertNotNull(bank.openOrNull(TinkDemo.ACCOUNT, sealed));
    }

    @Test
    void aDifferentKeyIsRefused() {
        byte[] sealed = bank.seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        Bank other = new Bank(DemoKeysets.aes256Gcm(TinkDemo.KEY_ID, "a-completely-different-key-text!"));
        assertNull(other.openOrNull(TinkDemo.ACCOUNT, sealed));
    }
}
