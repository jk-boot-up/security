package com.jk.explore.security.netiso;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** What each layout works out on paper, with no Docker. The integration tests prove that the real networks give the same pairs. */
class LayoutTest {

    @Test
    void aFlatNetworkJoinsEveryPair() {
        Layout flat = Layout.flat();
        assertEquals(15, flat.pairsOn("flat"));
        assertEquals(15, flat.pairs().size());
        assertEquals(Set.of("admin", "app", "cache", "ci", "db", "web"), flat.canLeave());
    }

    @Test
    void threeTiersGiveEightPairsTwoOfThemUnwanted() {
        Layout tiers = Layout.tiers();
        assertEquals(List.of(1, 6, 1), List.of(tiers.pairsOn("front"), tiers.pairsOn("back"), tiers.pairsOn("mgmt")));
        assertEquals(8, tiers.pairs().size());
        assertEquals(Set.of("admin-cache", "cache-db"), Intent.shop().extra(tiers.pairs()));
        assertEquals(Set.of(), Intent.shop().missing(tiers.pairs()));
        assertEquals(Set.of("app", "web"), tiers.canLeave());
    }

    @Test
    void aLayoutBuiltFromTheIntentGivesExactlyTheIntent() {
        Layout built = Layout.of(Intent.shop(), true);
        assertEquals(Intent.shop().pairs(), built.pairs());
        assertEquals(7, built.networks().size());
        assertEquals(Set.of("web"), built.canLeave());
        assertEquals(List.of("link1", "link4", "link5", "link6"), built.networksOf("app"));
        assertEquals(List.of("link6", "edge"), built.networksOf("web"));
    }

    @Test
    void withoutAnEdgeNothingCanLeave() {
        assertEquals(Set.of(), Layout.of(Intent.shop(), false).canLeave());
    }
}
