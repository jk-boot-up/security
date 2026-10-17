package com.jk.explore.security.securitylogging;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One morning at the online bank, a minute per event: Ana logs in, someone else guesses her password from another address and moves
 * money to an unknown account, Ben is refused another customer's account, Carol changes a role, and Ana pays by card. Some events
 * carry secrets that must never reach the log.
 */
final class BankDay {

    static final String STOLEN_FROM = "198.51.100.66";
    static final String PASSWORD_TRIED = "Summer2026!";
    static final String SESSION_ID = "sess-7f3a9c41d2e8";
    static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmEifQ.c2lnbmF0dXJl";
    static final String CARD = "4111111111111111";

    /** Every secret in the morning's events, each of which must be absent from any log line. */
    static final List<String> SECRETS = List.of(PASSWORD_TRIED, SESSION_ID, TOKEN, CARD, "4111-1111-1111-1111");

    private BankDay() {
    }

    static List<SecurityEvent> events(SimClock clock) {
        List<SecurityEvent> events = new ArrayList<>();
        int request = 0;
        record Step(EventType type, String actor, String action, String target, String source, Outcome outcome, Map<String, String> details) {
        }
        List<Step> steps = List.of(
                new Step(EventType.LOGIN, "ana", "login", "ana", "203.0.113.7", Outcome.SUCCESS, Map.of("method", "password and code")),
                new Step(EventType.LOGIN, "ana", "login", "ana", STOLEN_FROM, Outcome.FAILURE,
                        Map.of("reason", "wrong password", "password", PASSWORD_TRIED)),
                new Step(EventType.LOGIN, "ana", "login", "ana", STOLEN_FROM, Outcome.FAILURE, Map.of("reason", "wrong password")),
                new Step(EventType.LOGIN, "ana", "login", "ana", STOLEN_FROM, Outcome.SUCCESS, Map.of("session", SESSION_ID)),
                new Step(EventType.ACCESS_DECISION, "ben", "GET /accounts/ANA-1", "ANA-1", "203.0.113.20", Outcome.DENIED,
                        Map.of("rule", "owner only")),
                new Step(EventType.MONEY_MOVEMENT, "ana", "transfer", "ANA-1 -> MAL-9", STOLEN_FROM, Outcome.SUCCESS,
                        Map.of("amount", "500", "authorization", "Bearer " + TOKEN)),
                new Step(EventType.ADMIN_CHANGE, "carol", "grant role support", "dave", "10.0.0.5", Outcome.SUCCESS, Map.of()),
                new Step(EventType.MONEY_MOVEMENT, "ana", "card payment", "store BOOKS-4", "203.0.113.7", Outcome.SUCCESS,
                        Map.of("card", CARD, "amount", "80", "note", "paid with 4111-1111-1111-1111")));
        for (Step step : steps) {
            request++;
            events.add(new SecurityEvent(clock.now(), step.type(), step.actor(), step.action(), step.target(), step.source(), step.outcome(),
                    "req-%04d".formatted(request), ordered(step.details())));
            clock.advance(Duration.ofMinutes(1));
        }
        return events;
    }

    /** Map.of has no order; the log's fields should read the same way on every run. */
    private static Map<String, String> ordered(Map<String, String> details) {
        return new TreeMap<>(details);
    }

    /** The same morning as the old logger writes it. */
    static void oldLogger(PlainLogger log, SimClock clock) {
        String[] lines = {"INFO login ok", "ERROR login failed", "ERROR login failed", "INFO login ok", "WARN forbidden",
            "INFO transfer done amount=500", "INFO role changed", "INFO payment card=" + CARD + " amount=80"};
        for (String line : lines) {
            String level = line.substring(0, line.indexOf(' '));
            String message = line.substring(line.indexOf(' ') + 1);
            switch (level) {
                case "ERROR" -> log.error(message);
                case "WARN" -> log.warn(message);
                default -> log.info(message);
            }
            clock.advance(Duration.ofMinutes(1));
        }
    }
}
