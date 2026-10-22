package com.jk.explore.security.searchdetect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** A real OpenSearch with its alerting plugin. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenSearchTest {

    private Lab lab;
    private OpenSearch os;

    @BeforeAll
    void start() throws Exception {
        lab = new Lab();
        os = new OpenSearch(lab.url());
        os.createAuditIndex("audit");
        os.bulk("audit", Scenario.day());
    }

    @AfterAll
    void stop() {
        lab.close();
    }

    @Test
    void theWholeDayIsIndexed() throws Exception {
        assertEquals(772, os.count("audit", Map.of("match_all", Map.of())));
        assertEquals(768, os.count("audit", Map.of("term", Map.of("type", "login"))));
        assertEquals(113, os.count("audit", Map.of("exists", Map.of("field", "attack"))));
    }

    @Test
    void theFirstBruteForceRuleCatchesTheAttackAndTwoForgetfulPeople() throws Exception {
        List<Hit> hits = Detections.run(os, "audit", Detections.bruteForce(3));
        assertEquals(3, hits.size());
        assertEquals(1, hits.stream().filter(Hit::truePositive).count());
    }

    @Test
    void raisingTheThresholdRemovesTheFalseAlertsUntilItMissesTheAttack() throws Exception {
        assertEquals(1, Detections.run(os, "audit", Detections.bruteForce(10)).size());
        assertEquals(1, Detections.run(os, "audit", Detections.bruteForce(25)).size());
        assertEquals(0, Detections.run(os, "audit", Detections.bruteForce(40)).size());
    }

    @Test
    void sprayingIsOneAddressAndFortyAccounts() throws Exception {
        List<Hit> hits = Detections.run(os, "audit", Detections.spraying(8));
        assertEquals(1, hits.size());
        assertEquals("203.0.113.50", hits.get(0).subject());
        assertEquals(Set.of("A"), hits.get(0).attacks());
    }

    @Test
    void privilegeChangeFindsTheBreakGlassGrantAndTheSelfGrant() throws Exception {
        List<Hit> hits = Detections.run(os, "audit", Detections.privilegeChange());
        assertEquals(2, hits.size());
        assertEquals(1, hits.stream().filter(Hit::truePositive).count());
    }

    @Test
    void theBucketMonitorsTriggerOnTheSameBucketsAsTheQueries() throws Exception {
        assertEquals(1, Monitors.triggered(os.execute(os.createMonitor(Monitors.bruteForce(10)))).size());
        List<Hit> spraying = Monitors.triggered(os.execute(os.createMonitor(Monitors.spraying(8))));
        assertEquals(1, spraying.size());
        assertEquals("203.0.113.50", spraying.get(0).subject());
        assertEquals(2, Monitors.triggered(os.execute(os.createMonitor(Monitors.successAfterFailures(20)))).size());
    }

    @Test
    void theQueryLevelMonitorTriggersWhenThereIsAnUnapprovedGrant() throws Exception {
        var run = os.execute(os.createMonitor(Monitors.privilegeChange()));
        assertTrue(run.get("trigger_results").propertyStream().findFirst().orElseThrow().getValue().get("triggered").asBoolean());
        assertTrue(os.monitors() >= 1);
    }

    @Test
    void eventsReadBackInOrderGiveTheCodeRuleItsFourAlerts() throws Exception {
        List<Event> back = os.events("audit");
        assertEquals(772, back.size());
        assertEquals(4, new ImpossibleTravelRule(900, 500, Set.of(Scenario.VPN_EGRESS)).evaluate(back).size());
    }

    @Test
    void withoutAMappingAnAggregationOnATextFieldIsRefused() throws Exception {
        os.bulk("dyn-test", Scenario.day().subList(0, 20));
        assertEquals(400, os.search("dyn-test", Map.of("size", 0, "aggs", Map.of("u", Map.of("terms", Map.of("field", "user"))))).status());
        assertEquals(200, os.search("dyn-test", Map.of("size", 0, "aggs", Map.of("u", Map.of("terms", Map.of("field", "user.keyword"))))).status());
    }

    @Test
    void fixedBucketsMissAnAttackThatCrossesTheirBoundary() throws Exception {
        os.createAuditIndex("straddle-test");
        List<Event> burst = new java.util.ArrayList<>();
        for (int s = 0; s < 30; s++) {
            burst.add(Event.login(Scenario.at(13, 28, 0).plusSeconds(s * 8L), "u077", "198.51.100.77", City.SINGAPORE, false, "B"));
        }
        os.bulk("straddle-test", burst);
        assertFalse(Detections.run(os, "straddle-test", Detections.bruteForce(10)).isEmpty());
        assertTrue(Detections.run(os, "straddle-test", Detections.bruteForce(20)).isEmpty());
    }
}
