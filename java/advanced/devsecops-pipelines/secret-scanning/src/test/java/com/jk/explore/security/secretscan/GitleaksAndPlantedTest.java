package com.jk.explore.security.secretscan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class GitleaksAndPlantedTest {

    @Test
    void theReleaseArchiveMatchesTheMachine() {
        assertEquals("gitleaks_8.30.1_darwin_arm64.tar.gz", Gitleaks.assetName("Mac OS X", "aarch64"));
        assertEquals("gitleaks_8.30.1_linux_x64.tar.gz", Gitleaks.assetName("Linux", "amd64"));
    }

    @Test
    void aPlantedValueIsNeverPrintedInFull() {
        assertEquals("sk_live_...", Planted.STRIPE.shown());
        assertEquals("Summer20...", Planted.PASSWORD.shown());
    }

    @Test
    void aVerdictIsFoundOrMissedForASecretAndFlaggedOrQuietForALookAlike() {
        Score s = Score.of(List.of(new Finding("r", "f", Planted.AWS.value()), new Finding("r", "f", Planted.TEST_KEY.value())));
        assertEquals("found", s.verdict(Planted.AWS));
        assertEquals("missed", s.verdict(Planted.STRIPE));
        assertEquals("flagged", s.verdict(Planted.TEST_KEY));
        assertEquals("quiet", s.verdict(Planted.HASH));
        assertEquals("1/5 = 20%", s.recall());
        assertEquals("1/2 = 50%", s.precision());
    }
}
