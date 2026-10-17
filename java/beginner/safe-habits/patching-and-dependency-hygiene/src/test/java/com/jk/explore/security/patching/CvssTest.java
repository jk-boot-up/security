package com.jk.explore.security.patching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CvssTest {

    private static void check(double expected, String vector) {
        assertEquals(expected, Cvss31.base(vector), 0.0, vector);
    }

    @Test
    void reproducesPublishedScores() {
        check(9.8, "AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");
        check(10.0, "AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H");
        check(7.8, "AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:H");
        check(5.3, "AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N");
        check(6.1, "AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N");
        check(7.5, "AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H");
        check(5.9, "AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N");
        check(6.5, "AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N");
    }

    @Test
    void noImpactMeansZero() {
        check(0.0, "AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N");
    }

    @Test
    void thePrefixIsAccepted() {
        check(9.8, "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");
    }

    @Test
    void privilegesWeighMoreWhenScopeChanges() {
        assertEquals(true, Cvss31.base("AV:N/AC:L/PR:L/UI:N/S:C/C:L/I:L/A:N") > Cvss31.base("AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:L/A:N"));
    }

    @Test
    void roundUpFollowsTheSpecification() {
        assertEquals(4.0, Cvss31.roundUp(4.0), 0.0);
        assertEquals(3.1, Cvss31.roundUp(3.02), 0.0);
        assertEquals(4.1, Cvss31.roundUp(4.00001), 0.0);
        assertEquals(4.0, Cvss31.roundUp(3.999999999), 0.0);
        assertEquals(10.0, Cvss31.roundUp(9.91), 0.0);
    }

    @Test
    void severityBands() {
        assertEquals("None", Cvss31.severity(0.0));
        assertEquals("Low", Cvss31.severity(3.9));
        assertEquals("Medium", Cvss31.severity(4.0));
        assertEquals("Medium", Cvss31.severity(6.9));
        assertEquals("High", Cvss31.severity(7.0));
        assertEquals("High", Cvss31.severity(8.9));
        assertEquals("Critical", Cvss31.severity(9.0));
        assertEquals("Critical", Cvss31.severity(10.0));
    }

    @Test
    void badVectorsAreRefused() {
        for (String bad : new String[] {"", "AV:N", "AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H", "AV:X/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
            "AV:N/AV:N/PR:N/UI:N/S:U/C:H/I:H/A:H", "AV:N/AC:L/PR:N/UI:N/S:Z/C:H/I:H/A:H", "AV:N/AC:L/PR:Q/UI:N/S:U/C:H/I:H/A:H"}) {
            assertThrows(IllegalArgumentException.class, () -> Cvss31.base(bad), bad);
        }
    }
}
