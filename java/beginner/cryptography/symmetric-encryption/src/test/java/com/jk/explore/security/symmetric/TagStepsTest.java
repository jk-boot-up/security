package com.jk.explore.security.symmetric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** The tag worked out by hand is the tag the JDK's AES/GCM makes. */
class TagStepsTest {

    private static byte[] hex(String h) {
        return HexFormat.of().parseHex(h);
    }

    private static byte[] tagOf(Sealed sealed) {
        byte[] all = sealed.ciphertextWithTag();
        return Arrays.copyOfRange(all, all.length - GcmBox.TAG_BYTES, all.length);
    }

    @Test
    void matchesTestCaseTwoOfTheOriginalSpecification() {
        TagSteps.Steps s = TagSteps.of(new byte[16], new byte[12], new byte[0], hex("0388dace60b6a392f328c2b971b2fe78"));
        assertEquals("66e94bd4ef8a2c3b884cfa59ca342b2e", Hex.of(s.hashKey()));
        assertEquals("ab6e47d42cec13bdf53a67b21257bddf", Hex.of(s.tag()));
    }

    @Test
    void matchesTheJdkForTheDemoLine() {
        byte[] account = Hex.bytes(SymmetricDemo.ACCOUNT);
        Sealed sealed = new GcmBox(SymmetricDemo.KEY, new CountingNonces()).seal(account, Hex.bytes(SymmetricDemo.LINE));
        TagSteps.Steps s = TagSteps.of(SymmetricDemo.KEY, sealed.nonce(), account, sealed.ciphertext());
        assertEquals(Hex.of(tagOf(sealed)), Hex.of(s.tag()));
        assertEquals(3, s.mixes().size());
    }

    @Test
    void matchesTheJdkForLongerMessages() {
        byte[] account = Hex.bytes("account=a-much-longer-name-than-one-block");
        for (int n : new int[] {0, 1, 15, 16, 17, 33, 100}) {
            Sealed sealed = new GcmBox(SymmetricDemo.KEY, new CountingNonces()).seal(account, new byte[n]);
            assertEquals(Hex.of(tagOf(sealed)), Hex.of(TagSteps.of(SymmetricDemo.KEY, sealed.nonce(), account, sealed.ciphertext()).tag()),
                    "length " + n);
        }
    }
}
