package com.jk.explore.security.patching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PatchWindowAndPinsTest {

    @Test
    void deadlinesBySeverity() {
        assertEquals(7, PatchWindow.slaDays("Critical"));
        assertEquals(30, PatchWindow.slaDays("High"));
        assertEquals(90, PatchWindow.slaDays("Medium"));
        assertEquals(180, PatchWindow.slaDays("Low"));
    }

    @Test
    void patchedOnTheLastDayIsInTime() {
        assertTrue(PatchWindow.evaluate("Critical", 0, 7, 100).withinWindow());
        assertFalse(PatchWindow.evaluate("Critical", 0, 8, 100).withinWindow());
        assertEquals("patched in 8 days, LATE: the SLA was 7", PatchWindow.evaluate("Critical", 0, 8, 100).text());
    }

    @Test
    void openFindingsCountDownThenBreach() {
        assertEquals("open for 28 days, 62 days left in the SLA of 90", PatchWindow.evaluate("Medium", 12, null, 40).text());
        assertTrue(PatchWindow.evaluate("High", 0, null, 30).withinWindow());
        PatchWindow.Status late = PatchWindow.evaluate("High", 0, null, 40);
        assertFalse(late.withinWindow());
        assertEquals("open for 40 days, BREACHED: the SLA of 30 ended 10 days ago", late.text());
    }

    @Test
    void sha256MatchesPublishedVectors() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Pins.sha256(new byte[0]));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Pins.sha256("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void pinnedArtifactsVerifyAndChangedOnesDoNot() {
        Pins pins = new Pins();
        byte[] content = "jar".getBytes(StandardCharsets.UTF_8);
        pins.pin("a@1.0.0", content);
        assertEquals(Pins.Result.VERIFIED, pins.verify("a@1.0.0", content));
        for (int i = 0; i < content.length; i++) {
            for (int bit = 0; bit < 8; bit++) {
                byte[] changed = content.clone();
                changed[i] ^= (byte) (1 << bit);
                assertEquals(Pins.Result.MISMATCH, pins.verify("a@1.0.0", changed));
            }
        }
        assertEquals(Pins.Result.MISMATCH, pins.verify("a@1.0.0", new byte[0]));
    }

    @Test
    void unpinnedArtifactsAreRefusedWhateverTheyContain() {
        Pins pins = new Pins();
        pins.pin("a@1.0.0", new byte[] {1});
        assertEquals(Pins.Result.NOT_PINNED, pins.verify("a@1.0.1", new byte[] {1}));
        assertEquals(Pins.Result.NOT_PINNED, pins.verify("b@1.0.0", new byte[] {1}));
    }

    @Test
    void editDistanceIsTheUsualOne() {
        assertEquals(0, Typosquat.distance("abc", "abc"));
        assertEquals(1, Typosquat.distance("abc", "abd"));
        assertEquals(1, Typosquat.distance("abc", "ab"));
        assertEquals(1, Typosquat.distance("abc", "abcd"));
        assertEquals(3, Typosquat.distance("kitten", "sitting"));
        assertEquals(3, Typosquat.distance("", "abc"));
    }

    @Test
    void onlyNearMissesLookLikeCopies() {
        List<String> known = List.of("acme-json", "acme-xml");
        assertTrue(Typosquat.looksLikeACopy("acme-jsom", known));
        assertTrue(Typosquat.looksLikeACopy("acme-jsonn", known));
        assertFalse(Typosquat.looksLikeACopy("acme-json", known));
        assertFalse(Typosquat.looksLikeACopy("totally-different", known));
        assertFalse(Typosquat.looksLikeACopy("acme-yaml-parser", known));
    }
}
