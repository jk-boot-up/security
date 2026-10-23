package com.jk.explore.security.dast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ZapAndFlawTest {

    private static final String REPORT = """
            {"site":[{"alerts":[
              {"name":"SQL Injection","riskcode":"3","instances":[{"uri":"http://h:1/account?id=3-2"}]},
              {"name":"Cross Site Scripting (Reflected)","riskcode":"3","instances":[{"uri":"http://h:1/search?q=a"},{"uri":"http://h:1/search?q=b"},{"uri":"http://h:1/account?id=x"}]},
              {"name":"Missing Anti-clickjacking Header","riskcode":"2","instances":[{"uri":"http://h:1"}]}
            ]}]}
            """;

    @Test
    void aReportBecomesOneAlertForEachNameWithItsDistinctPaths() {
        List<Alert> alerts = Zap.parse(REPORT);
        assertEquals(3, alerts.size());
        Alert xss = alerts.stream().filter(a -> a.name().startsWith("Cross")).findFirst().orElseThrow();
        assertEquals(List.of("/account", "/search"), xss.paths());
        assertEquals(3, xss.risk());
    }

    @Test
    void theRootPathIsASlash() {
        assertTrue(Zap.parse(REPORT).stream().anyMatch(a -> a.paths().contains("/")));
    }

    @Test
    void aPlantedFlawIsRaisedWhenTheAlertIsOnItsPath() {
        List<Alert> alerts = Zap.parse(REPORT);
        assertTrue(Flaw.PLANTED.get(0).raisedIn(alerts));
        assertFalse(Flaw.PLANTED.get(2).raisedIn(alerts));
    }

    @Test
    void aHeaderFlawCountsOnAnyPath() {
        assertTrue(Flaw.PLANTED.stream().filter(f -> f.id().equals("no clickjacking protection")).findFirst().orElseThrow().raisedIn(Zap.parse(REPORT)));
    }

    @Test
    void theNinePlantedFlawsIncludeOneBehindTheLogin() {
        assertEquals(9, Flaw.PLANTED.size());
        assertEquals(1, Flaw.PLANTED.stream().filter(Flaw::needsLogin).count());
    }

    @Test
    void thePlanSendsTheSessionCookieAndOnlyTheChosenRules() {
        String plan = Zap.plan("http://host.docker.internal:8080", "abc", List.of(40012, 40018));
        assertTrue(plan.contains("replacementString: session=abc"));
        assertTrue(plan.contains("id: 40012") && plan.contains("id: 40018"));
        assertTrue(plan.contains("defaultThreshold: \"off\""));
    }

    @Test
    void aPassivePlanHasNoActiveScanAndNoCookie() {
        String plan = Zap.plan("http://host.docker.internal:8080", null, List.of());
        assertFalse(plan.contains("activeScan"));
        assertFalse(plan.contains("replacementString"));
    }
}
