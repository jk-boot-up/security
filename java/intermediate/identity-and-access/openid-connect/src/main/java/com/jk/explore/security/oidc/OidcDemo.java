package com.jk.explore.security.oidc;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs the "sign in with the bank" story in five numbered acts and prints exact, repeatable results. */
public class OidcDemo {

    static final long START = 1_800_000_000L;
    static final String BANK = "https://bank.example";
    static final String WALLET = "https://wallet.example";
    static final String ANA = "ana-0001";
    static final String MALLORY = "mal-3301";
    static final String SHARED_EMAIL = "ana@mail.example";

    static byte[] seed(int fill) {
        byte[] seed = new byte[32];
        Arrays.fill(seed, (byte) fill);
        return seed;
    }

    static final byte[] BANK_KEY_A = seed(0x11);
    static final byte[] BANK_KEY_B = seed(0x22);
    static final byte[] WALLET_KEY = seed(0x33);
    static final byte[] STRANGER_KEY = seed(0x44);

    /** One fresh, fixed world per act: a clock, the bank, a second provider, the web that serves their metadata, and the store. */
    static final class World {
        final SimClock clock = new SimClock(START);
        final Web web = new Web();
        final Provider bank = new Provider(BANK, clock).addKey("bank-2026-a", BANK_KEY_A)
                .addUser(new Provider.User(ANA, SHARED_EMAIL, true, "Ana"))
                .addClient(Store.CLIENT_ID, Store.REDIRECT_URI);
        final Provider wallet = new Provider(WALLET, clock).addKey("wallet-1", WALLET_KEY)
                .addUser(new Provider.User(MALLORY, SHARED_EMAIL, false, "Mallory"))
                .addClient(Store.CLIENT_ID, Store.REDIRECT_URI);

        World() {
            bank.publish(web);
            wallet.publish(web);
        }

        ProviderConfig discover(String issuer) throws InvalidTokenException {
            return Discovery.discover(issuer, web::get);
        }

        Store store(Store.AccountKey key) throws InvalidTokenException {
            Store store = new Store(clock, key);
            store.trust(discover(BANK), bank);
            store.trust(discover(WALLET), wallet);
            return store;
        }
    }

    /** The tokens of one sign-in, kept so the demo can show what each one is for. */
    record Login(Provider.AuthorizationRequest request, Provider.TokenResponse tokens) {
    }

    static Login login(Store store, Store.Browser browser, Provider provider, String subject, String scope) throws InvalidTokenException {
        Provider.AuthorizationRequest request = store.beginLogin(browser, provider.issuer(), scope);
        Provider.AuthorizationResponse response = provider.authorize(request, subject);
        Provider.TokenResponse tokens = provider.token(response.code(), Store.CLIENT_ID);
        store.signInWithToken(browser, tokens.idToken(), provider.issuer(), request.nonce());
        return new Login(request, tokens);
    }

