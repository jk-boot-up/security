package com.jk.explore.security.secretscan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DetectorTest {

    private final Detector detector = new Detector();

    @Test
    void aPaymentKeyIsFoundByItsPrefix() {
        List<Finding> f = detector.scanText("a.properties", "payment.key=" + Planted.STRIPE.value());
        assertEquals("payment-live-key", f.get(0).rule());
    }

    @Test
    void aPasswordAssignmentIsFoundEvenWhenTheValueIsShortOfEntropy() {
        assertTrue(detector.scanText("a", "db.password=" + Planted.PASSWORD.value()).stream().anyMatch(x -> x.rule().equals("password-assignment")));
    }

    @Test
    void aTestKeyIsNotOnTheLiveKeyRuleButItsEntropyGivesItAway() {
        List<Finding> f = detector.scanText("a", "k=" + Planted.TEST_KEY.value());
        assertEquals("high-entropy-string", f.get(0).rule());
    }

    @Test
    void aChecksumIsNotFlaggedBecauseHexCannotReachTheThreshold() {
        assertTrue(detector.scanText("a", Planted.HASH.value()).isEmpty());
    }

    @Test
    void entropyOfRepeatedCharactersIsZeroAndOfSixteenDistinctIsFour() {
        assertEquals(0.0, Detector.entropy("aaaaaaaa"), 1e-9);
        assertEquals(4.0, Detector.entropy("0123456789abcdef"), 1e-9);
    }

    @Test
    void aSecretInBase64IsFoundByEntropy() {
        String encoded = java.util.Base64.getEncoder().encodeToString(Planted.BASE64.value().getBytes());
        assertTrue(!detector.scanText("secret.yaml", "key: " + encoded).isEmpty());
    }
}
