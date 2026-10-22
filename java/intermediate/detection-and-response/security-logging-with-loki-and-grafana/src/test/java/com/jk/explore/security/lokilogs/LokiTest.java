package com.jk.explore.security.lokilogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** A real Loki with its rule evaluator, and a real Grafana. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LokiTest {

    private Lab lab;
    private Loki bank;

    @BeforeAll
    void start() throws Exception {
        lab = new Lab();
        bank = new Loki(lab.lokiUrl(), "bank");
        LokiLogsDemo.ship(bank, Event.morning(), Instant.now().minusSeconds(60));
    }

    @AfterAll
    void stop() {
        lab.close();
    }

    @Test
    void theEventsAreStoredInFourStreamsUnderTheTwoLabelsWeSent() throws Exception {
        assertEquals(List.of("app", "event_type", "service_name"), bank.labels());
        assertEquals(4, bank.streams("{app=\"bank\"}"));
        assertEquals(8, bank.lines("{app=\"bank\"}").size());
    }

    @Test
    void aPushWithoutATenantIsRefused() throws Exception {
        Loki.Answer a = new Loki(lab.lokiUrl(), null).push(Map.of("app", "bank"), Instant.now(), "x");
        assertEquals(401, a.status());
    }

    @Test
    void theTransferIsFoundByItsFieldsAndTheStoryFromTheSameAddressIsInOrder() throws Exception {
        List<Loki.Line> transfer = bank.lines("{app=\"bank\"} | json | action=\"transfer\" | amount >= 500");
        assertEquals(1, transfer.size());
        assertEquals("198.51.100.66", LokiLogsDemo.field(transfer.get(0), "source_ip"));
        List<String> story = bank.lines("{app=\"bank\"} | json | source_ip=\"198.51.100.66\"").stream().map(l -> LokiLogsDemo.field(l, "outcome")).toList();
        assertEquals(List.of("failure", "failure", "success", "success"), story);
    }

    @Test
    void failuresAreCountedByAddress() throws Exception {
        assertEquals(Map.of("198.51.100.66", 2.0), bank.metric("sum by (source_ip) (count_over_time({app=\"bank\"} | json | outcome=\"failure\" [1h]))", "source_ip"));
    }

    @Test
    void anotherTenantSeesNothing() throws Exception {
        Loki shop = new Loki(lab.lokiUrl(), "shop");
        assertEquals(0, shop.lines("{app=\"bank\"}").size());
        assertEquals(List.of(), shop.labels());
    }

    @Test
    void anEventTenDaysOldIsRefused() throws Exception {
        Loki.Answer a = bank.push(Map.of("app", "bank"), Instant.now().minusSeconds(10L * 86400), "{}");
        assertEquals(400, a.status());
        assertTrue(a.body().contains("too old"));
    }

    @Test
    void aUserAsALabelMakesAStreamForEachUser() throws Exception {
        Instant now = Instant.now();
        for (int i = 0; i < 20; i++) {
            bank.push(Map.of("app", "labelled", "actor", "user" + i), now.minusSeconds(5), "{\"actor\":\"user" + i + "\"}");
            bank.push(Map.of("app", "content"), now.minusSeconds(5).plusNanos(i), "{\"actor\":\"user" + i + "\"}");
        }
        assertEquals(20, bank.streams("{app=\"labelled\"}"));
        assertEquals(1, bank.streams("{app=\"content\"}"));
        assertEquals(1, bank.lines("{app=\"content\"} | json | actor=\"user3\"").size());
    }

    @Test
    void grafanaAsksTheSameLokiWithTheSameTenant() throws Exception {
        Grafana grafana = new Grafana(lab.grafanaUrl());
        String uid = grafana.addLoki("bank-test", "bank");
        assertEquals("OK", grafana.health(uid));
        assertTrue(grafana.count(uid, "{app=\"bank\"} | json | outcome=\"failure\"") >= 2);
        String shop = grafana.addLoki("shop-test", "shop");
        assertEquals(0, grafana.count(shop, "{app=\"bank\"}"));
    }

    @Test
    void theGoodRuleFiresOnFourFailuresFromOneAddressAndTheTypoRuleNeverDoes() throws Exception {
        assertFalse(bank.ruleStates().isEmpty());
        Instant now = Instant.now();
        for (int i = 0; i < 4; i++) {
            bank.push(Map.of("app", "bank", "event_type", "login"), now.plusMillis(i), new Event("req-t" + i, "login", "carol", "login", "session", "203.0.113.99", "failure", null).line());
        }
        List<String> firing = List.of();
        for (int i = 0; i < 60 && firing.isEmpty(); i++) {
            Thread.sleep(1000);
            firing = bank.firingAlerts();
        }
        assertTrue(firing.contains("PasswordGuessing 203.0.113.99"), firing.toString());
        assertTrue(firing.stream().noneMatch(a -> a.startsWith("PasswordGuessingTypo")));
        assertEquals("inactive/ok", bank.ruleStates().get("PasswordGuessingTypo"));
    }
}
