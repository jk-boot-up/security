package com.jk.explore.security.certlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    private static final Instant T0 = Instant.parse("2027-01-01T00:00:00Z");

    @Test
    void renewalIsDueAfterTwoThirdsOfTheLife() {
        assertEquals(T0.plusSeconds(8), Renewer.renewAt(T0, T0.plusSeconds(12)));
        assertEquals(T0.plusSeconds(60 * 86400L), Renewer.renewAt(T0, T0.plusSeconds(90 * 86400L)));
    }

    @Test
    void theHeadroomIsAThirdOfTheLife() {
        Instant notAfter = T0.plusSeconds(12);
        assertEquals(4, java.time.Duration.between(Renewer.renewAt(T0, notAfter), notAfter).toSeconds());
    }

    @Test
    void theMonitorAlertsWhenAQuarterOfTheLifeIsLeft() {
        Instant notAfter = T0.plusSeconds(12);
        assertFalse(ExpiryWatch.shouldAlert(T0, notAfter, T0.plusSeconds(8)));
        assertFalse(ExpiryWatch.shouldAlert(T0, notAfter, T0.plusMillis(8999)));
        assertTrue(ExpiryWatch.shouldAlert(T0, notAfter, T0.plusSeconds(9)));
        assertTrue(ExpiryWatch.shouldAlert(T0, notAfter, T0.plusSeconds(13)));
    }

    @Test
    void healthyRotationNeverReachesTheAlert() {
        // a renewal at two thirds leaves a third; the alert is at a quarter
        Instant notAfter = T0.plusSeconds(12);
        Instant renewal = Renewer.renewAt(T0, notAfter).plusMillis(500);
        assertFalse(ExpiryWatch.shouldAlert(T0, notAfter, renewal));
    }
}
