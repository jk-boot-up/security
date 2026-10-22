package com.jk.explore.security.bffspring;

import com.jk.explore.security.bffspring.api.Ledger;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
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
        return r.status() == 200 ? "200" : r.status() + " (" + error(r.challenge()) + ")";
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
        line(out, "  the banking app signs Ana in and keeps its access token and refresh token in localStorage");
        line(out, "  the app reads Ana's balance: " + answer(spa.bearer("GET", "/accounts", localStorage.get("access_token"))));
        Map<String, String> taken = new LinkedHashMap<>(localStorage);
        line(out, "  a third-party script on the page reads what scripts can read: " + taken.size() + " tokens taken");
        line(out, "  it sends them to its own server, and Ana closes the page");
        line(out, "  from elsewhere, the stolen access token reads the balance: " + answer(spa.bearer("GET", "/accounts", taken.get("access_token"))));
        api.bean(com.jk.explore.security.bffspring.OffsetClock.class).advance(Duration.ofSeconds(331));
        line(out, "  five minutes later: " + answer(spa.bearer("GET", "/accounts", taken.get("access_token"))) + ", the token has expired");
        api.bean(com.jk.explore.security.bffspring.OffsetClock.class).advance(Duration.ofSeconds(-331));
        SpaClient.Tokens renewed = spa.refresh("bank-spa", taken.get("refresh_token"), null);
        line(out, "  the stolen refresh token gets a new access token from Keycloak: " + (renewed.status() == 200) + ", which reads the balance: " + answer(spa.bearer("GET", "/accounts", renewed.string("access_token")))
                + "  <- ONE PAGE VISIT, AS LONG AS THE SESSION LIVES");
        line(out, "  anything a script can read, a script can send away");
    }

    private static void act2Bff(StringBuilder out, KeycloakLab keycloak, Running bff, Running api) throws Exception {
        line(out, "TWO. A backend for the frontend keeps the tokens on the server.");
        Shopper ana = new Shopper();
        ana.navigate(bff.url() + "/oauth2/authorization/keycloak", "ana", "demo-password");
        String header = ana.setCookieHeader(bff.url(), "__Host-bff");
        line(out, "  the BFF signs Ana in on the server; the browser gets a session cookie: " + header.replaceAll("^(__Host-bff)=[^;]+", "$1") + "  (and an XSRF-TOKEN cookie its script copies into a header)");
        boolean rules = header.startsWith("__Host-bff=") && header.contains("Path=/") && header.contains("Secure") && header.contains("HttpOnly") && header.contains("SameSite=Strict") && !header.contains("Domain=");
        OAuth2AuthorizedClient held = bff.bean(OAuth2AuthorizedClientService.class).loadAuthorizedClient("keycloak", SUBJECT);
        int tokensHeld = (held.getAccessToken() != null ? 1 : 0) + (held.getRefreshToken() != null ? 1 : 0);
        line(out, "  the cookie keeps the __Host- rules: " + rules + ", and the BFF holds " + tokensHeld + " tokens (an access token and a refresh token) that the browser never sees");
        Shopper.Reply balance = ana.get(bff.url() + "/api/accounts");
        line(out, "  the app reads the balance through the BFF: " + balance.status());
        Map<String, String> readable = ana.documentCookie(bff.url());
        line(out, "  the same third-party script reads what scripts can read, document.cookie: " + readable.keySet() + ": 0 tokens taken, and the session cookie is not readable: " + !readable.containsKey("__Host-bff"));
        line(out, "  while the page is open, the script can still ask the BFF for the balance: " + ana.get(bff.url() + "/api/accounts").status() + "  <- A SCRIPT ON THE PAGE CAN STILL ACT AS ANA");
        line(out, "  once the page is closed: the script is no longer running, and what it took works nowhere else: " + new Shopper().get(bff.url() + "/api/accounts").status());
        String before = held.getAccessToken().getTokenValue();
        bff.bean(com.jk.explore.security.bffspring.OffsetClock.class).advance(Duration.ofMinutes(6));
        Shopper.Reply later = ana.get(bff.url() + "/api/accounts");
        String after = bff.bean(OAuth2AuthorizedClientService.class).<OAuth2AuthorizedClient>loadAuthorizedClient("keycloak", SUBJECT).getAccessToken().getTokenValue();
        bff.bean(com.jk.explore.security.bffspring.OffsetClock.class).advance(Duration.ofMinutes(-6));
        line(out, "  six minutes later Ana opens the app again; the BFF refreshes the token on the server: " + later.status() + ", with a new access token: " + !before.equals(after));
    }

    private static void act3OtherSites(StringBuilder out, Running bff, Running api) throws Exception {
        line(out, "THREE. Requests started by another site.");
        Shopper ana = new Shopper();
        ana.navigate(bff.url() + "/oauth2/authorization/keycloak", "ana", "demo-password");
        Shopper.Reply linkOnEvil = ana.request("GET", bff.url() + "/api/accounts", Map.of("Origin", "https://evil.example"), null, true);
        Shopper.Reply formOnEvil = ana.request("POST", bff.url() + "/api/transfers", Map.of("Origin", "https://evil.example"), "", true);
        line(out, "  a link or a hidden form on evil.example reaches the BFF: SameSite=Strict keeps the cookie off both: the link gets " + linkOnEvil.status() + " (not signed in), the form's transfer " + formOnEvil.status() + " (no session, no CSRF token)");
        Shopper.Reply oldBrowser = ana.request("POST", bff.url() + "/api/transfers", Map.of("Origin", "https://evil.example"), "", false);
        line(out, "  in an older browser that ignores SameSite, the cookie goes, but the bank's request header does not: " + oldBrowser.status() + " (a change needs the bank's own request header)");
        Shopper.Reply preflight = ana.request("OPTIONS", bff.url() + "/api/transfers", Map.of("Origin", "https://evil.example", "Access-Control-Request-Method", "POST", "Access-Control-Request-Headers", "x-xsrf-token"), null, true);
        line(out, "  a script on evil.example tries to add the header itself: the browser asks first, and the BFF names no allowed origin: " + (preflight.headers().get("access-control-allow-origin") == null) + ", so the request is never sent");
        Shopper.Reply own = ana.request("POST", bff.url() + "/api/transfers", Map.of("X-XSRF-TOKEN", ana.documentCookie(bff.url()).get("XSRF-TOKEN")), "", false);
        line(out, "  the bank's own page sends the transfer with the header: " + own.status());
        line(out, "  transfers made: " + api.bean(Ledger.class).transfers().size() + ", and only the one the bank's own page asked for");
    }

    private static void act4Dpop(StringBuilder out, SpaClient spa) throws Exception {
        line(out, "FOUR. Tokens bound to a key, with DPoP.");
        DpopKey app = new DpopKey();
        SpaClient.Tokens tokens = spa.signIn("bank-spa-dpop", app);
        String access = tokens.string("access_token");
        Object cnf = SpaClient.claims(access).get("cnf");
        line(out, "  the app holds a key that scripts cannot read, and the token carries the key's thumbprint in cnf.jkt: " + String.valueOf(cnf).contains(app.thumbprint()) + ", token type " + tokens.string("token_type"));
        line(out, "  the app sends the token with a fresh proof signed by its key: " + answer(spa.dpop("GET", "/accounts", access, app.proof("GET", spa.urlOf("/accounts"), access))));
        line(out, "  a script takes the token from localStorage and sends it as a plain bearer token: " + answer(spa.bearer("GET", "/accounts", access)) + " (this token is bound to a key, so a proof is required)");
        DpopKey thief = new DpopKey();
        line(out, "  with a proof from a key of its own: " + answer(spa.dpop("GET", "/accounts", access, thief.proof("GET", spa.urlOf("/accounts"), access))) + " (the proof's key is not the key the token is bound to)");
        String proof = app.proof("GET", spa.urlOf("/accounts"), access);
        spa.dpop("GET", "/accounts", access, proof);
        line(out, "  the app's own request copied and sent again: " + answer(spa.dpop("GET", "/accounts", access, proof)) + " (the proof was already used)");
        line(out, "  the app's proof for reading the balance, used for a transfer: " + answer(spa.dpop("POST", "/transfers", access, app.proof("GET", spa.urlOf("/accounts"), access))) + " (the proof was made for another request)");
        line(out, "  a proof made two minutes ago, with the token still valid: " + answer(spa.dpop("GET", "/accounts", access, app.proof("GET", spa.urlOf("/accounts"), access, Instant.now().minusSeconds(120), UUID.randomUUID().toString()))) + " (the proof is too old)");
        line(out, "  a proof that does not name the token it goes with: " + answer(spa.dpop("GET", "/accounts", access, app.proof("GET", spa.urlOf("/accounts"), null))) + " (the proof does not hash the token)");
        SpaClient.Tokens stolen = spa.refresh("bank-spa-dpop", tokens.string("refresh_token"), thief);
        line(out, "  the stolen refresh token with the thief's key: refused (" + stolen.string("error_description") + ")");
        line(out, "  the same refresh token with no key at all: " + (spa.refresh("bank-spa-dpop", tokens.string("refresh_token"), null).status() == 200 ? "new tokens" : "refused"));
        line(out, "  the same refresh token presented by the app, with its own key: " + (spa.refresh("bank-spa-dpop", tokens.string("refresh_token"), app).status() == 200 ? "new tokens" : "refused"));
        line(out, "  a stolen token is useless without the key, and the key never leaves the browser");
    }

    private static void act5Bill(StringBuilder out, KeycloakLab keycloak, Running bff, Running api, SpaClient spa) throws Exception {
        line(out, "FIVE. The bill.");
        Shopper ana = new Shopper();
        ana.navigate(bff.url() + "/oauth2/authorization/keycloak", "ana", "demo-password");
        try (Running second = Running.bff(keycloak, Running.freePort(), api.url())) {
            Shopper moved = new Shopper();
            Shopper.Reply onSecond = moved.request("GET", second.url() + "/api/accounts", Map.of("Cookie", "__Host-bff=" + ana.cookieValue(bff.url(), "__Host-bff")), null, false);
            line(out, "  two BFF servers, each with its own session store: the second one answers Ana's cookie with " + onSecond.status() + " (not signed in), so the sessions need a shared store");
        }
        line(out, "  every API call now passes through one more server, which must be run, scaled and kept up to date");
        DpopKey first = new DpopKey();
        SpaClient.Tokens tokens = spa.signIn("bank-spa-dpop", first);
        DpopKey cleared = new DpopKey();
        line(out, "  clearing the browser's storage loses the DPoP key; the old token with a new key: " + answer(spa.dpop("GET", "/accounts", tokens.string("access_token"), cleared.proof("GET", spa.urlOf("/accounts"), tokens.string("access_token"))))
                + ", so Ana signs in again");
        line(out, "  a script on the open page can still ask the key for a proof and read the balance: " + answer(spa.dpop("GET", "/accounts", tokens.string("access_token"), first.proof("GET", spa.urlOf("/accounts"), tokens.string("access_token"))))
                + "  <- DPOP STOPS THEFT, NOT USE WHILE THE PAGE IS OPEN");
        line(out, "  so keep scripts off the page (a strict Content-Security-Policy), and keep tokens out of it (the BFF)");
    }
}
