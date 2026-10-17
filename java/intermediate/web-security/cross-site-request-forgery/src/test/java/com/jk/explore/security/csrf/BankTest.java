package com.jk.explore.security.csrf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** The bank's rules, called directly with hand-made requests: no network, no browser. */
class BankTest {

    private static String login(Bank bank) {
        Response r = bank.handle(new Request("POST", "/login", Map.of(), Map.of("user", "ana", "password", "correct horse battery staple")));
        String cookie = r.headers().get("Set-Cookie");
        return cookie.substring(0, cookie.indexOf(';'));
    }

    private static Request post(String cookie, Map<String, String> headers, Map<String, String> form) {
        Map<String, String> h = new HashMap<>(headers);
        h.put("cookie", cookie);
        return new Request("POST", "/transfer", h, form);
    }

    private static String token(Bank bank, String cookie) {
        Matcher m = Pattern.compile("value=\"([^\"]+)\"").matcher(bank.handle(new Request("GET", "/transfer-form", Map.of("cookie", cookie), Map.of())).body());
        assertTrue(m.find());
        return m.group(1);
    }

    @Test
    void aWrongPasswordGetsNoSession() {
        Bank bank = new Bank(Protection.none());
        Response r = bank.handle(new Request("POST", "/login", Map.of(), Map.of("user", "ana", "password", "guess")));
        assertEquals(401, r.status());
        assertTrue(r.headers().isEmpty());
    }

    @Test
    void theSessionCookieCarriesTheChosenAttributes() {
        Bank bank = new Bank(Protection.none().withSameSite("Lax"));
        String cookie = bank.handle(new Request("POST", "/login", Map.of(), Map.of("user", "ana", "password", "correct horse battery staple")))
                .headers().get("Set-Cookie");
        assertTrue(cookie.contains("Secure") && cookie.contains("HttpOnly") && cookie.endsWith("SameSite=Lax"));
    }

    @Test
    void withoutDefencesACookieIsEnough() {
        Bank bank = new Bank(Protection.none());
        assertEquals(200, bank.handle(post(login(bank), Map.of(), Map.of("to", "mallory", "amount", "500"))).status());
        assertEquals(500, bank.balance("ana"));
    }

    @Test
    void tokensMustBePresentAndBelongToTheSession() {
        Bank bank = new Bank(Protection.none().withTokens());
        String cookie = login(bank);
        String other = login(bank);
        assertEquals(403, bank.handle(post(cookie, Map.of(), Map.of("to", "mallory", "amount", "5"))).status());
        assertEquals(403, bank.handle(post(cookie, Map.of(), Map.of("to", "mallory", "amount", "5", "csrf", token(bank, other)))).status());
        assertEquals(403, bank.handle(post(cookie, Map.of(), Map.of("to", "mallory", "amount", "5", "csrf", "x"))).status());
        assertEquals(200, bank.handle(post(cookie, Map.of(), Map.of("to", "mallory", "amount", "5", "csrf", token(bank, cookie)))).status());
        assertEquals(1, bank.transfers());
    }

    @Test
    void fetchMetadataRefusesEverythingButTheBanksOwnPages() {
        Bank bank = new Bank(Protection.none().withFetchChecks());
        String cookie = login(bank);
        Map<String, String> form = Map.of("to", "mallory", "amount", "5");
        assertEquals(403, bank.handle(post(cookie, Map.of("sec-fetch-site", "cross-site"), form)).status());
        assertEquals(403, bank.handle(post(cookie, Map.of("sec-fetch-site", "same-site"), form)).status());
        assertEquals(200, bank.handle(post(cookie, Map.of("sec-fetch-site", "same-origin"), form)).status());
        assertEquals(200, bank.handle(post(cookie, Map.of("sec-fetch-site", "none"), form)).status());
    }

    @Test
    void withoutFetchMetadataTheOriginDecides() {
        Bank bank = new Bank(Protection.none().withFetchChecks());
        String cookie = login(bank);
        Map<String, String> form = Map.of("to", "mallory", "amount", "5");
        assertEquals(403, bank.handle(post(cookie, Map.of("origin", "https://evil.example"), form)).status());
        assertEquals(200, bank.handle(post(cookie, Map.of("origin", Bank.ORIGIN), form)).status());
        assertEquals(200, bank.handle(post(cookie, Map.of(), form)).status());
    }

    @Test
    void getNeverMovesMoneyUnlessTheBankIsWrong() {
        Bank safe = new Bank(Protection.none());
        assertEquals(405, safe.handle(new Request("GET", "/transfer", Map.of("cookie", login(safe)), Map.of("to", "mallory", "amount", "5"))).status());
        Bank unsafe = new Bank(Protection.none().withGetTransfers());
        assertEquals(200, unsafe.handle(new Request("GET", "/transfer", Map.of("cookie", login(unsafe)), Map.of("to", "mallory", "amount", "5"))).status());
    }

    @Test
    void transfersAreValidated() {
        Bank bank = new Bank(Protection.none());
        String cookie = login(bank);
        assertEquals(400, bank.handle(post(cookie, Map.of(), Map.of("to", "mallory", "amount", "-5"))).status());
        assertEquals(400, bank.handle(post(cookie, Map.of(), Map.of("to", "mallory", "amount", "5000"))).status());
        assertEquals(400, bank.handle(post(cookie, Map.of(), Map.of("to", "nobody", "amount", "5"))).status());
        assertEquals(400, bank.handle(post(cookie, Map.of(), Map.of("to", "mallory", "amount", "five"))).status());
        assertEquals(401, bank.handle(new Request("POST", "/transfer", Map.of(), Map.of("to", "mallory", "amount", "5"))).status());
    }

    @Test
    void theApiNeedsABearerToken() {
        Bank bank = new Bank(Protection.none());
        String token = bank.issueApiToken("ana");
        Map<String, String> form = Map.of("to", "mallory", "amount", "5");
        assertEquals(401, bank.handle(new Request("POST", "/api/transfer", Map.of("cookie", login(bank)), form)).status());
        assertEquals(200, bank.handle(new Request("POST", "/api/transfer", Map.of("authorization", "Bearer " + token), form)).status());
    }
}
