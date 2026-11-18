package com.jk.explore.security.cryptoagility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The teaching class must split every real envelope into parts that add up, with the lengths the video shows. JDK only. */
class LayoutTest {

    private final KeyRing keys = new KeyRing();
    private final int k = keys.add();
    private final Envelope envelope = new Envelope(keys);

    @Test
    void thePartsOfEverySuiteAddUpToTheRealEnvelope() {
        for (int n = 0; n <= 40; n++) {
            byte[] plain = "x".repeat(n).getBytes(StandardCharsets.UTF_8);
            Layout cbc = Layout.of(envelope.seal(plain, Envelope.V1_CBC_HMAC, k));
            assertEquals(Layout.cbcLength(n), cbc.ciphertext());
            assertEquals(3 + 16 + Layout.cbcLength(n) + 32, cbc.total());
            for (int v : new int[] {Envelope.V2_GCM, Envelope.V3_CHACHA}) {
                Layout aead = Layout.of(envelope.seal(plain, v, k));
                assertEquals(n, aead.ciphertext());
                assertEquals(3 + 12 + n + 16, aead.total());
            }
        }
    }

    @Test
    void theHeaderIsMagicVersionAndKeyIdInHex() {
        assertEquals("45 02 01", Layout.of(envelope.seal(new byte[5], Envelope.V2_GCM, k)).header());
    }

    @Test
    void cbcAlwaysAddsPadding() {
        assertEquals(32, Layout.cbcLength(21));
        assertEquals(32, Layout.cbcLength(16));
        assertEquals(16, Layout.cbcLength(0));
    }
}
