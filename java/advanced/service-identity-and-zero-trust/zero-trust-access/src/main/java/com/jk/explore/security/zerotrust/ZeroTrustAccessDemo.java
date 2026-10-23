package com.jk.explore.security.zerotrust;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.security.SecureRandom;

/**
 * A bank's internal ledger API. Before: reachable by anything inside the network, no identity. After: an identity-aware proxy that checks a Keycloak
 * token, asks OPA for a decision and writes one audit record per request. Every number printed below was measured against a real Keycloak
 * (Docker), a real opa server and real HTTP requests.
 */
public final class ZeroTrustAccessDemo {

    private static final String AUDIENCE = "ledger-api";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The requests the demo sent to the proxy, in order, and what the caller saw. */
    private record Sent(String method, String path, int status) {
    }

    private ZeroTrustAccessDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Path lab = Path.of("build", "lab").toAbsolutePath();
        Files.createDirectories(lab);
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        String mark = HexFormat.of().formatHex(bytes);
        try (Ledger open = Ledger.open(); Ledger behind = Ledger.behindProxy(mark)) {
            perimeter(out, open);
            Path policy = lab.resolve("policy");
            copyPolicy(policy);
            int[] tests = Opa.test(policy);
            try (Keycloak kc = Keycloak.start(Path.of("lab", "bank-realm.json")); Opa opa = Opa.start(policy.resolve("ledger.rego"), lab.resolve("opa.log"))) {
                Map<String, PublicKey> keys = kc.signingKeys();
                realm(out, kc, keys, tests);
                AuditLog audit = new AuditLog(lab.resolve("audit.log"));
                List<Sent> sent = new ArrayList<>();
                List<String> tokens = new ArrayList<>();
                try (Proxy proxy = new Proxy(keys, kc.issuer(), AUDIENCE, opa, behind.uri(), mark, audit, Clock.systemUTC())) {
                    Door door = new Door(proxy.uri(), sent);
                    requests(out, kc, keys, proxy, door, behind, tokens);
                    after(out, door, behind);
                    revocation(out, kc, keys, proxy, door, tokens);
                    outages(out, kc, opa, proxy, door, tokens);
                }
                audit(out, audit, sent, tokens);
            }
            bill(out);
        }
        return out.toString();
    }

    // ---- the proxy as the client sees it

    private record Door(URI base, List<Sent> sent) {

        int call(String method, String path, String token) throws IOException, InterruptedException {
            int status = send(base, method, path, token);
            sent.add(new Sent(method, path, status));
            return status;
        }
    }

    private static int send(URI base, String method, String path, String token) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(base.resolve(path)).method(method, HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static void copyPolicy(Path dir) throws IOException {
        Files.createDirectories(dir);
        for (String f : List.of("ledger.rego", "ledger_test.rego")) {
            Files.copy(Path.of("policy", f), dir.resolve(f), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The ten requests of the perimeter comparison: none of them carries an identity. */
    private static final List<String[]> NO_IDENTITY = List.of(
            new String[] {"GET", "/accounts/1001/balance"}, new String[] {"GET", "/accounts/1002/balance"}, new String[] {"GET", "/accounts/1003/balance"},
            new String[] {"POST", "/transfers"}, new String[] {"POST", "/transfers"}, new String[] {"POST", "/transfers"},
            new String[] {"GET", "/audit/entries"}, new String[] {"GET", "/audit/entries"},
            new String[] {"POST", "/admin/freeze"}, new String[] {"POST", "/admin/freeze"});

    // ---- 1. the before

    private static void perimeter(StringBuilder out, Ledger open) throws IOException, InterruptedException {
        line(out, "--- before: the perimeter");
        line(out, "the ledger API listens inside the network and answers whoever can reach it; the firewall is the only control");
        int ok = 0;
        for (String[] r : NO_IDENTITY) {
            if (send(open.uri(), r[0], r[1], null) == 200) {
                ok++;
            }
        }
        line(out, NO_IDENTITY.size() + " requests with no identity (3 balances, 3 transfers, 2 audit exports, 2 freezes): " + ok + " succeed");
        line(out, "the ledger's code ran " + open.executed() + " times, " + open.transfers() + " of them transfers, for callers nobody can name");
        line(out, "the ledger can log an address; it cannot say which person did it, or whether that person was allowed");
    }

    // ---- 2. the parts

    private static void realm(StringBuilder out, Keycloak kc, Map<String, PublicKey> keys, int[] tests) throws Exception {
        line(out, "--- identity from Keycloak, decisions from OPA");
        JsonNode realm = JSON.readTree(Files.readString(Path.of("lab", "bank-realm.json")));
        line(out, "Keycloak " + Keycloak.VERSION + " (start-dev in Docker) imported the realm bank from a file: "
                + realm.path("users").size() + " users, " + realm.path("clients").size() + " clients, "
                + realm.path("roles").path("realm").size() + " roles");
        line(out, "users: alice (teller), bob (auditor), carol (admin); the device posture is a claim named compliant, set by the client the token was asked through");
        line(out, "signing keys the proxy learned from the JWKS: " + keys.size());
        line(out, "the policy is one Rego file run by the opa binary as a server; its own tests: " + tests[0] + " of " + tests[1] + " pass");
        Keycloak.Tokens alice = kc.login("ledger-managed", "alice");
        Jwt.Result r = Jwt.verify(alice.access(), keys, kc.issuer(), AUDIENCE, Instant.now());
        long life = ((Number) r.claims().get("exp")).longValue() - ((Number) r.claims().get("iat")).longValue();
        line(out, "alice's token: valid " + yes(r.valid()) + ", audience " + r.claims().get("aud") + ", roles "
                + ((Map<?, ?>) r.claims().get("realm_access")).get("roles") + ", compliant " + r.claims().get("compliant") + ", lifetime " + life + " seconds");
    }

    // ---- 3. every request, one decision

    private static void requests(StringBuilder out, Keycloak kc, Map<String, PublicKey> keys, Proxy proxy, Door door, Ledger behind, List<String> tokens)
            throws Exception {
        line(out, "--- every request is authenticated, authorized and logged");
        Keycloak.Tokens shortLived = kc.login("ledger-short", "alice");
        Keycloak.Tokens alice = kc.login("ledger-managed", "alice");
        Keycloak.Tokens bob = kc.login("ledger-managed", "bob");
        Keycloak.Tokens carol = kc.login("ledger-managed", "carol");
        Keycloak.Tokens unmanaged = kc.login("ledger-unmanaged", "alice");
        Keycloak.Tokens otherAudience = kc.login("reports-ui", "alice");
        for (Keycloak.Tokens t : List.of(shortLived, alice, bob, carol, unmanaged, otherAudience)) {
            tokens.add(t.access());
            tokens.add(t.refresh());
        }
        String balance = "/accounts/1001/balance";
        line(out, "-- authentication: who is calling");
        line(out, "no token, GET " + balance + ": " + door.call("GET", balance, null));
        waitUntilExpired(shortLived.access(), keys, kc.issuer());
        line(out, "alice's token after its 4 second lifetime, GET " + balance + ": " + door.call("GET", balance, shortLived.access()));
        line(out, "alice's token made for another audience (reports-api), GET " + balance + ": " + door.call("GET", balance, otherAudience.access()));
        line(out, "bob's token with the roles edited to admin and the old signature, POST /admin/freeze: "
                + door.call("POST", "/admin/freeze", Forge.withEditedClaims(bob.access(), Map.of("realm_access", Map.of("roles", List.of("admin"))))));
        line(out, "alice's token with one character of the signature changed, GET " + balance + ": "
                + door.call("GET", balance, Forge.withFlippedSignature(alice.access())));
        Map<String, Object> header = Map.of("alg", "RS256", "typ", "JWT", "kid", keys.keySet().iterator().next());
        Map<String, Object> admin = Forge.claims(kc.issuer(), AUDIENCE, "mallory", List.of("admin"), true, Instant.now().getEpochSecond() + 300);
        line(out, "a token the attacker signed with their own key, claiming admin with the right issuer and audience, POST /admin/freeze: "
                + door.call("POST", "/admin/freeze", Forge.token(header, admin, Forge.newKey())));
        line(out, "the same token with the algorithm none and no signature, POST /admin/freeze: "
                + door.call("POST", "/admin/freeze", Forge.token(Map.of("alg", "none", "typ", "JWT"), admin, null)));
        line(out, "-- authorization: may this person do this");
        line(out, "alice (teller), GET " + balance + ": " + door.call("GET", balance, alice.access()));
        line(out, "alice (teller), POST /transfers at 10:00: " + door.call("POST", "/transfers", alice.access()));
        proxy.setHour(() -> 23);
        line(out, "alice (teller), POST /transfers at 23:00: " + door.call("POST", "/transfers", alice.access()));
        proxy.setHour(() -> 10);
        line(out, "bob (auditor), GET /audit/entries: " + door.call("GET", "/audit/entries", bob.access()));
        line(out, "bob (auditor), POST /transfers: " + door.call("POST", "/transfers", bob.access()));
        line(out, "alice (teller), GET /audit/entries: " + door.call("GET", "/audit/entries", alice.access()));
        line(out, "carol (admin), POST /admin/freeze: " + door.call("POST", "/admin/freeze", carol.access()));
        line(out, "carol (admin), POST /transfers: " + door.call("POST", "/transfers", carol.access()));
        line(out, "alice (teller), GET /nope: " + door.call("GET", "/nope", alice.access()));
        line(out, "-- the device: is the machine one the bank manages");
        line(out, "alice (teller) on a compliant device, GET " + balance + ": " + door.call("GET", balance, alice.access()));
        line(out, "alice (teller) on a device that is not compliant, GET " + balance + ": " + door.call("GET", balance, unmanaged.access()));
        int allowed = (int) door.sent().stream().filter(s -> s.status() == 200).count();
        line(out, "requests sent: " + door.sent().size() + ", answered 200: " + allowed + ", reached the ledger's code: " + behind.executed()
                + " (only the allowed ones get past the proxy)");
    }

    private static void waitUntilExpired(String token, Map<String, PublicKey> keys, String issuer) throws Exception {
        while (Jwt.verify(token, keys, issuer, AUDIENCE, Instant.now()).valid()) {
            Thread.sleep(250);
        }
    }

    // ---- 4. the after

    private static void after(StringBuilder out, Door door, Ledger behind) throws IOException, InterruptedException {
        line(out, "--- the same ten requests with no identity, now");
        int before = behind.executed();
        int viaProxy = 0;
        int direct = 0;
        for (String[] r : NO_IDENTITY) {
            if (door.call(r[0], r[1], null) == 200) {
                viaProxy++;
            }
            if (send(behind.uri(), r[0], r[1], null) == 200) {
                direct++;
            }
        }
        line(out, "through the proxy: " + viaProxy + " succeed (before: " + NO_IDENTITY.size() + ")");
        line(out, "straight to the ledger from inside the network, skipping the proxy: " + direct + " succeed (the ledger wants the proxy's mark)");
        line(out, "the ledger's code ran " + (behind.executed() - before) + " more times for callers without an identity");
    }

    // ---- 5. a token lives until it expires

    private static void revocation(StringBuilder out, Keycloak kc, Map<String, PublicKey> keys, Proxy proxy, Door door, List<String> tokens) throws Exception {
        line(out, "--- revocation: how long a token outlives its session");
        String balance = "/accounts/1001/balance";
        Keycloak.Tokens standard = kc.login("ledger-managed", "alice");
        tokens.add(standard.access());
        tokens.add(standard.refresh());
        line(out, "alice's token (300 second lifetime), GET " + balance + ": " + door.call("GET", balance, standard.access()));
        kc.logout("ledger-managed", standard);
        line(out, "alice's session is ended at Keycloak; the same token, GET " + balance + ": " + door.call("GET", balance, standard.access())
                + " (the proxy checks signature and expiry, and both are still good)");
        proxy.setSessionCheck(kc::sessionActive);
        int checks = kc.sessionChecks();
        Keycloak.Tokens fresh = kc.login("ledger-managed", "alice");
        Keycloak.Tokens bob = kc.login("ledger-managed", "bob");
        tokens.add(fresh.access());
        tokens.add(fresh.refresh());
        tokens.add(bob.access());
        tokens.add(bob.refresh());
        line(out, "with a session check at Keycloak on every request, the ended session's token: " + door.call("GET", balance, standard.access()));
        line(out, "with the session check, a token from a new login: " + door.call("GET", balance, fresh.access()));
        line(out, "with the session check, bob, GET /audit/entries: " + door.call("GET", "/audit/entries", bob.access()));
        line(out, "calls to Keycloak for those 3 requests: " + (kc.sessionChecks() - checks) + " (a request refused before the session check costs none)");
        proxy.setSessionCheck(null);
        Keycloak.Tokens shortLived = kc.login("ledger-short", "alice");
        tokens.add(shortLived.access());
        tokens.add(shortLived.refresh());
        line(out, "a token with a 4 second lifetime, GET " + balance + ": " + door.call("GET", balance, shortLived.access()));
        kc.logout("ledger-short", shortLived);
        line(out, "its session is ended; at once, with no session check: " + door.call("GET", balance, shortLived.access()));
        waitUntilExpired(shortLived.access(), keys, kc.issuer());
        line(out, "after its lifetime, with no session check: " + door.call("GET", balance, shortLived.access()) + " (the exposure was at most 4 seconds, against 300 for the standard token)");
    }

    // ---- 6. when a part is down

    private static void outages(StringBuilder out, Keycloak kc, Opa opa, Proxy proxy, Door door, List<String> tokens) throws Exception {
        line(out, "--- when a part is down");
        String balance = "/accounts/1001/balance";
        Keycloak.Tokens alice = kc.login("ledger-managed", "alice");
        tokens.add(alice.access());
        tokens.add(alice.refresh());
        kc.stop();
        line(out, "Keycloak stopped; alice's token issued before, GET " + balance + ": " + door.call("GET", balance, alice.access())
                + " (the keys are cached and the signature needs no call)");
        proxy.setSessionCheck(kc::sessionActive);
        line(out, "Keycloak stopped, with the session check on: " + door.call("GET", balance, alice.access()) + " (it cannot confirm the session, so it refuses)");
        proxy.setSessionCheck(null);
        opa.close();
        line(out, "OPA stopped, a valid token, GET " + balance + ": " + door.call("GET", balance, alice.access()) + " (no decision is a no)");
    }

    // ---- 7. the audit

    private static void audit(StringBuilder out, AuditLog audit, List<Sent> sent, List<String> tokens) throws IOException {
        line(out, "--- the audit log");
        List<AuditLog.Entry> entries = audit.entries();
        boolean oneEach = entries.size() == sent.size();
        int matching = 0;
        for (int i = 0; i < Math.min(entries.size(), sent.size()); i++) {
            AuditLog.Entry e = entries.get(i);
            Sent s = sent.get(i);
            if (e.seq() == i + 1 && e.method().equals(s.method()) && e.path().equals(s.path()) && e.status() == s.status()) {
                matching++;
            }
        }
        line(out, "requests sent to the proxy: " + sent.size() + ", decision records written: " + entries.size());
        line(out, "records that match the request in order, with the status the caller saw: " + matching + " of " + sent.size());
        line(out, "every request has exactly one record: " + yes(oneEach && matching == sent.size()));
        long allow = entries.stream().filter(e -> e.decision().equals("allow")).count();
        line(out, "allow: " + allow + ", deny: " + (entries.size() - allow));
        Map<String, Integer> reasons = new TreeMap<>();
        for (AuditLog.Entry e : entries) {
            reasons.merge(e.reason(), 1, Integer::sum);
        }
        reasons.forEach((reason, n) -> line(out, "  " + reason + ": " + n));
        String raw = audit.raw();
        long leaks = tokens.stream().filter(raw::contains).count() + (raw.contains("eyJ") ? 1 : 0);
        line(out, "tokens (access or refresh) found in the log file: " + leaks + " of " + tokens.size());
        long named = entries.stream().filter(e -> !e.user().equals("-")).count();
        line(out, "records that name the user: " + named + " (a forged or unreadable token names no one: its claims are not trusted)");
        line(out, "each record holds: seq, decision, status, reason, user, method, path and a timestamp");
    }

    private static void bill(StringBuilder out) {
        line(out, "--- the bill");
        line(out, "the proxy is the one door: the ledger turns away anything without its mark, and a mark in a flat network is a weak link; production wants mutual TLS between proxy and ledger");
        line(out, "a token stays valid until it expires: a short lifetime and a session check shrink that window, and each costs a refresh or a call per request");
        line(out, "Keycloak and OPA are on the path of every request; the proxy fails closed when OPA is gone, and keeps serving old tokens when Keycloak is gone");
        line(out, "the device posture here is a claim the demo sets per client; a real one comes from a device management system and is only as good as it");
        line(out, "no Envoy and no oauth2-proxy was run: the proxy is a small Java program so that every step shows, and either of them would do the same job in front of a real ledger");
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }
}
