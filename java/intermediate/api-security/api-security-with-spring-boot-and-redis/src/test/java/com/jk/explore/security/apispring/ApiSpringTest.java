package com.jk.explore.security.apispring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Real servers on loopback ports; the budget is kept in memory here, so no Docker is needed. */
class ApiSpringTest {

    private static final List<String> RECORD_FIELDS = List.of("balanceCents", "cardNumber", "creditLimitCents", "displayName", "email", "id", "pinHash", "riskScore", "role");
    private static final List<String> VIEW_FIELDS = List.of("id", "displayName", "email", "balanceCents", "cardLast4");

    @Test
    void firstVersionLetsTheCallerPromoteThemselves() throws Exception {
        try (Running api = Running.start("naive", null)) {
            Client.Reply r = api.client().patch("/accounts/me", "ana", "{\"role\":\"admin\",\"creditLimitCents\":100000000}");
            assertEquals(200, r.status());
            assertEquals("admin", r.json().get("role"));
            assertEquals(100_000_000, ((Number) r.json().get("creditLimitCents")).intValue());
            assertEquals("admin", api.bean(Bank.class).ownedBy("ana").getRole());
        }
    }

    @Test
    void firstVersionReturnsTheWholeRecordAndEveryCustomer() throws Exception {
        try (Running api = Running.start("naive", null)) {
            Client c = api.client();
            assertEquals(RECORD_FIELDS, c.get("/accounts/acc-0001", "ana").fields());
            Client.Reply all = c.get("/accounts?size=1000000", "ana");
            assertEquals(Bank.CUSTOMERS, all.list().size());
            assertTrue(all.list().get(5).containsKey("pinHash"));
        }
    }

    @Test
    void firstVersionAnswersAnyNumberOfRequests() throws Exception {
        try (Running api = Running.start("naive", null)) {
            for (int i = 0; i < 300; i++) {
                assertEquals(200, api.client().get("/accounts/acc-0002", "ana").status());
            }
        }
    }

    @Test
    void secondVersionRefusesAFieldTheModelDoesNotName() throws Exception {
        try (Running api = Running.start("safe", null, "bank.limiter=local")) {
            Client.Reply r = api.client().patch("/accounts/me", "ana", "{\"role\":\"admin\",\"creditLimitCents\":100000000}");
            assertEquals(400, r.status());
            assertEquals("unknown field", r.detail());
            assertEquals("customer", api.bean(Bank.class).ownedBy("ana").getRole());
            assertEquals(50_000, api.bean(Bank.class).ownedBy("ana").getCreditLimitCents());
        }
    }

    @Test
    void secondVersionRefusesAnUnknownFieldBesideAValidOne() throws Exception {
        try (Running api = Running.start("safe", null, "bank.limiter=local")) {
            Client.Reply r = api.client().patch("/accounts/me", "ana", "{\"displayName\":\"Changed\",\"role\":\"admin\"}");
            assertEquals(400, r.status());
            assertEquals("Ana", api.bean(Bank.class).ownedBy("ana").getDisplayName(), "nothing is applied from a refused body");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"email\":\"not-an-email\"}", "{\"displayName\":\"\"}", "{\"displayName\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}"})
    void secondVersionValidatesTheValues(String body) throws Exception {
        try (Running api = Running.start("safe", null, "bank.limiter=local")) {
            Client.Reply r = api.client().patch("/accounts/me", "ana", body);
            assertEquals(400, r.status());
            assertTrue(r.detail().startsWith("invalid "));
        }
    }

    @Test
    void secondVersionAcceptsAPartialUpdate() throws Exception {
        try (Running api = Running.start("safe", null, "bank.limiter=local")) {
            Client.Reply r = api.client().patch("/accounts/me", "ana", "{\"email\":\"ana@bank.example\"}");
            assertEquals(200, r.status());
            assertEquals("ana@bank.example", r.json().get("email"));
            assertEquals("Ana", r.json().get("displayName"));
        }
    }

