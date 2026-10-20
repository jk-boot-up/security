package com.jk.explore.security.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DetectionTest {

    private static final Instant T0 = Instant.parse("2026-03-02T10:00:00Z");

    private static Event fail(int seconds, String user, String ip) {
        return Event.login(T0.plusSeconds(seconds), user, ip, City.PARIS, false, null);
    }

    private static Event ok(int seconds, String user, String ip, City city) {
        return Event.login(T0.plusSeconds(seconds), user, ip, city, true, null);
    }

    @Test
    void distancesAreRight() {
        assertEquals(5837, City.PARIS.kmTo(City.NEW_YORK), 5);
        assertEquals(0, City.PARIS.kmTo(City.PARIS), 0.001);
        assertEquals(City.LONDON.kmTo(City.PARIS), City.PARIS.kmTo(City.LONDON), 0.001);
        assertEquals(17_000, City.SYDNEY.kmTo(City.PARIS) - 0, 1_000);
    }

    @Test
    void bruteForceFiresOnceAtTheThresholdInsideTheWindow() {
        List<Event> log = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            log.add(fail(i * 10, "ana", "1.1.1.1"));
        }
        assertEquals(1, new BruteForceRule(5, Duration.ofMinutes(5)).evaluate(log).size(), "one alert for a run, not one per failure");
        assertEquals(0, new BruteForceRule(7, Duration.ofMinutes(5)).evaluate(log).size());
    }

    @Test
    void bruteForceIgnoresSlowGuessingAndResetsOnSuccess() {
        List<Event> slow = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            slow.add(fail(i * 120, "ana", "1.1.1.1"));
        }
        assertEquals(0, new BruteForceRule(5, Duration.ofMinutes(5)).evaluate(slow).size());
        List<Event> reset = new ArrayList<>(List.of(fail(0, "ana", "x"), fail(5, "ana", "x"), fail(10, "ana", "x"), ok(15, "ana", "x", City.PARIS)));
        reset.addAll(List.of(fail(20, "ana", "x"), fail(25, "ana", "x"), fail(30, "ana", "x")));
        assertEquals(0, new BruteForceRule(4, Duration.ofMinutes(5)).evaluate(reset).size());
    }

    @Test
    void sprayingCountsDistinctAccountsFromOneAddress() {
        List<Event> log = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            log.add(fail(i * 30, "user" + i, "9.9.9.9"));
        }
        assertEquals(1, new SprayingRule(8, Duration.ofMinutes(10)).evaluate(log).size());
        assertEquals(0, new SprayingRule(9, Duration.ofMinutes(10)).evaluate(log).size());
        assertEquals(0, new BruteForceRule(3, Duration.ofMinutes(10)).evaluate(log).size(), "each account failed once: the brute-force rule never sees it");
        List<Event> oneUser = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            oneUser.add(fail(i, "ana", "9.9.9.9"));
        }
        assertEquals(0, new SprayingRule(8, Duration.ofMinutes(10)).evaluate(oneUser).size());
    }

    @Test
    void impossibleTravelNeedsDistanceAndSpeed() {
        Rule rule = new ImpossibleTravelRule(900, 500, Set.of());
        assertEquals(1, rule.evaluate(List.of(ok(0, "ana", "a", City.PARIS), ok(1500, "ana", "b", City.SYDNEY))).size());
        assertEquals(0, rule.evaluate(List.of(ok(0, "ana", "a", City.PARIS), ok(9 * 3600, "ana", "b", City.NEW_YORK))).size(), "a flight is possible");
        assertEquals(0, rule.evaluate(List.of(ok(0, "ana", "a", City.PARIS), ok(60, "ana", "b", City.FRANKFURT))).size(), "below the minimum distance");
        assertEquals(0, rule.evaluate(List.of(ok(0, "ana", "a", City.PARIS), ok(60, "bob", "b", City.SYDNEY))).size(), "different accounts");
        assertEquals(0, rule.evaluate(List.of(fail(0, "ana", "a"), ok(60, "ana", "b", City.SYDNEY))).size(), "failures do not count");
    }

    @Test
    void theVpnListHidesTheEgressCity() {
        List<Event> log = List.of(ok(0, "ana", "10.0.0.1", City.NEW_YORK), ok(1800, "ana", "198.18.0.1", City.FRANKFURT));
        assertEquals(1, new ImpossibleTravelRule(900, 500, Set.of()).evaluate(log).size());
        assertEquals(0, new ImpossibleTravelRule(900, 500, Set.of("198.18.0.1")).evaluate(log).size());
    }

    @Test
    void privilegeChangesNeedAnApproverAndATicket() {
        Rule rule = new PrivilegeChangeRule(Set.of("ops1"), true);
        assertEquals(0, rule.evaluate(List.of(Event.grant(T0, "ops1", "ana", "admin", "CHG-1", null))).size());
        assertEquals(1, rule.evaluate(List.of(Event.grant(T0, "ops1", "ana", "admin", "", null))).size(), "no ticket");
        assertEquals(1, rule.evaluate(List.of(Event.grant(T0, "bob", "bob", "admin", "CHG-1", null))).size(), "not an approver");
        assertEquals(0, rule.evaluate(List.of(Event.grant(T0, "bob", "ana", "viewer", "", null))).size(), "only the admin role is watched");
        assertEquals(0, new PrivilegeChangeRule(Set.of("ops1"), false).evaluate(List.of(Event.grant(T0, "ops1", "ana", "admin", "", null))).size());
    }

    @Test
    void takeoverNeedsFailuresFromTheSameAddressAndANewAddress() {
        List<Event> log = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            log.add(fail(i, "ana", "6.6.6.6"));
        }
        log.add(ok(10, "ana", "6.6.6.6", City.PARIS));
        assertEquals(1, new TakeoverRule(5, Duration.ofMinutes(10), Map.of()).evaluate(log).size());
        assertEquals(0, new TakeoverRule(5, Duration.ofMinutes(10), Map.of("ana", Set.of("6.6.6.6"))).evaluate(log).size(), "her usual address");
        assertEquals(0, new TakeoverRule(6, Duration.ofMinutes(10), Map.of()).evaluate(log).size());
    }

    @Test
    void theDayHasTheExpectedShape() {
        List<Event> log = Scenario.day();
        assertEquals(772, log.size());
        assertEquals(113, log.stream().filter(e -> e.attackId() != null).count());
        assertEquals(Set.of("A", "B", "C", "D"), log.stream().map(Event::attackId).filter(x -> x != null).collect(java.util.stream.Collectors.toSet()));
        for (int i = 1; i < log.size(); i++) {
            assertFalse(log.get(i).time().isBefore(log.get(i - 1).time()));
        }
        assertEquals(log, Scenario.day(), "the same day every time");
    }

    @Test
    void theTunedSetFindsEveryAttackWithFewFalseAlarms() {
        List<Event> log = Scenario.day();
        int alerts = 0;
        int tp = 0;
        Set<String> found = new java.util.TreeSet<>();
        for (Rule r : DetectionDemo.tuned()) {
            Score s = Score.of(r, log);
            alerts += s.alerts();
            tp += s.truePositives();
            found.addAll(s.attacksDetected());
        }
        assertEquals(9, alerts);
        assertEquals(7, tp);
        assertEquals(Set.of("A", "B", "C", "D"), found);
    }

    @Test
    void theFirstVersionsDrownTheAnalystAndTuningHelps() {
        List<Event> log = Scenario.day();
        Score loose = Score.of(new BruteForceRule(3, Duration.ofMinutes(5)), log);
        Score tight = Score.of(new BruteForceRule(10, Duration.ofMinutes(5)), log);
        Score tooTight = Score.of(new BruteForceRule(40, Duration.ofMinutes(5)), log);
        assertEquals(2, loose.falsePositives());
        assertEquals(0, tight.falsePositives());
        assertEquals(0, tooTight.alerts(), "a threshold above the attack misses it");
        assertTrue(Score.of(new ImpossibleTravelRule(900, 500, Set.of()), log).falsePositives() > 5);
        assertEquals(1, Score.of(new ImpossibleTravelRule(900, 500, Set.of(Scenario.VPN_EGRESS)), log).falsePositives());
    }

    @Test
    void anAlertIsTrueWhenItsEvidenceHoldsAnAttackEvent() {
        Event attack = Event.login(T0, "ana", "x", City.PARIS, false, "Z");
        Event plain = Event.login(T0, "bob", "x", City.PARIS, false, null);
        assertTrue(new Alert("r", "s", T0, "m", List.of(plain, attack)).isTruePositive());
        assertFalse(new Alert("r", "s", T0, "m", List.of(plain)).isTruePositive());
    }
}
