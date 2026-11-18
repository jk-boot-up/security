package com.jk.explore.security.bffspring;

import com.jk.explore.security.bffspring.api.Ledger;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

/** Runs the bank's story of tokens and the browser in five numbered acts against a real Keycloak in Docker, a real backend for the frontend and a real accounts API. */
public class BffSpringDemo {

    static final String SUBJECT = "a0a0a0a0-0000-4000-8000-000000000001";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Ports, tokens and keys are never printed. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        int bffPort = Running.freePort();
        try (KeycloakLab keycloak = KeycloakLab.start(bffPort); Running api = Running.api(keycloak); Running bff = Running.bff(keycloak, bffPort, api.url())) {
            SpaClient spa = new SpaClient(keycloak.issuer(), api.url());
            act1TokensInTheBrowser(out, spa, api);
            act2Bff(out, keycloak, bff, api);
            act3OtherSites(out, bff, api);
            act4Dpop(out, spa);
            act5Bill(out, keycloak, bff, api, spa);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static String error(String challenge) {
        if (challenge == null) {
            return "no challenge";
        }
        java.util.regex.Matcher m = Pattern.compile("error=\"([^\"]+)\"").matcher(challenge);
        return m.find() ? m.group(1) : "no error";
    }

    private static String answer(SpaClient.ApiReply r) {
        return r.status() == 200 ? "200 " + balance(r.body()) : r.status() + " (" + error(r.challenge()) + ")";
    }

    /** The balance in an accounts reply, or nothing when the reply has none. */
    static String balance(String body) {
        java.util.regex.Matcher m = Pattern.compile("\"balanceCents\":(\\d+)").matcher(body == null ? "" : body);
        return m.find() ? "balance " + m.group(1) + " cents" : "";
    }

    private static String reply(Shopper.Reply r) {
        return (r.status() + " " + balance(r.body())).strip();
    }

    /** How long a token lives: its exp minus its iat, both read from its middle part. */
    private static long lifetime(String jwt) {
        Map<String, Object> claims = SpaClient.claims(jwt);
        return ((Number) claims.get("exp")).longValue() - ((Number) claims.get("iat")).longValue();
    }

    /** What a script on the page can read: the page's own storage, here a plain map, standing for localStorage. */
    private static Map<String, String> localStorageOf(SpaClient.Tokens tokens) {
        Map<String, String> storage = new LinkedHashMap<>();
        storage.put("access_token", tokens.string("access_token"));
        storage.put("refresh_token", tokens.string("refresh_token"));
        return storage;
    }

    private static void act1TokensInTheBrowser(StringBuilder out, SpaClient spa, Running api) throws Exception {
        line(out, "ONE. Tokens kept in the browser.");
        SpaClient.Tokens tokens = spa.signIn("bank-spa", null);
        Map<String, String> localStorage = localStorageOf(tokens);
        line(out, "  THE SIGN-IN (Keycloak, client bank-spa; both go to localStorage):");
        line(out, "    access_token   <a JWT signed by Keycloak>");
        line(out, "    refresh_token  <a JWT signed by Keycloak>");
        Map<String, Object> claims = SpaClient.claims(localStorage.get("access_token"));
        line(out, "  THE ACCESS TOKEN'S CLAIMS (its middle part, decoded):");
        line(out, "    sub    " + claims.get("sub"));
        line(out, "    azp    " + claims.get("azp") + "  (the client it was issued to)");
        line(out, "    scope  " + String.join(" ", new java.util.TreeSet<>(List.of(String.valueOf(claims.get("scope")).split(" ")))));
        line(out, "    exp    = iat + " + lifetime(localStorage.get("access_token")) + " seconds (five minutes)");
        line(out, "  THE REFRESH TOKEN'S CLAIMS:");
        line(out, "    exp    = iat + " + lifetime(localStorage.get("refresh_token")) + " seconds (the session's idle time)");
        line(out, "  THE APP'S OWN REQUEST (Authorization: Bearer <access_token>):");
        line(out, "    GET /accounts -> " + answer(spa.bearer("GET", "/accounts", localStorage.get("access_token"))));
        Map<String, String> taken = new LinkedHashMap<>(localStorage);
        line(out, "  THE SCRIPT ON THE PAGE READS localStorage:");
        line(out, "    " + String.join(", ", taken.keySet()) + " -> " + taken.size() + " tokens sent to its server");
        line(out, "  THE STOLEN TOKENS, USED FROM ELSEWHERE (Ana closed the page):");
        line(out, "    access token, at once        -> " + answer(spa.bearer("GET", "/accounts", taken.get("access_token"))));
        api.bean(OffsetClock.class).advance(Duration.ofSeconds(331));
        line(out, "    access token, 331 s later    -> " + answer(spa.bearer("GET", "/accounts", taken.get("access_token"))));
        api.bean(OffsetClock.class).advance(Duration.ofSeconds(-331));
        SpaClient.Tokens renewed = spa.refresh("bank-spa", taken.get("refresh_token"), null);
        line(out, "    refresh token, at Keycloak   -> " + (renewed.status() == 200 ? "a new access token" : "refused"));
        line(out, "    the new access token         -> " + answer(spa.bearer("GET", "/accounts", renewed.string("access_token"))));
        line(out, "  ONE PAGE VISIT, AS LONG AS THE SESSION LIVES:");
        line(out, "    anything a script can read, a script can send away");
    }

    private static void act2Bff(StringBuilder out, KeycloakLab keycloak, Running bff, Running api) throws Exception {
        line(out, "TWO. A backend for the frontend keeps the tokens on the server.");
        Shopper ana = new Shopper();
        ana.navigate(bff.url() + "/oauth2/authorization/keycloak", "ana", "demo-password");
        String header = ana.setCookieHeader(bff.url(), "__Host-bff");
        String xsrf = ana.setCookieHeader(bff.url(), "XSRF-TOKEN");
        line(out, "  THE SIGN-IN (Spring's oauth2Login on the BFF, client bank-bff):");
        line(out, "    Set-Cookie: __Host-bff=<session id>");
        line(out, "      " + header.substring(header.indexOf(';') + 1).strip());
        line(out, "    Set-Cookie: XSRF-TOKEN=<random>; " + xsrf.substring(xsrf.indexOf(';') + 1).strip());
        boolean rules = header.startsWith("__Host-bff=") && header.contains("Path=/") && header.contains("Secure") && !header.contains("Domain=");
        line(out, "  THE COOKIE'S RULES (__Host-bff):");
        line(out, "    Secure           " + (header.contains("Secure") ? "sent over HTTPS only" : "also sent over HTTP"));
        line(out, "    HttpOnly         " + (header.contains("HttpOnly") ? "scripts on the page cannot read it" : "scripts can read it"));
        line(out, "    SameSite=Strict  " + (header.contains("SameSite=Strict") ? "not sent when another site starts it" : "missing"));
        line(out, "    __Host- prefix   Secure, Path=/, no Domain: " + rules);
        OAuth2AuthorizedClient held = bff.bean(OAuth2AuthorizedClientService.class).loadAuthorizedClient("keycloak", SUBJECT);
        int tokensHeld = (held.getAccessToken() != null ? 1 : 0) + (held.getRefreshToken() != null ? 1 : 0);
        Map<String, String> readable = ana.documentCookie(bff.url());
        line(out, "  WHERE THE TOKENS ARE:");
        line(out, "    the BFF's OAuth2AuthorizedClientService  " + tokensHeld + " tokens");
        line(out, "    the browser                              0 tokens");
        line(out, "  THE APP'S OWN REQUEST (only the cookie goes with it):");
        line(out, "    GET /api/accounts -> " + reply(ana.get(bff.url() + "/api/accounts")));
        line(out, "  THE SAME SCRIPT ON THE PAGE READS document.cookie:");
        line(out, "    " + readable.keySet() + " -> 0 tokens; __Host-bff is not readable: " + !readable.containsKey("__Host-bff"));
        line(out, "  WHAT THE SCRIPT CAN STILL DO:");
        line(out, "    page open    GET /api/accounts -> " + reply(ana.get(bff.url() + "/api/accounts")));
        line(out, "      <- A SCRIPT ON THE PAGE CAN STILL ACT AS ANA");
        line(out, "    page closed  what it took, from elsewhere -> " + new Shopper().get(bff.url() + "/api/accounts").status());
        String before = held.getAccessToken().getTokenValue();
        bff.bean(OffsetClock.class).advance(Duration.ofMinutes(6));
        Shopper.Reply later = ana.get(bff.url() + "/api/accounts");
        String after = bff.bean(OAuth2AuthorizedClientService.class).<OAuth2AuthorizedClient>loadAuthorizedClient("keycloak", SUBJECT).getAccessToken().getTokenValue();
        bff.bean(OffsetClock.class).advance(Duration.ofMinutes(-6));
        line(out, "  SIX MINUTES LATER (the access token has expired):");
        line(out, "    the browser sends the cookie -> " + reply(later));
        line(out, "    the BFF refreshed it on the server, a new token: " + !before.equals(after));
    }

    private static void act3OtherSites(StringBuilder out, Running bff, Running api) throws Exception {
        line(out, "THREE. Requests started by another site.");
        Shopper ana = new Shopper();
        ana.navigate(bff.url() + "/oauth2/authorization/keycloak", "ana", "demo-password");
        Shopper.Reply linkOnEvil = ana.request("GET", bff.url() + "/api/accounts", Map.of("Origin", "https://evil.example"), null, true);
        Shopper.Reply formOnEvil = ana.request("POST", bff.url() + "/api/transfers", Map.of("Origin", "https://evil.example"), "", true);
        line(out, "  THE REQUESTS (started on evil.example, Ana signed in):");
        line(out, "    a link   GET /api/accounts");
        line(out, "    a form   POST /api/transfers, no X-XSRF-TOKEN header");
        line(out, "  THE RESPONSES (a modern browser: SameSite=Strict, no cookie):");
        line(out, "    the link -> " + linkOnEvil.status() + " (not signed in)");
        line(out, "    the form -> " + formOnEvil.status() + " (no session, no CSRF token)");
        Shopper.Reply oldBrowser = ana.request("POST", bff.url() + "/api/transfers", Map.of("Origin", "https://evil.example"), "", false);
        line(out, "  THE SAME FORM IN AN OLDER BROWSER (it ignores SameSite):");
        line(out, "    the cookie goes, the header does not -> " + oldBrowser.status() + " (no CSRF token)");
        Shopper.Reply preflight = ana.request("OPTIONS", bff.url() + "/api/transfers", Map.of("Origin", "https://evil.example", "Access-Control-Request-Method", "POST", "Access-Control-Request-Headers", "x-xsrf-token"), null, true);
        line(out, "  A SCRIPT ON evil.example ADDS X-XSRF-TOKEN ITSELF:");
        line(out, "    the browser asks first (a CORS preflight)");
        line(out, "    Access-Control-Allow-Origin in the answer: " + (preflight.headers().get("access-control-allow-origin") != null));
        line(out, "      -> never sent");
        Shopper.Reply own = ana.request("POST", bff.url() + "/api/transfers", Map.of("X-XSRF-TOKEN", ana.documentCookie(bff.url()).get("XSRF-TOKEN")), "", false);
        line(out, "  THE BANK'S OWN PAGE (the cookie, and XSRF-TOKEN in the header):");
        line(out, "      -> " + own.status());
        line(out, "  transfers made: " + api.bean(Ledger.class).transfers().size() + ", the one the bank's own page asked for");
    }

    private static void act4Dpop(StringBuilder out, SpaClient spa) throws Exception {
        line(out, "FOUR. Tokens bound to a key, with DPoP.");
        DpopKey app = DpopKey.seeded("ana's browser key");
        String canonical = app.canonical();
        int x = canonical.indexOf(",\"x\"");
        int y = canonical.indexOf(",\"y\"");
        line(out, "  THE APP'S PUBLIC KEY (P-256; scripts may sign, not read it):");
        line(out, "    " + canonical.substring(0, x + 1));
        line(out, "    " + canonical.substring(x + 1, y + 1));
        line(out, "    " + canonical.substring(y + 1));
        line(out, "  THE THUMBPRINT, WORKED OUT (RFC 7638):");
        line(out, "    the lines above: a to z, no spaces, " + canonical.length() + " characters");
        line(out, "    SHA-256 of it, 32 bytes, in base64url:");
        line(out, "    " + app.thumbprintByHand());
        line(out, "    the same as Nimbus's computeThumbprint(): " + app.thumbprintByHand().equals(app.thumbprint()));
        SpaClient.Tokens tokens = spa.signIn("bank-spa-dpop", app);
        String access = tokens.string("access_token");
        Object jkt = ((Map<?, ?>) SpaClient.claims(access).get("cnf")).get("jkt");
        line(out, "  THE TOKEN FROM KEYCLOAK (client bank-spa-dpop):");
        line(out, "    token_type  " + tokens.string("token_type"));
        line(out, "    cnf.jkt     " + jkt);
        line(out, "                " + (app.thumbprint().equals(jkt) ? "= the thumbprint" : "<- DIFFERENT"));
        line(out, "  THE PROOF FOR ONE REQUEST (a small JWT signed by the key):");
        line(out, "    typ dpop+jwt, alg ES256, jwk <the public key above>");
        line(out, "    jti  <a random UUID>  (used once)");
        line(out, "    htm  GET");
        line(out, "    htu  http://127.0.0.1:<port>/accounts");
        line(out, "    iat  <now>");
        line(out, "    ath  <SHA-256 of the access token, in base64url>");
        line(out, "  THE APP'S OWN REQUEST (Authorization: DPoP <token>, DPoP: <proof>):");
        line(out, "    GET /accounts -> " + answer(spa.dpop("GET", "/accounts", access, app.proof("GET", spa.urlOf("/accounts"), access))));
        DpopKey thief = DpopKey.seeded("someone else's key");
        String proof = app.proof("GET", spa.urlOf("/accounts"), access);
        spa.dpop("GET", "/accounts", access, proof);
        line(out, "  THE ATTACKS (a script took the token from localStorage):");
        line(out, "    the token alone, as a plain bearer token");
        line(out, "      -> " + answer(spa.bearer("GET", "/accounts", access)));
        line(out, "    with a proof from the thief's own key");
        line(out, "      -> " + answer(spa.dpop("GET", "/accounts", access, thief.proof("GET", spa.urlOf("/accounts"), access))));
        line(out, "    the app's own request, copied and sent again");
        line(out, "      -> " + answer(spa.dpop("GET", "/accounts", access, proof)));
        line(out, "    the app's proof for GET /accounts, used for a transfer");
        line(out, "      -> " + answer(spa.dpop("POST", "/transfers", access, app.proof("GET", spa.urlOf("/accounts"), access))));
        line(out, "    a proof made 120 s ago (the token is still valid)");
        line(out, "      -> " + answer(spa.dpop("GET", "/accounts", access, app.proof("GET", spa.urlOf("/accounts"), access, Instant.now().minusSeconds(120), UUID.randomUUID().toString()))));
        line(out, "    a proof with no ath (it does not name the token)");
        line(out, "      -> " + answer(spa.dpop("GET", "/accounts", access, app.proof("GET", spa.urlOf("/accounts"), null))));
        SpaClient.Tokens stolen = spa.refresh("bank-spa-dpop", tokens.string("refresh_token"), thief);
        line(out, "  THE REFRESH TOKEN AT KEYCLOAK, BOUND TO THE SAME KEY:");
        line(out, "    with the thief's key       -> " + (stolen.status() == 200 ? "new tokens" : "refused"));
        line(out, "      (" + stolen.string("error_description") + ")");
        line(out, "    with no key at all         -> " + (spa.refresh("bank-spa-dpop", tokens.string("refresh_token"), null).status() == 200 ? "new tokens" : "refused"));
        line(out, "    the app, with its own key  -> " + (spa.refresh("bank-spa-dpop", tokens.string("refresh_token"), app).status() == 200 ? "new tokens" : "refused"));
        line(out, "  a stolen token is useless without the key,");
        line(out, "  and the key never leaves the browser");
    }

    private static void act5Bill(StringBuilder out, KeycloakLab keycloak, Running bff, Running api, SpaClient spa) throws Exception {
        line(out, "FIVE. The bill.");
        Shopper ana = new Shopper();
        ana.navigate(bff.url() + "/oauth2/authorization/keycloak", "ana", "demo-password");
        line(out, "  TWO BFF SERVERS, EACH WITH ITS OWN SESSION STORE:");
        try (Running second = Running.bff(keycloak, Running.freePort(), api.url())) {
            Shopper moved = new Shopper();
            Shopper.Reply onSecond = moved.request("GET", second.url() + "/api/accounts", Map.of("Cookie", "__Host-bff=" + ana.cookieValue(bff.url(), "__Host-bff")), null, false);
            line(out, "    Ana signs in on the first; her cookie reaches the second");
            line(out, "      -> " + onSecond.status() + " (not signed in)");
        }
        line(out, "    so the sessions need a shared store");
        line(out, "  ONE MORE SERVER:");
        line(out, "    every API call passes through the BFF, which must be run,");
        line(out, "    scaled and kept up to date");
        DpopKey first = DpopKey.seeded("ana's browser key");
        SpaClient.Tokens tokens = spa.signIn("bank-spa-dpop", first);
        DpopKey cleared = DpopKey.seeded("a key made after the storage was cleared");
        String access = tokens.string("access_token");
        line(out, "  THE BROWSER'S STORAGE CLEARED (the DPoP key is lost):");
        line(out, "    the token is bound to    " + first.thumbprint().substring(0, 12) + "...");
        line(out, "    the new key's thumbprint " + cleared.thumbprint().substring(0, 12) + "...");
        line(out, "      -> " + answer(spa.dpop("GET", "/accounts", access, cleared.proof("GET", spa.urlOf("/accounts"), access))));
        line(out, "    so Ana signs in again");
        line(out, "  A SCRIPT ON THE OPEN PAGE ASKS THE KEY FOR A PROOF:");
        line(out, "      -> " + answer(spa.dpop("GET", "/accounts", access, first.proof("GET", spa.urlOf("/accounts"), access))));
        line(out, "      <- DPOP STOPS THEFT, NOT USE WHILE THE PAGE IS OPEN");
        line(out, "  keep scripts off the page (a strict Content-Security-Policy),");
        line(out, "  and keep tokens out of it (the BFF)");
    }
}
