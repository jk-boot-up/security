package com.jk.explore.security.webhooks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The hand-made HMAC gives exactly the JDK's result, for a short key, the demo's key and a key longer than one block. */
class HmacStepsTest {

    @Test
    void byHandEqualsTheJdkMac() {
        byte[] content = Signatures.signedContent(1_800_000_000L, new Event("evt_1001paid", Event.PAID, "A-1001", 4200).toJson());
        for (byte[] key : new byte[][] {"Jefe".getBytes(StandardCharsets.US_ASCII), WebhookDemo.SECRET, new byte[100]}) {
            assertArrayEquals(Signatures.hmacSha256(key, content), HmacSteps.of(key, content).mac());
        }
    }

    @Test
    void theKeyIsPaddedToOneBlockAndMixed() {
        HmacSteps.Steps steps = HmacSteps.of(WebhookDemo.SECRET, new byte[0]);
        assertEquals(HmacSteps.BLOCK, steps.paddedKey().length);
        assertEquals(0, steps.paddedKey()[WebhookDemo.SECRET.length]);
        assertEquals((byte) ('w' ^ 0x36), steps.innerKey()[0]);
        assertEquals((byte) 0x5c, steps.outerKey()[HmacSteps.BLOCK - 1]);
    }
}
