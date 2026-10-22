package com.jk.explore.security.iacscan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EngineTest {

    @Test
    void theFirstVersionBreaksTwelveRulesAndAHostNetwork() {
        List<Finding> f = Engine.scan(Manifests.FIRST);
        assertEquals(13, f.size());
        assertEquals(Set.of("K1", "K2", "K3", "K4", "K5", "K6", "K7", "K8"), f.stream().map(Finding::rule).collect(Collectors.toSet()));
    }

    @Test
    void theFixedVersionKeepsOnlyTheHostNetworkOfTheAgent() {
        assertEquals(List.of(new Finding("K6", "DaemonSet/metrics-agent", "agent")), Engine.scan(Manifests.FIXED));
    }

    @Test
    void everyRuleFiresOnItsBadSampleAndNotOnItsGoodOne() {
        for (Rule r : Rules.ALL) {
            assertEquals("", RuleTester.check(r), r.id());
        }
    }

    @Test
    void aRuleThatOnlyKnowsLatestFailsItsOwnTest() {
        Rule mistaken = new Rule("K3", "x", c -> c.container().path("image").asString("").endsWith(":latest"), "image: shop/orders", "image: shop/web:1.8.2");
        assertTrue(RuleTester.check(mistaken).contains("does not fire"));
    }

    @Test
    void nonWorkloadsAreIgnored() {
        assertTrue(Engine.scan("apiVersion: v1\nkind: Service\nmetadata: {name: s}\n").isEmpty());
    }

    @Test
    void severalDocumentsAreAllRead() {
        assertEquals(3, Engine.documents(Manifests.FIRST).size());
    }

    @Test
    void aPodIsReadWithoutATemplate() {
        assertEquals(1, Engine.scan(RuleTester.wrap("securityContext: {privileged: true}"), List.of(Rules.ALL.get(0))).size());
    }

    @Test
    void aSecretFromASecretKeyRefIsNotWrittenInTheFile() {
        assertTrue(Engine.scan(RuleTester.wrap("env: [{name: DB_PASSWORD, valueFrom: {secretKeyRef: {name: s, key: k}}}]"), List.of(Rules.ALL.get(4))).isEmpty());
    }

    @Test
    void waiversEndAndNeedAnOwner() {
        List<Finding> f = Engine.scan(Manifests.FIXED);
        Waiver ok = new Waiver("K6", "DaemonSet/metrics-agent", "platform", "host counters", LocalDate.of(2026, 12, 31));
        assertTrue(Waiver.apply(f, List.of(ok), LocalDate.of(2026, 12, 31)).isEmpty());
        assertEquals(1, Waiver.apply(f, List.of(ok), LocalDate.of(2027, 1, 1)).size());
        assertEquals(1, Waiver.apply(f, List.of(new Waiver("K6", "DaemonSet/metrics-agent", " ", "x", LocalDate.of(2030, 1, 1))), LocalDate.of(2026, 1, 1)).size());
        assertEquals(1, Waiver.apply(f, List.of(new Waiver("K6", "DaemonSet/other", "platform", "x", LocalDate.of(2030, 1, 1))), LocalDate.of(2026, 1, 1)).size());
        assertEquals(1, Waiver.stale(List.of(ok), LocalDate.of(2027, 1, 1)).size());
    }

    @Test
    void aBaselineFailsOnlyOnNewFindings() {
        List<Finding> accepted = Engine.scan(Manifests.FIXED);
        List<Finding> now = Engine.scan(Manifests.FIXED + "---\n" + IacScanningDemo.CART);
        assertEquals(5, Baseline.newFindings(now, accepted).size());
        assertTrue(Baseline.fixed(now, accepted).isEmpty());
        assertEquals(1, Baseline.fixed(List.of(), accepted).size());
    }
}