    static Map<String, Object> claimsOf(String token) throws InvalidTokenException {
        return Json.parseObject(new String(Jws.parse(token).payload(), StandardCharsets.UTF_8));
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        act1Identity(out);
        act2Validation(out);
        act3Discovery(out);
        act4WhoSignedIn(out);
        act5Logout(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A section header: what follows is one input or one result. */
    private static void section(StringBuilder out, String header) {
        line(out, "  " + header);
    }

    private static void item(StringBuilder out, String text) {
        line(out, "    " + text);
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    /** The first characters of a long value, so a line stays readable. */
    static String cut(String text, int keep) {
        return text.length() <= keep ? text : text.substring(0, keep) + "...";
    }

    private static String outcome(Check check) {
        try {
            check.run();
            return "accepted";
        } catch (InvalidTokenException e) {
            return "refused: " + e.getMessage();
        }
    }

    interface Check {
        void run() throws InvalidTokenException;
    }

    private static void act1Identity(StringBuilder out) throws InvalidTokenException {
        line(out, "ONE. OAuth gives access; OpenID Connect adds who signed in.");
        World w = new World();
        Store store = w.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Store.Browser browser = new Store.Browser("ana");
        Login login = login(store, browser, w.bank, ANA, "openid email");
        Provider.AuthorizationRequest request = login.request();
        section(out, "THE REQUEST (the store sends Ana's browser to the bank):");
        item(out, w.discover(BANK).authorizationEndpoint());
        item(out, pad("client_id", 13) + request.clientId());
        item(out, pad("redirect_uri", 13) + request.redirectUri());
        item(out, pad("scope", 13) + request.scope());
        item(out, pad("state", 13) + "<random, kept for this browser>");
        item(out, pad("nonce", 13) + "<random, kept for this browser>");
        section(out, "THE RESPONSE (the bank's token endpoint, two tokens):");
        Map<String, Object> id = claimsOf(login.tokens().idToken());
        Map<String, Object> access = claimsOf(login.tokens().accessToken());
        item(out, pad("ID token", 14) + pad("typ " + Jws.parse(login.tokens().idToken()).header().get("typ"), 12) + "aud " + id.get("aud"));
        item(out, pad("access token", 14) + pad("typ " + Jws.parse(login.tokens().accessToken()).header().get("typ"), 12) + "aud " + access.get("aud"));
        section(out, "THE ID TOKEN'S CLAIMS (who signed in, for whom, when):");
        long iat = (Long) id.get("iat");
        long exp = (Long) id.get("exp");
        item(out, pad("iss", 11) + id.get("iss") + "  (who says so)");
        item(out, pad("sub", 11) + id.get("sub") + "  (who signed in)");
        item(out, pad("aud", 11) + id.get("aud") + "  (for which app)");
        item(out, pad("iat", 11) + iat + "  (issued, seconds since 1970)");
        item(out, pad("exp", 11) + exp + "  = iat + " + (exp - iat) + " seconds");
        item(out, pad("auth_time", 11) + id.get("auth_time"));
        item(out, pad("nonce", 11) + "<random>  equals the one kept: " + request.nonce().equals(id.get("nonce")));
        item(out, pad("acr, amr", 11) + id.get("acr") + ", " + id.get("amr"));
        item(out, pad("sid", 11) + id.get("sid") + "  (the bank's session)");
        item(out, pad("email", 11) + id.get("email") + ", email_verified " + id.get("email_verified"));
        section(out, "THE RESULT:");
        item(out, "Ana is signed in as " + store.signedIn(browser).orElseThrow().key());
        section(out, "THE ACCESS TOKEN, OFFERED AS PROOF OF SIGN-IN:");
        item(out, outcome(() -> store.signInWithToken(new Store.Browser("x"), login.tokens().accessToken(), BANK, request.nonce())));
        item(out, "<- AN ACCESS TOKEN SAYS WHAT IS ALLOWED, NOT WHO");
        Store.Browser plain = new Store.Browser("plain");
        section(out, "A SIGN-IN WITHOUT THE openid SCOPE (scope accounts.read):");
        item(out, outcome(() -> {
            Provider.AuthorizationRequest r = store.beginLogin(plain, BANK, "accounts.read");
            store.callback(plain, w.bank.authorize(r, ANA));
        }));
    }

    private static void act2Validation(StringBuilder out) throws InvalidTokenException {
        line(out, "TWO. Checking the ID token, as OpenID Connect Core requires.");
        World w = new World();
        ProviderConfig config = w.discover(BANK);
        String nonce = "nonce-for-this-browser";
        long now = w.clock.now();
        Map<String, Object> good = w.bank.sampleIdClaims(ANA, Store.CLIENT_ID, nonce);
        String token = w.bank.sign("JWT", good);
        Jws.Parts parts = Jws.parse(token);
        String claimsJson = new String(parts.payload(), StandardCharsets.UTF_8);
        section(out, "BEFORE SIGNING (a header and the claims, as JSON):");
        item(out, "header  " + Json.write(parts.header()));
        item(out, "claims  " + cut(claimsJson, 48));
        item(out, "        (" + good.size() + " claims, " + parts.payload().length + " bytes)");
        section(out, "HOW THE BANK SIGNS IT (step by step):");
        item(out, "1. header part = base64url(header)");
        item(out, "   " + cut(parts.headerPart(), 40) + " (" + parts.headerPart().length() + " chars)");
        item(out, "2. claims part = base64url(claims)");
        item(out, "   " + cut(parts.payloadPart(), 40) + " (" + parts.payloadPart().length() + " chars)");
        item(out, "3. signing input = header part + \".\" + claims part");
        item(out, "   " + parts.signingInput().length + " bytes");
        item(out, "4. signature = Ed25519(private key bank-2026-a, input)");
        String signaturePart = token.substring(token.lastIndexOf('.') + 1);
        item(out, "   " + parts.signature().length + " bytes = " + cut(signaturePart, 32) + " (" + signaturePart.length() + " chars)");
        section(out, "AFTER SIGNING (the ID token: three parts, joined by dots):");
        item(out, cut(parts.headerPart(), 12) + "." + cut(parts.payloadPart(), 12) + "." + cut(signaturePart, 12));
        item(out, token.length() + " characters in all");
        section(out, "HOW THE STORE CHECKS IT (in this order):");
        item(out, "1. alg is EdDSA, the store's choice: " + "EdDSA".equals(parts.header().get("alg")));
        item(out, "2. kid bank-2026-a is in the published keys: " + config.keys().find("bank-2026-a").isPresent());
        item(out, "3. Ed25519 verify with that public key: "
                + Ed25519.verify(config.keys().find("bank-2026-a").orElseThrow(), parts.signingInput(), parts.signature()));
        item(out, "4. iss equals " + config.issuer() + " exactly: " + config.issuer().equals(good.get("iss")));
        item(out, "5. aud contains " + Store.CLIENT_ID + ": " + Store.CLIENT_ID.equals(good.get("aud")));
        long exp = (Long) good.get("exp");
        item(out, "6. now " + now + " < exp " + exp + " + " + IdTokenValidator.SKEW_SECONDS + ": " + (now < exp + IdTokenValidator.SKEW_SECONDS));
        item(out, "7. nonce equals " + nonce + ": " + nonce.equals(good.get("nonce")));
        section(out, "THE RESULT:");
        item(out, check(token, config, nonce, now));

        section(out, "ONE THING CHANGED AT A TIME (the input, then the result):");
        item(out, "claims signed with a key the bank never published");
        item(out, "  -> " + check(Provider.signWith("bank-2026-a", STRANGER_KEY, "JWT", good), config, nonce, now));
        item(out, "iss " + BANK + "/ (one extra slash)");
        item(out, "  -> " + check(w.bank.sign("JWT", with(good, "iss", BANK + "/")), config, nonce, now));
        item(out, "iss " + WALLET + ", signed by the bank");
        item(out, "  -> " + check(w.bank.sign("JWT", with(good, "iss", WALLET)), config, nonce, now));
        item(out, "aud budget-app");
        item(out, "  -> " + check(w.bank.sign("JWT", with(good, "aud", "budget-app")), config, nonce, now));
        Map<String, Object> two = with(good, "aud", List.of(Store.CLIENT_ID, "budget-app"));
        item(out, "aud [store-web, budget-app], no azp");
        item(out, "  -> " + check(w.bank.sign("JWT", two), config, nonce, now));
        item(out, "aud [store-web, budget-app], azp store-web");
        item(out, "  -> " + check(w.bank.sign("JWT", with(two, "azp", Store.CLIENT_ID)), config, nonce, now));
        section(out, "THE NONCE AND THE HEADER (input, then result):");
        item(out, "nonce someone-elses-nonce, from another sign-in");
        item(out, "  -> " + check(w.bank.sign("JWT", with(good, "nonce", "someone-elses-nonce")), config, nonce, now));
        item(out, "  <- A TOKEN FROM ANOTHER LOGIN CANNOT BE REPLAYED");
        Map<String, Object> noNonce = new LinkedHashMap<>(good);
        noNonce.remove("nonce");
        item(out, "no nonce at all");
        item(out, "  -> " + check(w.bank.sign("JWT", noNonce), config, nonce, now));
        Map<String, Object> noneHeader = new LinkedHashMap<>();
        noneHeader.put("alg", "none");
        noneHeader.put("kid", "bank-2026-a");
        String unsigned = Base64Url.encode(Json.write(noneHeader).getBytes(StandardCharsets.UTF_8)) + "."
                + Base64Url.encode(Json.write(good).getBytes(StandardCharsets.UTF_8)) + ".";
        item(out, "header " + Json.write(noneHeader) + ", no signature");
        item(out, "  -> " + check(unsigned, config, nonce, now));

        section(out, "THE TIME CHECK (now " + now + ", skew " + IdTokenValidator.SKEW_SECONDS + " seconds):");
        for (long late : new long[] {29, 30}) {
            long e = now - late;
            Map<String, Object> claims = with(with(good, "iat", e - 300), "exp", e);
            item(out, "exp " + e + ", " + late + " s ago: exp + " + IdTokenValidator.SKEW_SECONDS + " = " + (e + IdTokenValidator.SKEW_SECONDS)
                    + (now < e + IdTokenValidator.SKEW_SECONDS ? " > now" : " = now"));
            item(out, "  -> " + check(w.bank.sign("JWT", claims), config, nonce, now));
        }
    }

    private static String check(String token, ProviderConfig config, String nonce, long now) {
        return outcome(() -> IdTokenValidator.validate(token, config, Store.CLIENT_ID, nonce, now));
    }

    static Map<String, Object> with(Map<String, Object> claims, String name, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(claims);
        copy.put(name, value);
        return copy;
    }

    private static void act3Discovery(StringBuilder out) throws Exception {
        line(out, "THREE. Discovery and keys.");
        World w = new World();
        section(out, "THE CONFIGURATION (the store knows one value):");
        item(out, "issuer " + BANK);
        section(out, "THE REQUEST (the address is made from the issuer):");
        item(out, "GET " + Discovery.metadataUrl(BANK));
        section(out, "THE RESPONSE (the metadata, the fields the store uses):");
        Map<String, Object> metadata = Json.parseObject(w.bank.metadata());
        for (String name : List.of("issuer", "authorization_endpoint", "token_endpoint", "userinfo_endpoint", "jwks_uri", "end_session_endpoint")) {
            item(out, pad(name, 23) + metadata.get(name));
        }
        item(out, pad("signing algorithms", 23) + metadata.get("id_token_signing_alg_values_supported"));
        ProviderConfig config = w.discover(BANK);
        section(out, "THE CHECKS:");
        item(out, "issuer equals the configured issuer: " + BANK.equals(metadata.get("issuer")));
        item(out, "jwks_uri on the issuer's own site: " + Discovery.sameOrigin(BANK, (String) metadata.get("jwks_uri")));
        section(out, "THE KEYS (GET " + metadata.get("jwks_uri") + "):");
        Map<?, ?> jwk = (Map<?, ?>) ((List<?>) Json.parseObject(w.bank.jwks()).get("keys")).get(0);
        item(out, "kid " + jwk.get("kid") + ", kty " + jwk.get("kty") + ", crv " + jwk.get("crv") + ", use " + jwk.get("use"));
        item(out, "x = " + jwk.get("x"));
        item(out, "x is the public key the bank made from its private seed:");
        String hex = HexFormat.ofDelimiter(" ").formatHex(BANK_KEY_A);
        item(out, "seed " + hex.substring(0, 8) + " ... " + hex.substring(hex.length() - 2) + " (" + BANK_KEY_A.length + " bytes, never published)");
        item(out, "store's key ids: " + String.join(", ", config.keys().ids()));

        section(out, "FORGED METADATA AT THE SAME ADDRESS (input, then result):");
        Web forged = new Web();
        forged.serve(Discovery.metadataUrl(BANK), () -> Json.write(with(metadata, "issuer", WALLET)));
        item(out, "issuer " + WALLET);
        item(out, "  -> " + outcome(() -> Discovery.discover(BANK, forged::get)));
        item(out, "  <- ONE PROVIDER CANNOT POSE AS ANOTHER");
        Web elsewhere = new Web();
        elsewhere.serve(Discovery.metadataUrl(BANK), () -> Json.write(with(metadata, "jwks_uri", "https://keys.example/jwks.json")));
        item(out, "jwks_uri https://keys.example/jwks.json");
        item(out, "  -> " + outcome(() -> Discovery.discover(BANK, elsewhere::get)));

        String nonce = "n";
        String oldToken = w.bank.sign("JWT", w.bank.sampleIdClaims(ANA, Store.CLIENT_ID, nonce));
        w.bank.addKey("bank-2026-b", BANK_KEY_B);
        w.bank.signWith("bank-2026-b");
        String newToken = w.bank.sign("JWT", w.bank.sampleIdClaims(ANA, Store.CLIENT_ID, nonce));
        long now = w.clock.now();
        section(out, "KEY ROTATION (step by step):");
        ProviderConfig reloaded = w.discover(BANK);
        item(out, "1. the bank adds bank-2026-b and signs new tokens with it");
        item(out, "   the store reloads the keys: " + String.join(", ", reloaded.keys().ids()));
        item(out, "   old token: " + check(oldToken, reloaded, nonce, now) + "; new token: " + check(newToken, reloaded, nonce, now));
        w.bank.retireKey("bank-2026-a");
        ProviderConfig after = w.discover(BANK);
        item(out, "2. the bank retires bank-2026-a; the store reloads the keys:");
        item(out, "   " + String.join(", ", after.keys().ids()));
        item(out, "   old token: " + check(oldToken, after, nonce, now));
        item(out, "   new token: " + check(newToken, after, nonce, now));

        try (LoopbackProvider loop = new LoopbackProvider(w.clock, seed(0x55))) {
            loop.provider().addUser(new Provider.User(ANA, SHARED_EMAIL, true, "Ana"));
            ProviderConfig real = Discovery.discover(loop.provider().issuer(), LoopbackProvider::fetch);
            String token = loop.provider().sign("JWT", loop.provider().sampleIdClaims(ANA, Store.CLIENT_ID, nonce));
            section(out, "OVER REAL HTTP ON THIS MACHINE (port not printed):");
            item(out, "metadata and keys loaded: " + real.keys().ids());
            item(out, "an ID token from that provider: " + check(token, real, nonce, now));
        }
    }

    private static void act4WhoSignedIn(StringBuilder out) throws InvalidTokenException {
        line(out, "FOUR. Who signed in: issuer and subject, never the email.");
        World w = new World();
        section(out, "THE TWO PROVIDERS SAY (issuer, subject, email):");
        item(out, pad(BANK, 23) + pad(ANA, 9) + SHARED_EMAIL + " verified");
        item(out, pad(WALLET, 23) + pad(MALLORY, 9) + SHARED_EMAIL + " NOT verified");
        Store naive = w.store(Store.AccountKey.EMAIL);
        Store.Browser anaBrowser = new Store.Browser("ana");
        login(naive, anaBrowser, w.bank, ANA, "openid email");
        Store.Browser malloryBrowser = new Store.Browser("mallory");
        login(naive, malloryBrowser, w.wallet, MALLORY, "openid email");
        Store.Account taken = naive.signedIn(malloryBrowser).orElseThrow();
        section(out, "BEFORE THE FIX (the store keys accounts by email):");
        item(out, "Ana signs in with the bank: account " + naive.signedIn(anaBrowser).orElseThrow().key());
        item(out, "Mallory signs in with the wallet: account " + taken.key());
        item(out, "accounts: " + naive.accountCount() + "; Mallory is in Ana's account: "
                + (naive.accountCount() == 1 && taken.subject().equals(ANA)));
        item(out, "  <- ACCOUNT TAKEOVER");
        Store store = w.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Store.Browser ana = new Store.Browser("ana");
        Login anaLogin = login(store, ana, w.bank, ANA, "openid email");
        Store.Browser mallory = new Store.Browser("mallory");
        login(store, mallory, w.wallet, MALLORY, "openid email");
        Store.Account anaAccount = store.signedIn(ana).orElseThrow();
        Store.Account malloryAccount = store.signedIn(mallory).orElseThrow();
        section(out, "AFTER THE FIX (accounts keyed by issuer | subject):");
        item(out, "Ana:     " + anaAccount.key() + ", email " + anaAccount.email());
        item(out, "Mallory: " + malloryAccount.key() + ", email " + (malloryAccount.email() == null ? "none" : malloryAccount.email()));
        item(out, "accounts: " + store.accountCount() + "; an unverified email is not kept");
        section(out, "THE USERINFO REQUEST (input, then result):");
        Map<String, Object> info = w.bank.userinfo(anaLogin.tokens().accessToken());
        item(out, "Ana's access token");
        item(out, "  -> sub " + info.get("sub") + ", email " + info.get("email"));
        item(out, "  -> same sub as the ID token: " + info.get("sub").equals(claimsOf(anaLogin.tokens().idToken()).get("sub")));
        item(out, "the ID token instead");
        item(out, "  -> " + outcome(() -> w.bank.userinfo(anaLogin.tokens().idToken())));
        w.clock.advance(Provider.ACCESS_TOKEN_SECONDS + 1);
        item(out, "the access token, " + (Provider.ACCESS_TOKEN_SECONDS + 1) + " seconds later");
        item(out, "  -> " + outcome(() -> w.bank.userinfo(anaLogin.tokens().accessToken())));
    }

    private static void act5Logout(StringBuilder out) throws InvalidTokenException {
        line(out, "FIVE. Signing out, and the bill.");
        World w = new World();
        Store store = w.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Store.Browser ana = new Store.Browser("ana");
        login(store, ana, w.bank, ANA, "openid email");
        String redirect = store.logout(ana);
        section(out, "ANA SIGNS OUT AT THE STORE (RP-initiated logout):");
        item(out, "the store's session ended: " + store.signedIn(ana).isEmpty());
        item(out, "her browser is sent to " + redirect.substring(0, redirect.indexOf('?')));
        String postLogout = redirect.substring(redirect.indexOf("post_logout_redirect_uri=") + 25, redirect.indexOf("&state="));
        item(out, "?id_token_hint=<her ID token>");
        item(out, "&post_logout_redirect_uri=" + postLogout);
        item(out, "&state=<random>");

        Store.Browser laptop = new Store.Browser("laptop");
        Store.Browser phone = new Store.Browser("phone");
        Login onLaptop = login(store, laptop, w.bank, ANA, "openid email");
        Login onPhone = login(store, phone, w.bank, ANA, "openid email");
        String sid = (String) claimsOf(onLaptop.tokens().idToken()).get("sid");
        section(out, "BEFORE THE LOGOUT TOKEN (Ana on two devices):");
        item(out, "store sessions: " + store.sessionCount() + "; laptop " + sid + ", phone " + claimsOf(onPhone.tokens().idToken()).get("sid"));
        String logoutToken = w.bank.logoutToken(Store.CLIENT_ID, ANA, sid);
        Jws.Parts parts = Jws.parse(logoutToken);
        Map<String, Object> claims = claimsOf(logoutToken);
        section(out, "THE LOGOUT TOKEN (the bank ends the laptop's session):");
        item(out, "header  typ " + parts.header().get("typ") + ", alg " + parts.header().get("alg") + ", kid " + parts.header().get("kid"));
        item(out, "claims  iss " + claims.get("iss") + ", aud " + claims.get("aud"));
        item(out, "        sub " + claims.get("sub") + ", sid " + claims.get("sid") + ", jti " + claims.get("jti"));
        item(out, "        events: " + ((Map<?, ?>) claims.get("events")).keySet().iterator().next());
        item(out, "        nonce: " + (claims.containsKey("nonce") ? "present" : "none"));
        section(out, "AFTER THE LOGOUT TOKEN:");
        item(out, outcome(() -> store.backchannelLogout(logoutToken)));
        item(out, "store sessions: " + store.sessionCount() + "; laptop signed in: " + store.signedIn(laptop).isPresent()
                + "; phone: " + store.signedIn(phone).isPresent());
        section(out, "BROKEN LOGOUT TOKENS (input, then result):");
        item(out, "a logout token with a nonce");
        item(out, "  -> " + outcome(() -> store.backchannelLogout(w.bank.sign("logout+jwt", with(claims, "nonce", "x")))));
        Map<String, Object> noEvent = new LinkedHashMap<>(claims);
        noEvent.remove("events");
        item(out, "a logout token without the event");
        item(out, "  -> " + outcome(() -> store.backchannelLogout(w.bank.sign("logout+jwt", noEvent))));
        item(out, "an ID token sent as a logout token");
        item(out, "  -> " + outcome(() -> store.backchannelLogout(onLaptop.tokens().idToken())));
        section(out, "THE BILL:");
        item(out, "many checks and options: use a certified library");
        item(out, "the ID token stays in the store; never take an access token");
        item(out, "  as a sign-in");
        item(out, "sign-out must reach the store, the provider and every app");
    }
}
