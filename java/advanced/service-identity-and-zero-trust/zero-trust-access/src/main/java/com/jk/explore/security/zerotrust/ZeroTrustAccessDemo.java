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
import java.nio.file.StandardCopyOption;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A bank's internal ledger API. Before: reachable by anything inside the network, no identity. After: an identity-aware proxy that checks a Keycloak
 * token, asks OPA for a decision and writes one audit record per request. Every number printed below was measured against a real Keycloak
 * (Docker), a real opa server and real HTTP requests; ports, tokens, key ids and times are never printed, so the output is the same on every run.
 */
public final class ZeroTrustAccessDemo {

    private static final String AUDIENCE = "ledger-api";
    private static final String BALANCE = "/accounts/1001/balance";
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
        Opa.requirePinnedVersion();
        Path lab = Path.of("build", "lab").toAbsolutePath();
        Files.createDirectories(lab);
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        String mark = HexFormat.of().formatHex(bytes);
        try (Ledger open = Ledger.open(); Ledger behind = Ledger.behindProxy(mark)) {
            Path policy = lab.resolve("policy");
            copyPolicy(policy);
            int[] tests = Opa.test(policy);
            try (Keycloak kc = Keycloak.start(Path.of("lab", "bank-realm.json")); Opa opa = Opa.start(policy.resolve("ledger.rego"), lab.resolve("opa.log"))) {
                Map<String, PublicKey> keys = kc.signingKeys();
                perimeter(out, open, keys, tests);
                AuditLog audit = new AuditLog(lab.resolve("audit.log"));
                List<Sent> sent = new ArrayList<>();
                List<String> tokens = new ArrayList<>();
                try (Proxy proxy = new Proxy(keys, kc.issuer(), AUDIENCE, opa, behind.uri(), mark, audit, Clock.systemUTC())) {
                    Door door = new Door(proxy.uri(), sent, audit);
                    authentication(out, kc, keys, door, behind, tokens);
                    authorization(out, kc, opa, proxy, door, behind, tokens);
                    revocation(out, kc, keys, opa, proxy, door, tokens);
                }
                audit(out, audit, sent, tokens);
            }
        }
        return out.toString();
    }

    // ---- the proxy as the client sees it

    private record Door(URI base, List<Sent> sent, AuditLog audit) {

        int call(String method, String path, String token) throws IOException, InterruptedException {
            int status = send(base, method, path, token);
            sent.add(new Sent(method, path, status));
            return status;
        }

        /** Sends the request and returns the status with the reason the proxy wrote in its record. */
        String answer(String method, String path, String token) throws IOException, InterruptedException {
            int status = call(method, path, token);
            List<AuditLog.Entry> all = audit.entries();
            String reason = all.get(all.size() - 1).reason();
            return status + (reason.equals("allowed") ? "" : "  " + reason);
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
            Files.copy(Path.of("policy", f), dir.resolve(f), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The ten requests of the perimeter comparison: none of them carries an identity. */
    private static final List<String[]> NO_IDENTITY = List.of(
            new String[] {"GET", "/accounts/1001/balance"}, new String[] {"GET", "/accounts/1002/balance"}, new String[] {"GET", "/accounts/1003/balance"},
            new String[] {"POST", "/transfers"}, new String[] {"POST", "/transfers"}, new String[] {"POST", "/transfers"},
            new String[] {"GET", "/audit/entries"}, new String[] {"GET", "/audit/entries"},
            new String[] {"POST", "/admin/freeze"}, new String[] {"POST", "/admin/freeze"});

    // ---- ONE. the before, and the parts of the after

    private static void perimeter(StringBuilder out, Ledger open, Map<String, PublicKey> keys, int[] tests) throws IOException, InterruptedException {
        act(out, "ONE", "Before: the ledger trusts the network.");
        head(out, "THE REQUESTS (" + NO_IDENTITY.size() + ", none with an identity):");
        Map<String, Integer> kinds = new LinkedHashMap<>();
        for (String[] r : NO_IDENTITY) {
            kinds.merge(String.format("%-4s %s", r[0], r[1].replaceAll("/[0-9]+/", "/{id}/")), 1, Integer::sum);
        }
        kinds.forEach((k, n) -> row(out, 30, k, String.valueOf(n)));
        int ok = 0;
        for (String[] r : NO_IDENTITY) {
            if (send(open.uri(), r[0], r[1], null) == 200) {
                ok++;
            }
        }
        head(out, "THE RESULT (sent straight to the ledger):");
        row(out, 12, "succeed", ok + " of " + NO_IDENTITY.size());
        row(out, 12, "ledger ran", open.executed() + " times, " + open.transfers() + " of them transfers");
        row(out, 12, "who", "unknown: the ledger sees an address, not a person");
        JsonNode realm = JSON.readTree(Files.readString(Path.of("lab", "bank-realm.json")));
        head(out, "THE PARTS OF THE AFTER:");
        row(out, 17, "Keycloak " + Keycloak.VERSION, "realm bank: " + realm.path("users").size() + " users, "
                + realm.path("clients").size() + " clients, " + realm.path("roles").path("realm").size() + " roles");
        List<String> users = new ArrayList<>();
        realm.path("users").forEach(u -> users.add(u.path("username").asText() + " " + u.path("realmRoles").get(0).asText()));
        row(out, 17, "users", String.join(", ", users));
        row(out, 17, "OPA " + Opa.VERSION, "policy/ledger.rego; its tests: " + tests[0] + " of " + tests[1] + " pass");
        row(out, 17, "proxy", keys.size() + " signing key, learned from the JWKS");
        row(out, 17, "device", "a claim, compliant, set per Keycloak client");
    }

    // ---- TWO. who is calling

    private static void authentication(StringBuilder out, Keycloak kc, Map<String, PublicKey> keys, Door door, Ledger behind, List<String> tokens)
            throws Exception {
        act(out, "TWO", "Who is calling: the token, checked by hand.");
        Keycloak.Tokens shortLived = kc.login("ledger-short", "alice");
        Keycloak.Tokens alice = kc.login("ledger-managed", "alice");
        Keycloak.Tokens bob = kc.login("ledger-managed", "bob");
        Keycloak.Tokens otherAudience = kc.login("reports-ui", "alice");
        for (Keycloak.Tokens t : List.of(shortLived, alice, bob, otherAudience)) {
            tokens.add(t.access());
            tokens.add(t.refresh());
        }
        Jwt.Result r = Jwt.verify(alice.access(), keys, kc.issuer(), AUDIENCE, Instant.now());
        Map<?, ?> header = JSON.readValue(Base64.getUrlDecoder().decode(alice.access().split("\\.")[0]), Map.class);
        long life = ((Number) r.claims().get("exp")).longValue() - ((Number) r.claims().get("iat")).longValue();
        String issuer = String.valueOf(r.claims().get("iss"));
        head(out, "THE TOKEN (alice, asked through the client ledger-managed):");
        row(out, 9, "form", "header.claims.signature, each part base64url");
        row(out, 9, "header", "alg " + header.get("alg") + ", typ " + header.get("typ") + ", kid: " + (keys.containsKey(header.get("kid")) ? "a key in the JWKS" : "unknown"));
        row(out, 9, "claims", "iss ..." + issuer.substring(issuer.indexOf("/realms/")) + ", aud " + r.claims().get("aud"));
        row(out, 9, "", "preferred_username " + r.user() + ", roles " + ((Map<?, ?>) r.claims().get("realm_access")).get("roles"));
        row(out, 9, "", "compliant " + r.claims().get("compliant") + ", exp - iat = " + life + " seconds");

        RSAPublicKey key = (RSAPublicKey) keys.get(String.valueOf(header.get("kid")));
        Rs256Steps.Steps s = Rs256Steps.check(alice.access(), key);
        head(out, "HOW THE SIGNATURE IS CHECKED (RS256, by hand):");
        row(out, 9, "key", "RSA " + s.keyBits() + " bits from the JWKS: n, and e = " + s.e());
        row(out, 9, "s", "the signature: " + s.signatureBytes() + " bytes");
        row(out, 9, "m", "s^e mod n: " + s.blockBytes() + " bytes");
        row(out, 9, "m holds", "00 01, " + s.ffBytes() + " x FF, 00, label (" + Rs256Steps.SHA256_LABEL.length + " bytes), hash (32)");
        row(out, 9, "label", "3031...0420: a SHA-256 hash follows: " + yes(s.label()));
        row(out, 9, "hash", "SHA-256(header.claims) = the last 32 bytes: " + yes(s.hash()));
        row(out, 9, "JDK", "Signature SHA256withRSA agrees: " + yes(jdk(alice.access(), key) == s.valid()));
        row(out, 9, "then", "iss, aud and exp: " + (r.valid() ? "good, the token is valid" : r.reason()));

        String edited = Forge.withEditedClaims(bob.access(), Map.of("realm_access", Map.of("roles", List.of("admin"))));
        Map<String, Object> forgedHeader = Map.of("alg", "RS256", "typ", "JWT", "kid", String.valueOf(header.get("kid")));
        Map<String, Object> admin = Forge.claims(kc.issuer(), AUDIENCE, "mallory", List.of("admin"), true, Instant.now().getEpochSecond() + 300);
        String forged = Forge.token(forgedHeader, admin, Forge.newKey());
        head(out, "THE SAME STEPS ON TWO FORGERIES:");
        row(out, 30, "token", "padding  hash  JDK");
        for (String[] t : List.of(new String[] {"alice's, as issued", alice.access()}, new String[] {"bob's, roles edited to admin", edited},
                new String[] {"mallory's, own key", forged})) {
            Rs256Steps.Steps f = Rs256Steps.check(t[1], key);
            row(out, 30, t[0], String.format("%-9s%-6s%s", yes(f.padding()), yes(f.hash()), jdk(t[1], key) ? "valid" : "refused"));
        }

        head(out, "THE BAD TOKENS AT THE PROXY (status, reason):");
        int w = 38;
        row(out, w, "no token", door.answer("GET", BALANCE, null));
        waitUntilExpired(shortLived.access(), keys, kc.issuer());
        row(out, w, "alice, after its 4 second lifetime", door.answer("GET", BALANCE, shortLived.access()));
        row(out, w, "alice, made for reports-api", door.answer("GET", BALANCE, otherAudience.access()));
        row(out, w, "bob, roles edited to admin", door.answer("POST", "/admin/freeze", edited));
        row(out, w, "alice, a signature character changed", door.answer("GET", BALANCE, Forge.withFlippedSignature(alice.access())));
        row(out, w, "mallory, own key, claims admin", door.answer("POST", "/admin/freeze", forged));
        row(out, w, "mallory, algorithm none", door.answer("POST", "/admin/freeze", Forge.token(Map.of("alg", "none", "typ", "JWT"), admin, null)));

        int before = behind.executed();
        int viaProxy = 0;
        int direct = 0;
        for (String[] q : NO_IDENTITY) {
            if (door.call(q[0], q[1], null) == 200) {
                viaProxy++;
            }
            if (send(behind.uri(), q[0], q[1], null) == 200) {
                direct++;
            }
        }
        head(out, "THE SAME " + NO_IDENTITY.size() + " REQUESTS WITH NO IDENTITY, NOW:");
        row(out, 24, "through the proxy", viaProxy + " of " + NO_IDENTITY.size() + " succeed (before: " + NO_IDENTITY.size() + ")");
        row(out, 24, "straight to the ledger", direct + " of " + NO_IDENTITY.size() + " (it wants the proxy's mark)");
        row(out, 24, "ledger ran", (behind.executed() - before) + " more times");
    }

    private static boolean jdk(String token, RSAPublicKey key) throws Exception {
        int dot = token.lastIndexOf('.');
        Signature v = Signature.getInstance("SHA256withRSA");
        v.initVerify(key);
        v.update(token.substring(0, dot).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        try {
            return v.verify(Base64.getUrlDecoder().decode(token.substring(dot + 1)));
        } catch (java.security.SignatureException e) {
            return false;
        }
    }

    private static void waitUntilExpired(String token, Map<String, PublicKey> keys, String issuer) throws Exception {
        while (Jwt.verify(token, keys, issuer, AUDIENCE, Instant.now()).valid()) {
            Thread.sleep(250);
        }
    }

    // ---- THREE. may they

    private static void authorization(StringBuilder out, Keycloak kc, Opa opa, Proxy proxy, Door door, Ledger behind, List<String> tokens) throws Exception {
        act(out, "THREE", "May they: OPA decides.");
        Keycloak.Tokens alice = kc.login("ledger-managed", "alice");
        Keycloak.Tokens bob = kc.login("ledger-managed", "bob");
        Keycloak.Tokens carol = kc.login("ledger-managed", "carol");
        Keycloak.Tokens unmanaged = kc.login("ledger-unmanaged", "alice");
        for (Keycloak.Tokens t : List.of(alice, bob, carol, unmanaged)) {
            tokens.add(t.access());
            tokens.add(t.refresh());
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("user", "alice");
        input.put("roles", List.of("teller"));
        input.put("device_compliant", true);
        input.put("method", "POST");
        input.put("path", "/transfers");
        input.put("hour", 10);
        head(out, "THE INPUT THE PROXY SENDS OPA (alice, POST /transfers):");
        line(out, "    user alice, roles [teller], device_compliant true");
        line(out, "    method POST, path /transfers, hour 10 (then 23)");
        JsonNode day = opa.explain(input);
        Map<String, Object> late = new HashMap<>(input);
        late.put("hour", 23);
        JsonNode night = opa.explain(late);
        head(out, "HOW THE DECISION IS MADE (each rule's value, from OPA):");
        row(out, 16, "rule", String.format("%-16s%s", "at 10:00", "at 23:00"));
        for (String rule : List.of("resource", "role_ok", "device_ok", "business_hours", "reasons", "allow")) {
            row(out, 16, rule, String.format("%-16s%s", value(day.get(rule)), value(night.get(rule))));
        }
        head(out, "THE GRANTS (any other request is denied):");
        Map<String, TreeSet<String>> grants = new LinkedHashMap<>();
        for (String role : List.of("teller", "auditor", "admin")) {
            grants.put(role, new TreeSet<>());
        }
        day.path("grants").forEach(g -> grants.get(g.path("role").asText()).add(g.path("method").asText() + " " + g.path("resource").asText()));
        grants.forEach((role, ops) -> row(out, 9, role, String.join(", ", ops)));

        head(out, "THE DECISIONS AT THE PROXY (a valid token each time):");
        int from = door.sent().size();
        int ran = behind.executed();
        int w = 38;
        row(out, w, "alice teller   GET  balance", door.answer("GET", BALANCE, alice.access()));
        row(out, w, "alice teller   POST transfers 10:00", door.answer("POST", "/transfers", alice.access()));
        proxy.setHour(() -> 23);
        row(out, w, "alice teller   POST transfers 23:00", door.answer("POST", "/transfers", alice.access()));
        proxy.setHour(() -> 10);
        row(out, w, "bob auditor    GET  audit", door.answer("GET", "/audit/entries", bob.access()));
        row(out, w, "bob auditor    POST transfers", door.answer("POST", "/transfers", bob.access()));
        row(out, w, "alice teller   GET  audit", door.answer("GET", "/audit/entries", alice.access()));
        row(out, w, "carol admin    POST freeze", door.answer("POST", "/admin/freeze", carol.access()));
        row(out, w, "carol admin    POST transfers", door.answer("POST", "/transfers", carol.access()));
        row(out, w, "alice teller   GET  /nope", door.answer("GET", "/nope", alice.access()));
        row(out, w, "alice, managed device, GET balance", door.answer("GET", BALANCE, alice.access()));
        row(out, w, "alice, unmanaged device, GET balance", door.answer("GET", BALANCE, unmanaged.access()));
        List<Sent> mine = door.sent().subList(from, door.sent().size());
        long allowed = mine.stream().filter(x -> x.status() == 200).count();
        head(out, "THE RESULT:");
        row(out, 22, "valid tokens sent", mine.size() + ": 200 for " + allowed + ", 403 for " + (mine.size() - allowed));
        row(out, 22, "ledger ran", (behind.executed() - ran) + " times: only for the allowed ones");
    }

    /** A rule's value as OPA gave it; a rule that is not true for this input is missing ("undefined"). */
    private static String value(JsonNode n) {
        if (n == null) {
            return "undefined";
        }
        if (n.isArray()) {
            List<String> items = new ArrayList<>();
            n.forEach(i -> items.add(i.asText()));
            return "[" + String.join(", ", items) + "]";
        }
        return n.asText();
    }

    // ---- FOUR. a token lives until it expires, and the parts can fail

    private static void revocation(StringBuilder out, Keycloak kc, Map<String, PublicKey> keys, Opa opa, Proxy proxy, Door door, List<String> tokens)
            throws Exception {
        act(out, "FOUR", "A token outlives its session.");
        int w = 38;
        Keycloak.Tokens standard = kc.login("ledger-managed", "alice");
        tokens.add(standard.access());
        tokens.add(standard.refresh());
        head(out, "THE TOKEN (alice, a lifetime of 300 seconds):");
        row(out, w, "before the logout", door.answer("GET", BALANCE, standard.access()));
        kc.logout("ledger-managed", standard);
        head(out, "AFTER THE LOGOUT AT KEYCLOAK:");
        row(out, w, "the same token, no session check", door.answer("GET", BALANCE, standard.access()));
        proxy.setSessionCheck(kc::sessionActive);
        int checks = kc.sessionChecks();
        Keycloak.Tokens fresh = kc.login("ledger-managed", "alice");
        Keycloak.Tokens bob = kc.login("ledger-managed", "bob");
        for (Keycloak.Tokens t : List.of(fresh, bob)) {
            tokens.add(t.access());
            tokens.add(t.refresh());
        }
        row(out, w, "the same token, with a session check", door.answer("GET", BALANCE, standard.access()));
        row(out, w, "a new login, with the check", door.answer("GET", BALANCE, fresh.access()));
        row(out, w, "bob, GET audit, with the check", door.answer("GET", "/audit/entries", bob.access()));
        row(out, w, "calls to Keycloak for those 3", String.valueOf(kc.sessionChecks() - checks));
        proxy.setSessionCheck(null);

        Keycloak.Tokens shortLived = kc.login("ledger-short", "alice");
        tokens.add(shortLived.access());
        tokens.add(shortLived.refresh());
        head(out, "A SHORT LIFETIME INSTEAD (4 seconds, no session check):");
        row(out, w, "before the logout", door.answer("GET", BALANCE, shortLived.access()));
        kc.logout("ledger-short", shortLived);
        row(out, w, "after the logout, at once", door.answer("GET", BALANCE, shortLived.access()));
        waitUntilExpired(shortLived.access(), keys, kc.issuer());
        row(out, w, "after its lifetime", door.answer("GET", BALANCE, shortLived.access()));
        row(out, w, "the window after a logout", "4 s at most, not 300");

        Keycloak.Tokens alice = kc.login("ledger-managed", "alice");
        tokens.add(alice.access());
        tokens.add(alice.refresh());
        head(out, "WHEN A PART IS DOWN (alice, a valid token):");
        kc.stop();
        row(out, w, "Keycloak stopped, no session check", door.answer("GET", BALANCE, alice.access()));
        proxy.setSessionCheck(kc::sessionActive);
        row(out, w, "Keycloak stopped, session check on", door.answer("GET", BALANCE, alice.access()));
        proxy.setSessionCheck(null);
        opa.close();
        row(out, w, "OPA stopped", door.answer("GET", BALANCE, alice.access()));
    }

    // ---- FIVE. the audit

    private static void audit(StringBuilder out, AuditLog audit, List<Sent> sent, List<String> tokens) throws IOException {
        act(out, "FIVE", "One record per request.");
        List<AuditLog.Entry> entries = audit.entries();
        AuditLog.Entry late = entries.stream().filter(e -> e.reason().equals("outside business hours")).findFirst().orElseThrow();
        head(out, "ONE RECORD (a line of JSON in build/lab/audit.log):");
        line(out, "    seq " + late.seq() + ", decision " + late.decision() + ", status " + late.status());
        line(out, "    reason " + late.reason() + ", user " + late.user());
        line(out, "    method " + late.method() + ", path " + late.path() + ", ts (the time)");
        int matching = 0;
        for (int i = 0; i < Math.min(entries.size(), sent.size()); i++) {
            AuditLog.Entry e = entries.get(i);
            Sent s = sent.get(i);
            if (e.seq() == i + 1 && e.method().equals(s.method()) && e.path().equals(s.path()) && e.status() == s.status()) {
                matching++;
            }
        }
        head(out, "THE REQUESTS AGAINST THE RECORDS:");
        row(out, 28, "requests sent to the proxy", String.valueOf(sent.size()));
        row(out, 28, "records written", String.valueOf(entries.size()));
        row(out, 28, "same order, same status", matching + " of " + sent.size());
        long allow = entries.stream().filter(e -> e.decision().equals("allow")).count();
        Map<String, Integer> reasons = new TreeMap<>();
        for (AuditLog.Entry e : entries) {
            reasons.merge(e.reason(), 1, Integer::sum);
        }
        head(out, "THE REASONS (allow " + allow + ", deny " + (entries.size() - allow) + "):");
        List<String> cells = new ArrayList<>();
        reasons.forEach((reason, n) -> cells.add(String.format("%-22s%3d", reason, n)));
        int half = (cells.size() + 1) / 2;
        for (int i = 0; i < half; i++) {
            line(out, "    " + cells.get(i) + (i + half < cells.size() ? "    " + cells.get(i + half) : ""));
        }
        String raw = audit.raw();
        long leaks = tokens.stream().filter(raw::contains).count() + (raw.contains("eyJ") ? 1 : 0);
        long named = entries.stream().filter(e -> !e.user().equals("-")).count();
        head(out, "WHAT THE LOG DOES NOT HOLD:");
        row(out, 28, "tokens (access or refresh)", leaks + " of " + tokens.size());
        row(out, 28, "a name from a forged token", "none: " + named + " of " + entries.size() + " records name a user");
    }

    // ---- output helpers

    private static void act(StringBuilder out, String number, String title) {
        line(out, number + ". " + title);
    }

    private static void head(StringBuilder out, String text) {
        line(out, "  " + text);
    }

    private static void row(StringBuilder out, int width, String label, String value) {
        line(out, ("    " + String.format("%-" + width + "s", label) + value).stripTrailing());
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }
}
