package com.jk.explore.security.patching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    void versionsCompareAsNumbers() {
        assertTrue(SemVer.parse("1.10.0").compareTo(SemVer.parse("1.9.0")) > 0);
        assertTrue(SemVer.parse("2.0.0").compareTo(SemVer.parse("1.99.99")) > 0);
        assertEquals(0, SemVer.parse("1.2.3").compareTo(SemVer.parse("1.2.3")));
        assertEquals("1.2.3", SemVer.parse(" 1.2.3 ").toString());
    }

    @Test
    void nonsenseVersionsAreRefused() {
        for (String bad : new String[] {"1.2", "1.2.3.4", "1.x.3", "", "1..3", "-1.2.3", "1.2.3-beta"}) {
            assertThrows(IllegalArgumentException.class, () -> SemVer.parse(bad), bad);
        }
    }

    @Test
    void rangeBoundariesAreExact() {
        VersionRange range = VersionRange.parse(">=1.5.0 <1.8.3");
        assertFalse(range.contains(SemVer.parse("1.4.99")));
        assertTrue(range.contains(SemVer.parse("1.5.0")));
        assertTrue(range.contains(SemVer.parse("1.8.2")));
        assertFalse(range.contains(SemVer.parse("1.8.3")));
    }

    @Test
    void everyOperatorWorks() {
        SemVer v = SemVer.parse("2.0.0");
        assertTrue(VersionRange.parse(">1.9.9").contains(v));
        assertFalse(VersionRange.parse(">2.0.0").contains(v));
        assertTrue(VersionRange.parse("<=2.0.0").contains(v));
        assertFalse(VersionRange.parse("<2.0.0").contains(v));
        assertTrue(VersionRange.parse("=2.0.0").contains(v));
    }

    @Test
    void badRangesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(""));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(">=1.2"));
    }
}
