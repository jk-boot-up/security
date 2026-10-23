package com.jk.explore.security.detectioncode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EngineTest {

    private static final Rule.Correlation TEN_IN_FIVE = new Rule.Correlation("base", List.of("src_ip"), 300, 10);

    private static List<Ev> failures(String ip, int n, int everySeconds, int firstId) {
        List<Ev> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new Ev(firstId + i, Corpus.START + i * everySeconds * 1000L, Map.of("src_ip", ip)));
        }
        return out;
    }

    @Test
    void tenInFiveMinutesAlertsOnTheTenthEvent() {
        assertEquals(List.of(10), Engine.count(TEN_IN_FIVE, failures("a", 10, 20, 1)));
    }

    @Test
    void nineDoNotAlert() {
        assertEquals(List.of(), Engine.count(TEN_IN_FIVE, failures("a", 9, 20, 1)));
    }

    @Test
    void slowFailuresNeverFillTheWindow() {
        assertEquals(List.of(), Engine.count(TEN_IN_FIVE, failures("a", 40, 100, 1)));
    }

    @Test
    void oneCampaignIsOneAlertBecauseTheGroupIsQuietForOneWindow() {
        assertEquals(List.of(10), Engine.count(TEN_IN_FIVE, failures("a", 30, 5, 1)));
    }

    @Test
    void groupsAreCountedApart() {
        List<Ev> mixed = new ArrayList<>(failures("a", 6, 10, 1));
        mixed.addAll(failures("b", 6, 10, 7));
        mixed.sort(java.util.Comparator.comparingLong(Ev::ms));
        assertEquals(List.of(), Engine.count(TEN_IN_FIVE, mixed));
    }
}
