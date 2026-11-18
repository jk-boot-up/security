package com.jk.explore.security.apisec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiSecurityTest {

    private final Bank bank = Bank.demo();
    private final SimClock clock = new SimClock();

    @Test
    void naiveApiLetsTheCallerSetAnyField() {
        new NaiveApi(bank).patchProfile("ana", "acc-0001", ApiSecurityDemo.ESCALATION);
        Account a = bank.find("acc-0001").orElseThrow();
        assertEquals("admin", a.get("role"));
        assertEquals(100_000_000L, a.get("creditLimitCents"));
    }

    @Test
    void naiveApiSendsEveryFieldAndEveryAccount() {
        Map<?, ?> body = assertInstanceOf(Map.class, new NaiveApi(bank).getAccount("ana", "acc-0001").body());
        assertTrue(body.containsKey("pinHash") && body.containsKey("riskScore"));
        assertEquals("4242 4242 4242 0001", body.get("cardNumber"));
        assertEquals(Bank.ACCOUNTS, ((List<?>) new NaiveApi(bank).listAccounts("ana", 0, 1_000_000).body()).size());
    }

    @Test
    void safeApiRefusesUnknownFieldsAndChangesNothing() {
        ApiResponse r = new SafeApi(bank, clock).patchProfile("ana", "acc-0001", ApiSecurityDemo.ESCALATION);
        assertEquals(400, r.status());
        Account a = bank.find("acc-0001").orElseThrow();
        assertEquals("customer", a.get("role"));
        assertEquals("ana", a.get("displayName"));
    }

    @Test
    void safeApiValidatesTheFieldsItAccepts() {
        Api api = new SafeApi(bank, clock);
        assertEquals(200, api.patchProfile("ana", "acc-0001", Map.of("email", "ana@new.example")).status());
        assertEquals(400, api.patchProfile("ana", "acc-0001", Map.of("email", "not an email")).status());
        assertEquals(400, api.patchProfile("ana", "acc-0001", Map.of("displayName", 42)).status());
        assertEquals(400, api.patchProfile("ana", "acc-0001", Map.of("displayName", "x".repeat(41))).status());
        assertEquals(400, api.patchProfile("ana", "acc-0001", Map.of()).status());
    }

    @Test
    void safeResponsesShowOnlyTheViewAndMaskTheCard() {
        AccountView v = assertInstanceOf(AccountView.class, new SafeApi(bank, clock).getAccount("ana", "acc-0001").body());
        assertEquals("0001", v.cardLast4());
        assertFalse(v.toString().contains("4242"));
        assertFalse(v.toString().contains("pbkdf2"));
    }

    @Test
    void safeListIsCappedAndShowsOnlyTheDirectory() {
        Api api = new SafeApi(bank, clock);
        List<?> page = (List<?>) api.listAccounts("ana", 0, 1_000_000).body();
        assertEquals(SafeApi.MAX_SIZE, page.size());
        assertInstanceOf(DirectoryEntry.class, page.get(0));
        assertEquals(SafeApi.DEFAULT_SIZE, ((List<?>) api.listAccounts("ana", 1, 0).body()).size());
        assertEquals(400, api.listAccounts("ana", -1, 10).status());
        assertEquals(0, ((List<?>) new SafeApi(bank, new SimClock()).listAccounts("ana", 1000, 10).body()).size());
    }

    @Test
    void theBudgetRunsOutRefillsAndIsPerCaller() {
        Api api = new SafeApi(bank, clock);
        for (int i = 0; i < 5; i++) {
            assertEquals(200, api.patchProfile("ana", "acc-0001", Map.of("displayName", "n" + i)).status());
        }
        ApiResponse limited = api.patchProfile("ana", "acc-0001", Map.of("displayName", "again"));
        assertEquals(429, limited.status());
        assertEquals(12, limited.retryAfterSeconds());
        assertEquals(200, api.patchProfile("user2", "acc-0002", Map.of("displayName", "other")).status());
        clock.advance(limited.retryAfterSeconds());
        assertEquals(200, api.patchProfile("ana", "acc-0001", Map.of("displayName", "again")).status());
    }

    @Test
    void ownershipIsStillChecked() {
        assertEquals(404, new SafeApi(bank, clock).getAccount("ana", "acc-0002").status());
        assertEquals(404, new SafeApi(bank, clock).patchProfile("ana", "acc-0002", Map.of("displayName", "x")).status());
    }

    @Test
    void rateLimiterNeverExceedsItsCapacity() {
        RateLimiter l = new RateLimiter(clock, 3, 1);
        clock.advance(1000);
        assertEquals(0, l.tryTake("a", 3));
        assertTrue(l.tryTake("a", 1) > 0);
        assertTrue(l.tryTake("a", 4) > 0);
    }

    @Test
    void tokensShowTheRefillWithoutTakingAny() {
        RateLimiter l = new RateLimiter(clock, 100, 100.0 / 60);
        assertEquals(0, l.tryTake("ana", 100));
        assertEquals(0.0, l.tokens("ana"), 1e-9);
        int wait = l.tryTake("ana", 1);
        assertEquals((int) Math.ceil(1 / l.perSecond()), wait);
        assertEquals(1, wait);
        clock.advance(wait);
        assertEquals(100.0 / 60, l.tokens("ana"), 1e-9);
        assertEquals(100.0 / 60, l.tokens("ana"), 1e-9);
        assertEquals(100.0, l.tokens("someone else"), 1e-9);
    }

    @Test
    void theRequestModelNamesOnlyTwoFields() {
        assertEquals(List.of("displayName", "email"), UpdateProfileRequest.FIELDS);
        assertEquals(List.of("displayName", "role", "creditLimitCents"), List.copyOf(ApiSecurityDemo.ESCALATION.keySet()));
    }
}
