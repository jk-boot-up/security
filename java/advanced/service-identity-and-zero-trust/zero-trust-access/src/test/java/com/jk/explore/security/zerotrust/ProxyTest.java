package com.jk.explore.security.zerotrust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The proxy, the ledger and the audit log with a stand-in for OPA and a key made here: no Docker, no tools. */
class ProxyTest {

    private static final String ISSUER = "https://sso.bank.example/realms/bank";

    private KeyPair key;
    private Ledger ledger;
    private Proxy proxy;
    private AuditLog audit;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void start() throws Exception {
        key = Forge.newKey();
        ledger = Ledger.behindProxy("the-mark");
        audit = new AuditLog(Files.createTempFile("audit", ".log"));
        // allow only a teller on a compliant device to POST /transfers
        Decider decider = in -> {
            boolean ok = in.get("roles") instanceof List<?> l && l.contains("teller") && Boolean.TRUE.equals(in.get("device_compliant"))
                    && "POST".equals(in.get("method")) && "/transfers".equals(in.get("path"));
            return new Decider.Decision(ok, ok ? List.of() : List.of("no rule"));
        };
        proxy = new Proxy(Map.of("k1", key.getPublic()), ISSUER, "ledger-api", decider, ledger.uri(), "the-mark", audit, Clock.systemUTC());
    }

    @AfterEach
    void stop() {
        proxy.close();
        ledger.close();
    }

    private String token(String audience, boolean compliant, long ttl) throws Exception {
        return Forge.token(Map.of("alg", "RS256", "kid", "k1"),
                Forge.claims(ISSUER, audience, "alice", List.of("teller"), compliant, Instant.now().getEpochSecond() + ttl), key);
    }

    private int call(String method, String path, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(proxy.uri() + path)).method(method, HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return http.send(b.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void theStatusCodesFollowTheThreeQuestions() throws Exception {
        assertEquals(401, call("POST", "/transfers", null));
        assertEquals(401, call("POST", "/transfers", token("ledger-api", true, -5)));
        assertEquals(401, call("POST", "/transfers", token("reports-api", true, 60)));
        assertEquals(403, call("GET", "/audit/entries", token("ledger-api", true, 60)));
        assertEquals(403, call("POST", "/transfers", token("ledger-api", false, 60)));
        assertEquals(200, call("POST", "/transfers", token("ledger-api", true, 60)));
        assertEquals(1, ledger.executed());
    }

    @Test
    void everyRequestLeavesExactlyOneRecordAndNoToken() throws Exception {
        String good = token("ledger-api", true, 60);
        call("POST", "/transfers", good);
        call("POST", "/transfers", null);
        call("GET", "/audit/entries", good);
        call("POST", "/transfers", token("reports-api", true, 60));
        List<AuditLog.Entry> entries = audit.entries();
        assertEquals(4, entries.size());
        assertEquals(List.of("allow", "deny", "deny", "deny"), entries.stream().map(AuditLog.Entry::decision).toList());
        assertEquals(List.of(200, 401, 403, 401), entries.stream().map(AuditLog.Entry::status).toList());
        assertEquals(4, audit.raw().lines().count());
        assertFalse(audit.raw().contains(good));
        assertFalse(audit.raw().contains("eyJ"));
        assertTrue(audit.raw().contains("\"user\":\"alice\""));
    }

    @Test
    void aSessionCheckRefusesATokenWhoseSessionEnded() throws Exception {
        String t = token("ledger-api", true, 60);
        assertEquals(200, call("POST", "/transfers", t));
        proxy.setSessionCheck(x -> false);
        assertEquals(401, call("POST", "/transfers", t));
        assertEquals("session ended", audit.entries().get(1).reason());
    }

    @Test
    void whenTheDecisionFailsTheAnswerIsNo() throws Exception {
        proxy.close();
        Decider broken = in -> {
            throw new java.io.IOException("down");
        };
        proxy = new Proxy(Map.of("k1", key.getPublic()), ISSUER, "ledger-api", broken, ledger.uri(), "the-mark", audit, Clock.systemUTC());
        assertEquals(503, call("POST", "/transfers", token("ledger-api", true, 60)));
        assertEquals(0, ledger.executed());
        assertEquals(1, audit.entries().size());
    }

    @Test
    void theLedgerBehindTheProxyRefusesAnyoneElse() throws Exception {
        HttpResponse<Void> r = http.send(HttpRequest.newBuilder(ledger.uri().resolve("/transfers")).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(403, r.statusCode());
        assertEquals(0, ledger.executed());
    }

    @Test
    void thePerimeterLedgerAnswersAnyone() throws Exception {
        try (Ledger open = Ledger.open()) {
            HttpResponse<Void> r = http.send(HttpRequest.newBuilder(open.uri().resolve("/transfers")).POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertEquals(200, r.statusCode());
            assertEquals(1, open.transfers());
        }
    }

    @Test
    void theRealmFileHoldsOnlyDemoUsersAndNoKeys() throws Exception {
        String realm = Files.readString(Path.of("lab", "bank-realm.json"));
        assertFalse(realm.contains("PRIVATE KEY"));
        assertFalse(realm.contains("\"secret\""));
        assertTrue(realm.contains("demopassword"));
    }
}