    @Test
    void secondVersionReturnsOnlyTheViewAndAMaskedCard() throws Exception {
        try (Running api = Running.start("safe", null, "bank.limiter=local")) {
            Client.Reply r = api.client().get("/accounts/acc-0001", "ana");
            assertEquals(VIEW_FIELDS, r.fields());
            assertEquals("****0001", r.json().get("cardLast4"));
            assertFalse(r.body().contains("4111"));
            assertFalse(r.body().contains("pinHash"));
        }
    }

    @Test
    void aPageHasACeilingAndALowerBound() throws Exception {
        try (Running api = Running.start("safe", null, "bank.limiter=local", "bank.budget.capacity=100")) {
            Client c = api.client();
            Client.Reply big = c.get("/accounts?size=1000000", "ana");
            assertEquals(50, big.list().size());
            assertEquals(List.of("id", "displayName"), List.copyOf(big.list().get(0).keySet()));
            assertEquals(5, c.get("/accounts?size=5", "ana").list().size());
            assertEquals(400, c.get("/accounts?size=0", "ana").status());
            assertEquals(400, c.get("/accounts?size=-3", "ana").status());
        }
    }

    @Test
    void noLoginMeansNoAnswer() throws Exception {
        try (Running api = Running.start("safe", null, "bank.limiter=local")) {
            assertEquals(401, api.client().get("/accounts", null).status());
            assertEquals(401, api.client().get("/accounts/acc-0001", null).status());
        }
    }

    @Test
    void aBudgetRunsOutAndRefillsAtTheConfiguredRate() throws Exception {
        try (Running api = Running.start("safe", null, "bank.limiter=local")) {
            Client c = api.client();
            int answered = 0;
            Client.Reply last = null;
            for (int i = 0; i < 200; i++) {
                last = c.get("/accounts/acc-0001", "ana");
                if (last.status() == 200) {
                    answered++;
                }
            }
            assertEquals(50, answered);
            assertEquals(429, last.status());
            assertEquals("1", last.header("retry-after"));
            api.bean(ManualClock.class).advance(Duration.ofMillis(500));
            Client.Reply early = c.get("/accounts/acc-0001", "ana");
            assertEquals(429, early.status());
            assertEquals("1", early.header("retry-after"));
            api.bean(ManualClock.class).advance(Duration.ofMillis(500));
            assertEquals(200, c.get("/accounts/acc-0001", "ana").status());
            assertEquals(429, c.get("/accounts/acc-0001", "ana").status());
        }
    }

    @Test
    void aRefillNeverGoesAboveTheCapacity() throws Exception {
        try (Running api = Running.start("safe", null, "bank.limiter=local")) {
            Client c = api.client();
            api.bean(ManualClock.class).advance(Duration.ofHours(1));
            int answered = 0;
            for (int i = 0; i < 100; i++) {
                if (c.get("/accounts/acc-0001", "ana").status() == 200) {
                    answered++;
                }
            }
            assertEquals(50, answered);
        }
    }

    @Test
    void eachCallerHasABudgetOfTheirOwn() throws Exception {
        try (Running api = Running.start("safe", null, "bank.limiter=local")) {
            Client c = api.client();
            for (int i = 0; i < 60; i++) {
                c.get("/accounts/acc-0001", "ana");
            }
            assertEquals(429, c.get("/accounts/acc-0001", "ana").status());
            assertEquals(200, c.get("/accounts/acc-0001", "ben").status());
        }
    }

    @Test
    void twoServersWithTheirOwnBudgetsGiveTwiceTheBudget() throws Exception {
        try (Running a = Running.start("safe", null, "bank.limiter=local"); Running b = Running.start("safe", null, "bank.limiter=local")) {
            int answered = 0;
            for (int i = 0; i < 400; i++) {
                if ((i % 2 == 0 ? a : b).client().get("/accounts/acc-0001", "ana").status() == 200) {
                    answered++;
                }
            }
            assertEquals(100, answered);
        }
    }
}
