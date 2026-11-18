package com.jk.explore.security.k8snetpol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The demo's lines from recorded measurements, with no cluster. */
class ReportTest {

    private static final Set<String> DESIGN = Lab.INTENT.allowed();

    @Test
    void theDesignIsListedInThePodsOrder() {
        assertEquals(List.of("shop/web -> pay/api", "pay/api -> pay/db", "ops/admin -> pay/db"), Report.design(Lab.INTENT, Lab.PODS));
    }

    @Test
    void theMatrixMarksTheDesignsPairs() {
        List<String> m = Report.matrix(Lab.PODS, DESIGN);
        assertEquals("from\\to   web    jobs   api    db     admin", m.get(0));
        assertEquals("web       -      .      o      .      .", m.get(1));
        assertEquals("api       .      .      -      o      .", m.get(3));
    }

    @Test
    void fivePodsMakeTwentyOrderedPairs() {
        assertEquals("5 pods   5 x 4 = 20 pairs", Report.pairCount(5));
    }

    @Test
    void reachableIsWantedPlusUnwanted() {
        Set<String> measured = new java.util.HashSet<>(DESIGN);
        measured.add(Intent.pair(Lab.JOBS, Lab.API));
        assertEquals(List.of("reachable   4 of 20 = 3 wanted + 1 unwanted", "unwanted 1  shop/jobs -> pay/api", "missing     0"),
                Report.against(Lab.INTENT.compare(Lab.PODS, measured)));
    }

    @Test
    void aClosedDesignIsThreeMissing() {
        List<String> lines = Report.against(Lab.INTENT.compare(Lab.PODS, Set.of()));
        assertEquals("missing 3   shop/web -> pay/api", lines.get(2));
        assertEquals("            pay/api -> pay/db", lines.get(3));
    }

    @Test
    void aLongListIsCountedNotListed() {
        List<String> lines = Report.against(Lab.INTENT.compare(Lab.PODS, Set.copyOf(java.util.stream.Stream.of(Lab.PODS.toArray(Workload[]::new))
                .flatMap(a -> Lab.PODS.stream().filter(b -> !a.equals(b)).map(b -> Intent.pair(a, b))).toList())));
        assertEquals("unwanted    17, every o that is not in the design", lines.get(1));
    }

    @Test
    void curlExitCodesInWords() {
        assertEquals("answered", Report.exit(0));
        assertEquals("timed out", Report.exit(28));
    }

    @Test
    void theApiServersAnswerIsItsLastWord() {
        assertEquals("created", KubernetesNetworkPoliciesDemo.verdict(List.of("networkpolicy.networking.k8s.io/a created", "networkpolicy.networking.k8s.io/b created")));
    }

    @Test
    void aSelectorOnTheWebLeavesTheJobsPod() {
        assertEquals(List.of("jobs"), KubernetesNetworkPoliciesDemo.leftOut("shop", List.of("web")));
    }
}
