package com.jk.explore.security.bff;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Optional;

/**
 * A single-page banking app and where its tokens should live, in five acts: tokens in the browser's storage, a backend for the
 * frontend that keeps them on the server, requests from another site, tokens bound to a key with DPoP, and the bill.
 * The clock is simulated, keys come from fixed seeds, and only facts that do not depend on random values are printed.
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

    private static String status(Response response) {
        return response.status() == 0 ? response.body() : response.status() + (response.status() >= 400 ? " (" + response.body() + ")" : "");
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
        line(out, "  the banking app signs Ana in and keeps its access token and refresh token in localStorage");
        Response own = browser.send(Browser.BANK_SITE, Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "Bearer " + browser.stored("access_token")));
        line(out, "  the app reads Ana's balance: " + status(own));
        PageScript script = new PageScript();
        line(out, "  a third-party script on the page reads what scripts can read: " + script.collect(browser) + " tokens taken");
        browser.closePage();
        line(out, "  it sends them to its own server, and Ana closes the page");
        String stolenAccess = script.sentHome().get(0);
        String stolenRefresh = script.sentHome().get(1);
        line(out, "  from elsewhere, the stolen access token reads the balance: "
                + status(api.handle(Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "Bearer " + stolenAccess))));
        clock.advance(AuthServer.ACCESS_SECONDS);
        line(out, "  five minutes later: " + status(api.handle(Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "Bearer " + stolenAccess))));
        clock.advance(10L * 24 * 3600);
        Optional<AuthServer.Tokens> renewed = auth.refresh(stolenRefresh, null);
        line(out, "  ten days later the stolen refresh token gets a new access token: " + renewed.isPresent()
                + ", which reads the balance: " + status(api.handle(Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "Bearer " + renewed.orElseThrow().accessToken())))
                + "  <- ONE PAGE VISIT, A MONTH OF ACCESS");
        line(out, "  anything a script can read, a script can send away");
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
        line(out, "  the BFF signs Ana in on the server; the browser gets one cookie: " + cookie.describe());
        line(out, "  the cookie keeps the __Host- rules: " + cookie.hostPrefixValid() + ", and the BFF holds " + bff.tokensHeld() + " tokens the browser never sees");
        line(out, "  the app reads the balance through the BFF: " + status(browser.send(Browser.BANK_SITE, Request.of("GET", Bff.ACCOUNTS))));
        PageScript script = new PageScript();
        line(out, "  the same third-party script reads what scripts can read: " + script.collect(browser) + " tokens taken, and the cookie is not readable");
        line(out, "  while the page is open, the script can still ask the BFF for the balance: "
                + status(script.requestFromPage(browser, Request.of("GET", Bff.ACCOUNTS))) + "  <- A SCRIPT ON THE PAGE CAN STILL ACT AS ANA");
        browser.closePage();
        line(out, "  once the page is closed: " + status(script.requestFromPage(browser, Request.of("GET", Bff.ACCOUNTS)))
                + ", and nothing it took works anywhere else");
        clock.advance(AuthServer.ACCESS_SECONDS + 60);
        Browser later = new Browser(Browser.Kind.MODERN, bff, api);
        later.receiveCookie(Bff.COOKIE + "=" + cookie.value() + "; Path=/; Secure; HttpOnly; SameSite=Strict");
        line(out, "  six minutes later Ana opens the app again; the BFF refreshes the token on the server: "
                + status(later.send(Browser.BANK_SITE, Request.of("GET", Bff.ACCOUNTS))));
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
        line(out, "  a hidden form on evil.example posts a transfer to the BFF; SameSite=Strict keeps the cookie off it: "
                + status(modern.send("evil.example", transfer)));
        line(out, "  in an older browser that ignores SameSite, the cookie goes, but the bank's request header does not: "
                + status(older.send("evil.example", transfer)));
        line(out, "  a script on evil.example tries to add the header itself: "
                + status(older.send("evil.example", transfer.withHeader(Bff.REQUEST_HEADER, "1"))));
        line(out, "  the bank's own page sends the transfer with the header: " + status(modern.send(Browser.BANK_SITE, transfer.withHeader(Bff.REQUEST_HEADER, "1"))));
        line(out, "  transfers made: " + api.transfersMade() + ", and only the one the bank's own page asked for");
    }

    private static void act4Dpop(StringBuilder out) {
        line(out, "FOUR. Tokens bound to a key, with DPoP.");
        SimClock clock = new SimClock(START);
        AuthServer auth = new AuthServer(clock, seeded("bank signing key"));
        BankApi api = new BankApi(clock, auth.publicKey());
        Browser browser = new Browser(Browser.Kind.MODERN, new Bff(auth, api, clock), api);
        DpopKey appKey = new DpopKey(seeded("ana's browser key"));
        browser.holdKey(appKey);
        AuthServer.Tokens tokens = auth.issue("ana", "accounts.read payments.write", appKey.thumbprint());
        browser.store("access_token", tokens.accessToken());
        browser.store("refresh_token", tokens.refreshToken());
        line(out, "  the app holds a key that scripts cannot read, and the token carries the key's thumbprint in cnf.jkt");
        String token = browser.stored("access_token");
        String proof = browser.keyForThisPage().proof("GET", BankApi.ACCOUNTS, clock.now(), token);
        Request withProof = Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "DPoP " + token).withHeader("DPoP", proof);
        line(out, "  the app sends the token with a fresh proof signed by its key: " + status(browser.send(Browser.BANK_SITE, withProof)));
        PageScript script = new PageScript();
        script.collect(browser);
        String stolen = script.sentHome().get(0);
        line(out, "  a script takes the token from localStorage and sends it as a plain bearer token: "
                + status(api.handle(Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "Bearer " + stolen))));
        DpopKey thiefKey = new DpopKey(seeded("someone else's key"));
        line(out, "  with a proof from a key of its own: " + status(api.handle(Request.of("GET", BankApi.ACCOUNTS)
                .withHeader("Authorization", "DPoP " + stolen).withHeader("DPoP", thiefKey.proof("GET", BankApi.ACCOUNTS, clock.now(), stolen)))));
        line(out, "  the app's own request copied and sent again: " + status(api.handle(withProof)));
        line(out, "  the app's proof for reading the balance, used for a transfer: " + status(api.handle(Request.of("POST", BankApi.TRANSFERS)
                .withHeader("Authorization", "DPoP " + token).withHeader("DPoP", browser.keyForThisPage().proof("GET", BankApi.ACCOUNTS, clock.now(), token)))));
        String early = browser.keyForThisPage().proof("GET", BankApi.ACCOUNTS, clock.now(), token);
        clock.advance(120);
        line(out, "  a proof made two minutes ago, with the token still valid: " + status(api.handle(Request.of("GET", BankApi.ACCOUNTS)
                .withHeader("Authorization", "DPoP " + token).withHeader("DPoP", early))));
        DpopVerifier tokenEndpoint = new DpopVerifier(clock);
        String stolenRefresh = script.sentHome().get(1);
        String thiefProofKey = verified(tokenEndpoint, thiefKey.proof("POST", AuthServer.ISSUER + "/token", clock.now(), null));
        line(out, "  the stolen refresh token with the thief's key: " + (auth.refresh(stolenRefresh, thiefProofKey).isPresent() ? "new tokens" : "refused"));
        String appProofKey = verified(tokenEndpoint, browser.keyForThisPage().proof("POST", AuthServer.ISSUER + "/token", clock.now(), null));
        line(out, "  the same refresh token presented by the app, with its own key: " + (auth.refresh(stolenRefresh, appProofKey).isPresent() ? "new tokens" : "refused"));
        line(out, "  a stolen token is useless without the key, and the key never leaves the browser");
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
        line(out, "  two BFF servers, each with its own session store: the second one answers Ana's cookie with "
                + status(sameCookie.send(Browser.BANK_SITE, Request.of("GET", Bff.ACCOUNTS))) + ", so the sessions need a shared store");
        line(out, "  every API call now passes through one more server, which must be run, scaled and kept up to date");
        DpopKey oldKey = new DpopKey(seeded("ana's browser key"));
        AuthServer.Tokens bound = auth.issue("ana", "accounts.read", oldKey.thumbprint());
        DpopKey newKey = new DpopKey(seeded("a key made after the storage was cleared"));
        line(out, "  clearing the browser's storage loses the DPoP key; the old token with a new key: "
                + status(api.handle(Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "DPoP " + bound.accessToken())
                .withHeader("DPoP", newKey.proof("GET", BankApi.ACCOUNTS, clock.now(), bound.accessToken())))) + ", so Ana signs in again");
        Browser page = new Browser(Browser.Kind.MODERN, first, api);
        page.holdKey(oldKey);
        page.store("access_token", bound.accessToken());
        PageScript script = new PageScript();
        String proofFromScript = page.keyForThisPage().proof("GET", BankApi.ACCOUNTS, clock.now(), page.stored("access_token"));
        line(out, "  a script on the open page can still ask the key for a proof and read the balance: "
                + status(script.requestFromPage(page, Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "DPoP " + page.stored("access_token"))
                .withHeader("DPoP", proofFromScript))) + "  <- DPOP STOPS THEFT, NOT USE WHILE THE PAGE IS OPEN");
        line(out, "  so keep scripts off the page (a strict Content-Security-Policy), and keep tokens out of it (the BFF)");
    }
}
