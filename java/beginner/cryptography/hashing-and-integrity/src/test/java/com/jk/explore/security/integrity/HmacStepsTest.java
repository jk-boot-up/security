package com.jk.explore.security.integrity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class HmacStepsTest {

    @Test
    void theStepsGiveJavasOwnTagForTheDemoInvoice() {
        SignedInvoices service = new SignedInvoices(IntegrityDemo.KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, false);
        byte[] message = service.encode(IntegrityDemo.INVOICE);
        assertArrayEquals(Hmac.tag(IntegrityDemo.KEY, message), HmacSteps.of(IntegrityDemo.KEY, message).tag());
    }

    @Test
    void theStepsMatchRfc4231TestCaseOne() {
        byte[] key = new byte[20];
        Arrays.fill(key, (byte) 0x0b);
        assertEquals("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
                Hex.of(HmacSteps.of(key, Hex.bytes("Hi There")).tag()));
    }

    @Test
    void aKeyLongerThanABlockIsHashedFirst() {
        byte[] key = new byte[131];
        Arrays.fill(key, (byte) 0xaa);
        byte[] message = Hex.bytes("Test Using Larger Than Block-Size Key - Hash Key First");
        assertArrayEquals(Hmac.tag(key, message), HmacSteps.of(key, message).tag());
    }

    @Test
    void theInnerAndOuterKeysAreThePaddedKeyXorTheirPads() {
        HmacSteps.Steps s = HmacSteps.of(Hex.bytes("k"), Hex.bytes("m"));
        assertEquals(64, s.paddedKey().length);
        assertEquals((byte) ('k' ^ 0x36), s.innerKey()[0]);
        assertEquals((byte) 0x36, s.innerKey()[63]);
        assertEquals((byte) ('k' ^ 0x5c), s.outerKey()[0]);
        assertEquals((byte) 0x5c, s.outerKey()[63]);
    }
}
