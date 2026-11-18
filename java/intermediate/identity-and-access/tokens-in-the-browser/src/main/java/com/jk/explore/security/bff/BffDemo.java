package com.jk.explore.security.bff;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * A single-page banking app and where its tokens should live, in five acts: tokens in the browser's storage, a backend for the
 * frontend that keeps them on the server, requests from another site, tokens bound to a key with DPoP, and the bill.
 * The clock is simulated and keys come from fixed seeds, so tokens and thumbprints are the same on every run. Session ids, refresh
 * tokens and proof ids are random, so they are printed as placeholders.
 */
public class BffDemo {

    static final long START = 1_790_000_000L;

    public static void main(String[] args) {
        System.out.print(run());
    }

    static KeyPair seeded(String label) {
        return Ed25519.fromSeed(Arrays.copyOf(Bytes.sha256(label.getBytes(StandardCharsets.UTF_8)), 32));
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A response as one short text: the status and the body, with the reason in brackets when refused. */
    private static String answer(Response response) {
        if (response.status() == 0) {
            return response.body();
        }
        return response.status() + (response.status() >= 400 ? " (" + response.body() + ")" : " " + response.body());
    }

    /** The claims of a signed token, decoded from its middle part. */
    static Map<String, Object> claims(String token) {
        try {
            return Json.parseObject(new String(Jws.parse(token).payload(), StandardCharsets.UTF_8));
        } catch (InvalidTokenException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> header(String token) {
        try {
            return Jws.parse(token).header();
        } catch (InvalidTokenException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Request get(String url, String scheme, String token) {
        return Request.of("GET", url).withHeader("Authorization", scheme + " " + token);
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1TokensInStorage(out);
        act2BackendForFrontend(out);
        act3OtherSites(out);
        act4Dpop(out);
        act5Bill(out);
        return out.toString();
    }

    private static void act1TokensInStorage(StringBuilder out) {
        line(out, "ONE. Tokens kept in the browser.");
        SimClock clock = new SimClock(START);
        AuthServer auth = new AuthServer(clock, seeded("bank signing key"));
        BankApi api = new BankApi(clock, auth.publicKey());
        Browser browser = new Browser(Browser.Kind.MODERN, new Bff(auth, api, clock), api);
        AuthServer.Tokens tokens = auth.issue("ana", "accounts.read payments.write", null);
        browser.store("access_token", tokens.accessToken());
        browser.store("refresh_token", tokens.refreshToken());
        String access = browser.stored("access_token");
        line(out, "  THE SIGN-IN (the app writes both tokens to localStorage):");
        line(out, "    access_token   " + access.substring(0, 40) + "...");
        line(out, "    refresh_token  <32 random bytes>, valid "
                + AuthServer.REFRESH_SECONDS / 86_400 + " days");
        Map<String, Object> claims = claims(access);
        line(out, "  THE ACCESS TOKEN'S CLAIMS (its middle part, decoded):");
        line(out, "    sub    " + claims.get("sub"));
        line(out, "    scope  " + claims.get("scope"));
        line(out, "    iat    " + claims.get("iat") + "  (issued at, in seconds)");
        long lifetime = (Long) claims.get("exp") - (Long) claims.get("iat");
        line(out, "    exp    " + claims.get("exp") + "  = iat + " + lifetime + " (five minutes)");
        line(out, "  THE APP'S OWN REQUEST (Authorization: Bearer <access_token>):");
        line(out, "    GET /accounts -> " + answer(browser.send(Browser.BANK_SITE, get(BankApi.ACCOUNTS, "Bearer", access))));
        PageScript script = new PageScript();
        int taken = script.collect(browser);
        browser.closePage();
        line(out, "  THE SCRIPT ON THE PAGE READS localStorage:");
        line(out, "    access_token, refresh_token -> " + taken + " tokens sent to its server");
        String stolenAccess = script.sentHome().get(0);
        String stolenRefresh = script.sentHome().get(1);
        line(out, "  THE STOLEN TOKENS, USED FROM ELSEWHERE (Ana closed the page):");
        line(out, "    access token, at once        -> " + answer(api.handle(get(BankApi.ACCOUNTS, "Bearer", stolenAccess))));
        clock.advance(AuthServer.ACCESS_SECONDS);
        line(out, "    access token, " + AuthServer.ACCESS_SECONDS + " s later    -> "
                + answer(api.handle(get(BankApi.ACCOUNTS, "Bearer", stolenAccess))));
        clock.advance(10L * 24 * 3600);
        Optional<AuthServer.Tokens> renewed = auth.refresh(stolenRefresh, null);
        line(out, "    refresh token, 10 days later -> " + (renewed.isPresent() ? "a new access token" : "refused"));
        line(out, "    the new access token         -> "
                + answer(api.handle(get(BankApi.ACCOUNTS, "Bearer", renewed.orElseThrow().accessToken()))));
        line(out, "  ONE PAGE VISIT, A MONTH OF ACCESS:");
        line(out, "    anything a script can read, a script can send away");
    }

    private static void act2BackendForFrontend(StringBuilder out) {
        line(out, "TWO. A backend for the frontend keeps the tokens on the server.");
        SimClock clock = new SimClock(START);
        AuthServer auth = new AuthServer(clock, seeded("bank signing key"));
        BankApi api = new BankApi(clock, auth.publicKey());
        Bff bff = new Bff(auth, api, clock);
        Browser browser = new Browser(Browser.Kind.MODERN, bff, api);
        browser.receiveCookie(bff.signIn("ana"));
        Cookie cookie = browser.cookie(Bff.COOKIE);
        line(out, "  THE SIGN-IN (the BFF signs Ana in on the server):");
        line(out, "    Set-Cookie: " + cookie.name() + "=<32 random bytes>");
        line(out, "      " + cookie.describe().substring(cookie.name().length() + 2));
        line(out, "  THE COOKIE'S RULES:");
        line(out, "    Secure           " + (cookie.secure() ? "sent over HTTPS only" : "also sent over HTTP"));
        line(out, "    HttpOnly         " + (cookie.httpOnly() ? "scripts on the page cannot read it" : "scripts can read it"));
        line(out, "    SameSite=" + cookie.sameSite() + "  not sent when another site starts it");
        line(out, "    __Host- prefix   Secure, Path=/, no Domain: " + cookie.hostPrefixValid());
        line(out, "  WHERE THE TOKENS ARE:");
        line(out, "    the BFF's session store  " + bff.tokensHeld() + " tokens (access, refresh)");
        line(out, "    the browser              " + browser.readableByScripts().size() + " tokens, 1 cookie");
        line(out, "  THE APP'S OWN REQUEST (only the cookie goes with it):");
        line(out, "    GET /bff/accounts -> " + answer(browser.send(Browser.BANK_SITE, Request.of("GET", Bff.ACCOUNTS))));
        PageScript script = new PageScript();
        int taken = script.collect(browser);
        line(out, "  THE SAME SCRIPT ON THE PAGE READS WHAT SCRIPTS CAN READ:");
        line(out, "    " + taken + " tokens taken; the HttpOnly cookie is not readable");
        line(out, "  WHAT THE SCRIPT CAN STILL DO:");
        line(out, "    page open    GET /bff/accounts -> "
                + answer(script.requestFromPage(browser, Request.of("GET", Bff.ACCOUNTS))));
        line(out, "      <- A SCRIPT ON THE PAGE CAN STILL ACT AS ANA");
        browser.closePage();
        line(out, "    page closed  " + answer(script.requestFromPage(browser, Request.of("GET", Bff.ACCOUNTS))));
        clock.advance(AuthServer.ACCESS_SECONDS + 60);
        Browser later = new Browser(Browser.Kind.MODERN, bff, api);
        later.receiveCookie(Bff.COOKIE + "=" + cookie.value() + "; Path=/; Secure; HttpOnly; SameSite=Strict");
        line(out, "  SIX MINUTES LATER (the access token has expired):");
        line(out, "    the browser sends the cookie -> "
                + answer(later.send(Browser.BANK_SITE, Request.of("GET", Bff.ACCOUNTS))));
        line(out, "    the BFF used the refresh token on the server, out of sight");
    }

    private static void act3OtherSites(StringBuilder out) {
        line(out, "THREE. Requests started by another site.");
        SimClock clock = new SimClock(START);
        AuthServer auth = new AuthServer(clock, seeded("bank signing key"));
        BankApi api = new BankApi(clock, auth.publicKey());
        Bff bff = new Bff(auth, api, clock);
        String setCookie = bff.signIn("ana");
        Browser modern = new Browser(Browser.Kind.MODERN, bff, api);
        modern.receiveCookie(setCookie);
        Browser older = new Browser(Browser.Kind.OLDER, bff, api);
        older.receiveCookie(setCookie);
        Request transfer = Request.of("POST", Bff.TRANSFERS);
        line(out, "  THE REQUEST (a hidden form on evil.example, Ana signed in):");
        line(out, "    POST " + Bff.TRANSFERS);
        line(out, "    no " + Bff.REQUEST_HEADER + " header (a form cannot add one)");
        line(out, "  THE RESPONSES:");
        line(out, "    a modern browser: SameSite=Strict keeps the cookie off");
        line(out, "      -> " + answer(modern.send("evil.example", transfer)));
        line(out, "    an older browser that ignores SameSite: the cookie goes");
        line(out, "      -> " + answer(older.send("evil.example", transfer)));
        line(out, "  A SCRIPT ON evil.example ADDS " + Bff.REQUEST_HEADER + ": 1 ITSELF:");
        line(out, "    the browser asks the BFF first (a CORS preflight): refused");
        line(out, "      -> " + answer(older.send("evil.example", transfer.withHeader(Bff.REQUEST_HEADER, "1"))));
        line(out, "  THE BANK'S OWN PAGE (the cookie and the header):");
        line(out, "      -> " + answer(modern.send(Browser.BANK_SITE, transfer.withHeader(Bff.REQUEST_HEADER, "1"))));
        line(out, "  transfers made: " + api.transfersMade() + ", the one the bank's own page asked for");
    }

    private static void act4Dpop(StringBuilder out) {
        line(out, "FOUR. Tokens bound to a key, with DPoP.");
        SimClock clock = new SimClock(START);
        AuthServer auth = new AuthServer(clock, seeded("bank signing key"));
        BankApi api = new BankApi(clock, auth.publicKey());
        Browser browser = new Browser(Browser.Kind.MODERN, new Bff(auth, api, clock), api);
        DpopKey appKey = new DpopKey(seeded("ana's browser key"));
        browser.holdKey(appKey);
        String canonical = DpopKey.canonical(appKey.jwk());
        line(out, "  THE APP'S PUBLIC KEY (Ed25519; scripts may sign, not read it):");
        line(out, "    " + canonical.substring(0, canonical.indexOf("\"x\"")));
        line(out, "    " + canonical.substring(canonical.indexOf("\"x\"")));
        line(out, "  THE THUMBPRINT, WORKED OUT (RFC 7638):");
        line(out, "    the line above: members a to z, no spaces, " + canonical.length() + " characters");
        line(out, "    SHA-256 of it, 32 bytes, in base64url:");
        line(out, "    " + appKey.thumbprint());
        AuthServer.Tokens tokens = auth.issue("ana", "accounts.read payments.write", appKey.thumbprint());
        browser.store("access_token", tokens.accessToken());
        browser.store("refresh_token", tokens.refreshToken());
        String token = browser.stored("access_token");
        Object jkt = ((Map<?, ?>) claims(token).get("cnf")).get("jkt");
        line(out, "  THE ACCESS TOKEN CARRIES IT (claim cnf.jkt):");
        line(out, "    " + jkt + (appKey.thumbprint().equals(jkt) ? "  = the thumbprint" : "  <- DIFFERENT"));
        String proof = browser.keyForThisPage().proof("GET", BankApi.ACCOUNTS, clock.now(), token);
        Map<String, Object> proofHeader = header(proof);
        Map<String, Object> proofClaims = claims(proof);
        line(out, "  THE PROOF FOR ONE REQUEST (a small JWT signed by the key):");
        line(out, "    typ " + proofHeader.get("typ") + ", alg " + proofHeader.get("alg") + ", jwk <the public key above>");
        line(out, "    jti  <16 random bytes>  (used once)");
        line(out, "    htm  " + proofClaims.get("htm"));
        line(out, "    htu  " + proofClaims.get("htu"));
        line(out, "    iat  " + proofClaims.get("iat"));
        line(out, "    ath  " + proofClaims.get("ath"));
        line(out, "         = SHA-256 of the access token, in base64url");
        Request withProof = get(BankApi.ACCOUNTS, "DPoP", token).withHeader("DPoP", proof);
        line(out, "  THE APP'S OWN REQUEST (Authorization: DPoP <token>, DPoP: <proof>):");
        line(out, "    GET /accounts -> " + answer(browser.send(Browser.BANK_SITE, withProof)));
        PageScript script = new PageScript();
        script.collect(browser);
        String stolen = script.sentHome().get(0);
        DpopKey thiefKey = new DpopKey(seeded("someone else's key"));
        line(out, "  THE ATTACKS (a script took the token from localStorage):");
        line(out, "    the token alone, as a plain bearer token");
        line(out, "      -> " + answer(api.handle(get(BankApi.ACCOUNTS, "Bearer", stolen))));
        line(out, "    with a proof from the thief's own key");
        line(out, "      -> " + answer(api.handle(get(BankApi.ACCOUNTS, "DPoP", stolen)
                .withHeader("DPoP", thiefKey.proof("GET", BankApi.ACCOUNTS, clock.now(), stolen)))));
        line(out, "    the app's own request, copied and sent again");
        line(out, "      -> " + answer(api.handle(withProof)));
        line(out, "    the app's proof for GET /accounts, used for a transfer");
        line(out, "      -> " + answer(api.handle(Request.of("POST", BankApi.TRANSFERS).withHeader("Authorization", "DPoP " + token)
                .withHeader("DPoP", browser.keyForThisPage().proof("GET", BankApi.ACCOUNTS, clock.now(), token)))));
        String early = browser.keyForThisPage().proof("GET", BankApi.ACCOUNTS, clock.now(), token);
        clock.advance(120);
        line(out, "    a proof made " + 120 + " s ago (the token is still valid)");
        line(out, "      -> " + answer(api.handle(get(BankApi.ACCOUNTS, "DPoP", token).withHeader("DPoP", early))));
        DpopVerifier tokenEndpoint = new DpopVerifier(clock);
        String stolenRefresh = script.sentHome().get(1);
        String thiefProofKey = verified(tokenEndpoint, thiefKey.proof("POST", AuthServer.ISSUER + "/token", clock.now(), null));
        line(out, "  THE REFRESH TOKEN, BOUND TO THE SAME KEY:");
        line(out, "    stolen, with the thief's key  -> " + (auth.refresh(stolenRefresh, thiefProofKey).isPresent() ? "new tokens" : "refused"));
        String appProofKey = verified(tokenEndpoint, browser.keyForThisPage().proof("POST", AuthServer.ISSUER + "/token", clock.now(), null));
        line(out, "    the app, with its own key     -> " + (auth.refresh(stolenRefresh, appProofKey).isPresent() ? "new tokens" : "refused"));
        line(out, "  a stolen token is useless without the key,");
        line(out, "  and the key never leaves the browser");
    }

    private static String verified(DpopVerifier verifier, String proof) {
        try {
            return verifier.verify(proof, "POST", AuthServer.ISSUER + "/token", null);
        } catch (InvalidTokenException e) {
            return null;
        }
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        SimClock clock = new SimClock(START);
        AuthServer auth = new AuthServer(clock, seeded("bank signing key"));
        BankApi api = new BankApi(clock, auth.publicKey());
        Bff first = new Bff(auth, api, clock);
        Bff second = new Bff(auth, api, clock);
        Browser browser = new Browser(Browser.Kind.MODERN, first, api);
        browser.receiveCookie(first.signIn("ana"));
        Browser sameCookie = new Browser(Browser.Kind.MODERN, second, api);
        sameCookie.receiveCookie(Bff.COOKIE + "=" + browser.cookie(Bff.COOKIE).value() + "; Path=/; Secure; HttpOnly; SameSite=Strict");
        line(out, "  TWO BFF SERVERS, EACH WITH ITS OWN SESSION STORE:");
        line(out, "    Ana signs in on the first; her cookie reaches the second");
        line(out, "      -> " + answer(sameCookie.send(Browser.BANK_SITE, Request.of("GET", Bff.ACCOUNTS))));
        line(out, "    so the sessions need a shared store");
        line(out, "  ONE MORE SERVER:");
        line(out, "    every API call passes through the BFF, which must be run,");
        line(out, "    scaled and kept up to date");
        DpopKey oldKey = new DpopKey(seeded("ana's browser key"));
        AuthServer.Tokens bound = auth.issue("ana", "accounts.read", oldKey.thumbprint());
        DpopKey newKey = new DpopKey(seeded("a key made after the storage was cleared"));
        line(out, "  THE BROWSER'S STORAGE CLEARED (the DPoP key is lost):");
        line(out, "    the token is bound to    " + oldKey.thumbprint().substring(0, 12) + "...");
        line(out, "    the new key's thumbprint " + newKey.thumbprint().substring(0, 12) + "...");
        line(out, "      -> " + answer(api.handle(get(BankApi.ACCOUNTS, "DPoP", bound.accessToken())
                .withHeader("DPoP", newKey.proof("GET", BankApi.ACCOUNTS, clock.now(), bound.accessToken())))));
        line(out, "    so Ana signs in again");
        Browser page = new Browser(Browser.Kind.MODERN, first, api);
        page.holdKey(oldKey);
        page.store("access_token", bound.accessToken());
        PageScript script = new PageScript();
        String proofFromScript = page.keyForThisPage().proof("GET", BankApi.ACCOUNTS, clock.now(), page.stored("access_token"));
        line(out, "  A SCRIPT ON THE OPEN PAGE ASKS THE KEY FOR A PROOF:");
        line(out, "      -> " + answer(script.requestFromPage(page, get(BankApi.ACCOUNTS, "DPoP", page.stored("access_token"))
                .withHeader("DPoP", proofFromScript))));
        line(out, "      <- DPOP STOPS THEFT, NOT USE WHILE THE PAGE IS OPEN");
        line(out, "  keep scripts off the page (a strict Content-Security-Policy),");
        line(out, "  and keep tokens out of it (the BFF)");
    }
}
