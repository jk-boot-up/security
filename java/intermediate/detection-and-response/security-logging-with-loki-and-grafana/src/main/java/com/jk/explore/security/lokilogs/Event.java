package com.jk.explore.security.lokilogs;

import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** One security event of the bank: who did what to what, from where, and how it ended. Written as a JSON line, which is what is shipped. */
public record Event(String requestId, String eventType, String actor, String action, String resource, String sourceIp, String outcome, Integer amount) {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** The line: fields in a fixed order, the amount only when there is one. */
    public String line() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("request_id", requestId);
        fields.put("event_type", eventType);
        fields.put("actor", actor);
        fields.put("action", action);
        fields.put("resource", resource);
        fields.put("source_ip", sourceIp);
        fields.put("outcome", outcome);
        if (amount != null) {
            fields.put("amount", amount);
        }
        return JSON.writeValueAsString(fields);
    }

    /** The morning of the plain security-logging project: ana's usual login, then someone with her password from another address who moves 500 to MAL-9. */
    public static java.util.List<Event> morning() {
        return java.util.List.of(
                new Event("req-0001", "login", "ana", "login", "session", "203.0.113.5", "success", null),
                new Event("req-0002", "access-decision", "ana", "view-balance", "ANA-1", "203.0.113.5", "success", null),
                new Event("req-0003", "login", "ana", "login", "session", "198.51.100.66", "failure", null),
                new Event("req-0004", "login", "ana", "login", "session", "198.51.100.66", "failure", null),
                new Event("req-0005", "login", "ana", "login", "session", "198.51.100.66", "success", null),
                new Event("req-0006", "money-movement", "ana", "transfer", "ANA-1->MAL-9", "198.51.100.66", "success", 500),
                new Event("req-0007", "admin-change", "admin", "add-payee", "MAL-9", "203.0.113.50", "success", null),
                new Event("req-0008", "access-decision", "ben", "view-account", "ANA-1", "203.0.113.77", "denied", null));
    }
}
