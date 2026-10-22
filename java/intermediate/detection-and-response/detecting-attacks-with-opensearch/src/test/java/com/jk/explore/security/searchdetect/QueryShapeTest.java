package com.jk.explore.security.searchdetect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The queries and monitors as documents, and the scenario, with no server. */
class QueryShapeTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void theScenarioIsTheSameMondayAsInThePlainProject() {
        List<Event> day = Scenario.day();
        assertEquals(772, day.size());
        assertEquals(113, day.stream().filter(e -> e.attackId() != null).count());
        assertEquals(4, day.stream().filter(e -> e.type().equals("role-grant")).count() + 0);
    }

    @Test
    void anEventBecomesADocumentWithKeywordFriendlyFields() {
        var doc = OpenSearch.doc(Scenario.day().get(0));
        assertTrue(doc.keySet().containsAll(List.of("time", "type", "user", "ip", "city", "success", "actor", "role", "ticket")));
        assertEquals(false, doc.containsKey("attack") && doc.get("attack") == null);
    }

    @Test
    void theBruteForceQueryFiltersFailuresGroupsByUserAndBucketsFiveMinutes() {
        String q = JSON.writeValueAsString(Detections.bruteForce(10));
        assertTrue(q.contains("\"success\":false") && q.contains("\"fixed_interval\":\"5m\"") && q.contains("\"min_doc_count\":10") && q.contains("\"field\":\"user\""));
    }

    @Test
    void theSprayingQueryCountsDistinctAccountsPerAddress() {
        String q = JSON.writeValueAsString(Detections.spraying(8));
        assertTrue(q.contains("cardinality") && q.contains("params.u >= 8") && q.contains("\"fixed_interval\":\"20m\"") && q.contains("\"field\":\"ip\""));
    }

    @Test
    void thePrivilegeChangeQueryWantsAnApproverAndATicket() {
        String q = JSON.writeValueAsString(Detections.privilegeChange());
        assertTrue(q.contains("ops1") && q.contains("ops2") && q.contains("\"ticket\":\"\"") && q.contains("minimum_should_match"));
    }

    @Test
    void theMonitorsHaveAScheduleAQueryAndATrigger() {
        String m = JSON.writeValueAsString(Monitors.bruteForce(10));
        assertTrue(m.contains("bucket_level_monitor") && m.contains("MINUTES") && m.contains("params._count >= 10") && m.contains("composite_agg"));
        assertTrue(JSON.writeValueAsString(Monitors.privilegeChange()).contains("ctx.results[0].hits.total.value > 0"));
    }

    @Test
    void impossibleTravelNeedsThePreviousLoginSoItIsCode() {
        var alerts = new ImpossibleTravelRule(900, 500, java.util.Set.of(Scenario.VPN_EGRESS)).evaluate(Scenario.day());
        assertEquals(4, alerts.size());
        assertEquals(3, alerts.stream().filter(Alert::isTruePositive).count());
    }

    @Test
    void aSlidingWindowSeesAnAttackThatFixedBucketsSplit() {
        List<Event> burst = new java.util.ArrayList<>();
        for (int s = 0; s < 30; s++) {
            burst.add(Event.login(Scenario.at(13, 28, 0).plusSeconds(s * 8L), "u077", "198.51.100.77", City.SINGAPORE, false, "B"));
        }
        assertEquals(1, new OpenSearchDetectDemo.BruteForceRuleForStraddle().evaluate(burst).size());
        long inFirstBucket = burst.stream().filter(e -> e.time().isBefore(Scenario.at(13, 30, 0))).count();
        assertEquals(15, inFirstBucket);
    }
}
