package com.jk.explore.security.threatmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StrideTest {

    private final Model bank = BankModel.baseline();
    private final List<String> keys = StrideRules.generate(bank).stream().map(Threat::key).toList();

    @Test
    void theRulesFindTwentyThreatsInAStableOrder() {
        assertEquals(List.of("S:E1", "S:E2", "S:P2", "S:P3", "T:F2", "T:F3", "T:F6", "T:F7", "T:F8",
                "R:F2", "R:F3", "R:F4", "R:F6", "I:S1", "I:S2", "D:P2", "E:F2", "E:F3", "E:F7", "E:F8"), keys);
    }

    @Test
    void flowsInsideOneZoneAreNotTamperingCandidates() {
        assertFalse(keys.contains("T:F1"));
        assertFalse(keys.contains("T:F4"));
    }

    @Test
    void onlyFlowsIntoMoreTrustedZonesRaisePrivilege() {
        assertTrue(keys.contains("E:F7"));
        assertFalse(keys.contains("E:F6"));
    }

    @Test
    void aNewPublicProcessIsADenialOfServiceCandidate() {
        Model more = bank.with(new Element("P4", "web shop", Kind.PROCESS, BankModel.DMZ, "web team", true, Set.of()))
                .with(new Flow("F10", "P1", "P4", "page request", true, true, false));
        List<String> found = StrideRules.generate(more).stream().map(Threat::key).toList();
        assertTrue(found.containsAll(List.of("D:P4", "S:P4", "T:F10", "E:F10")));
    }

    @Test
    void ratingsFollowTheWeakerZoneAndWhatIsAtStake() {
        Threat t = StrideRules.generate(bank).stream().filter(x -> x.key().equals("T:F2")).findFirst().orElseThrow();
        assertEquals(25, Rating.of(bank, t).risk());
        Threat inside = StrideRules.generate(bank).stream().filter(x -> x.key().equals("R:F4")).findFirst().orElseThrow();
        assertEquals(new Rating(2, 5), Rating.of(bank, inside));
        Threat store = StrideRules.generate(bank).stream().filter(x -> x.key().equals("I:S2")).findFirst().orElseThrow();
        assertEquals(new Rating(2, 3), Rating.of(bank, store));
    }
}
