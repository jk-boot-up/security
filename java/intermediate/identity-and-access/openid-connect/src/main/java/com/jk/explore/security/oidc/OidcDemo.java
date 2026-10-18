package com.jk.explore.security.oidc;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

    private static String outcome(Check check) {
        try {
            check.run();
            return "accepted";
        } catch (InvalidTokenException e) {
            return "refused (" + e.getMessage() + ")";
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
        line(out, "  the store sends Ana to the bank with the scope \"openid email\", and a state and a nonce kept for her browser");
        line(out, "  the bank's reply holds two tokens: an ID token for the store, and an access token for the bank's API");
        Map<String, Object> id = claimsOf(login.tokens().idToken());
        Map<String, Object> access = claimsOf(login.tokens().accessToken());
        line(out, "  the ID token's claims: " + String.join(", ", id.keySet()));
        line(out, "  the ID token is for " + id.get("aud") + "; the access token is for " + access.get("aud"));
        Store.Account account = store.signedIn(browser).orElseThrow();
        line(out, "  Ana is signed in to the store as " + account.key() + ", at level " + id.get("acr") + " with " + id.get("amr"));
        line(out, "  the access token handed to the store as proof of sign-in: "
                + outcome(() -> store.signInWithToken(new Store.Browser("x"), login.tokens().accessToken(), BANK, login.request().nonce()))
                + "  <- AN ACCESS TOKEN SAYS WHAT IS ALLOWED, NOT WHO SIGNED IN");
        Store.Browser plain = new Store.Browser("plain");
        line(out, "  a sign-in that asks without the openid scope: " + outcome(() -> {
            Provider.AuthorizationRequest request = store.beginLogin(plain, BANK, "accounts.read");
            store.callback(plain, w.bank.authorize(request, ANA));
        }));
    }

    private static void act2Validation(StringBuilder out) throws InvalidTokenException {
        line(out, "TWO. Checking the ID token, as OpenID Connect Core requires.");
        World w = new World();
        ProviderConfig config = w.discover(BANK);
        String nonce = "nonce-for-this-browser";
        long now = w.clock.now();
        Map<String, Object> good = w.bank.sampleIdClaims(ANA, Store.CLIENT_ID, nonce);
        line(out, "  the bank's own ID token: " + check(w.bank.sign("JWT", good), config, nonce, now));
        line(out, "  the same claims signed with a key the bank never published, under the bank's key id: "
                + check(Provider.signWith("bank-2026-a", STRANGER_KEY, "JWT", good), config, nonce, now));
        line(out, "  issuer " + BANK + "/ with one extra slash: " + check(w.bank.sign("JWT", with(good, "iss", BANK + "/")), config, nonce, now));
        line(out, "  issuer " + WALLET + ", signed by the bank: " + check(w.bank.sign("JWT", with(good, "iss", WALLET)), config, nonce, now));
        line(out, "  audience budget-app: " + check(w.bank.sign("JWT", with(good, "aud", "budget-app")), config, nonce, now));
        Map<String, Object> two = with(good, "aud", List.of(Store.CLIENT_ID, "budget-app"));
        line(out, "  audiences store-web and budget-app, with no azp: " + check(w.bank.sign("JWT", two), config, nonce, now)
                + "; with azp store-web: " + check(w.bank.sign("JWT", with(two, "azp", Store.CLIENT_ID)), config, nonce, now));
        Map<String, Object> old = with(with(good, "iat", now - 329), "exp", now - 29);
        Map<String, Object> older = with(with(good, "iat", now - 330), "exp", now - 30);
        line(out, "  twenty-nine seconds past its expiry: " + check(w.bank.sign("JWT", old), config, nonce, now)
                + ", inside the " + IdTokenValidator.SKEW_SECONDS + " second skew; thirty seconds past: " + check(w.bank.sign("JWT", older), config, nonce, now));
        line(out, "  a nonce from another sign-in: " + check(w.bank.sign("JWT", with(good, "nonce", "someone-elses-nonce")), config, nonce, now)
                + "  <- A TOKEN FROM ANOTHER LOGIN CANNOT BE REPLAYED INTO THIS ONE");
        Map<String, Object> noNonce = new LinkedHashMap<>(good);
        noNonce.remove("nonce");
        line(out, "  no nonce at all: " + check(w.bank.sign("JWT", noNonce), config, nonce, now));
        Map<String, Object> noneHeader = new LinkedHashMap<>();
        noneHeader.put("alg", "none");
        noneHeader.put("kid", "bank-2026-a");
        String unsigned = Base64Url.encode(Json.write(noneHeader).getBytes(StandardCharsets.UTF_8)) + "."
                + Base64Url.encode(Json.write(good).getBytes(StandardCharsets.UTF_8)) + ".";
        line(out, "  a header saying alg none: " + check(unsigned, config, nonce, now));
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
        line(out, "  the store is configured with one value, the issuer " + BANK);
        ProviderConfig config = w.discover(BANK);
        line(out, "  it reads " + Discovery.metadataUrl(BANK) + ", which names the issuer " + config.issuer() + ": accepted");
        line(out, "  keys loaded from " + BANK + "/jwks.json: " + String.join(", ", config.keys().ids()));

        Web forged = new Web();
        Map<String, Object> metadata = Json.parseObject(w.bank.metadata());
        forged.serve(Discovery.metadataUrl(BANK), () -> Json.write(with(metadata, "issuer", WALLET)));
        line(out, "  a document at that address naming the issuer " + WALLET + ": " + outcome(() -> Discovery.discover(BANK, forged::get))
                + "  <- ONE PROVIDER CANNOT POSE AS ANOTHER");
        Web elsewhere = new Web();
        elsewhere.serve(Discovery.metadataUrl(BANK), () -> Json.write(with(metadata, "jwks_uri", "https://keys.example/jwks.json")));
        line(out, "  a document whose keys are on https://keys.example: " + outcome(() -> Discovery.discover(BANK, elsewhere::get)));

        String nonce = "n";
        String oldToken = w.bank.sign("JWT", w.bank.sampleIdClaims(ANA, Store.CLIENT_ID, nonce));
        w.bank.addKey("bank-2026-b", BANK_KEY_B);
        w.bank.signWith("bank-2026-b");
        String newToken = w.bank.sign("JWT", w.bank.sampleIdClaims(ANA, Store.CLIENT_ID, nonce));
        ProviderConfig reloaded = w.discover(BANK);
        line(out, "  the bank adds a second key and signs new tokens with it; the store reloads the key set: " + String.join(", ", reloaded.keys().ids()));
        long now = w.clock.now();
        line(out, "  a token signed with the old key: " + check(oldToken, reloaded, nonce, now) + "; with the new key: " + check(newToken, reloaded, nonce, now));
        w.bank.retireKey("bank-2026-a");
        ProviderConfig after = w.discover(BANK);
        line(out, "  the bank retires bank-2026-a; after a reload, the old key's token: " + check(oldToken, after, nonce, now)
                + "; a new token: " + check(newToken, after, nonce, now));

        try (LoopbackProvider loop = new LoopbackProvider(w.clock, seed(0x55))) {
            loop.provider().addUser(new Provider.User(ANA, SHARED_EMAIL, true, "Ana"));
            ProviderConfig real = Discovery.discover(loop.provider().issuer(), LoopbackProvider::fetch);
            String token = loop.provider().sign("JWT", loop.provider().sampleIdClaims(ANA, Store.CLIENT_ID, nonce));
            boolean accepted = check(token, real, nonce, now).equals("accepted");
            line(out, "  over real HTTP on this machine: metadata and keys loaded: " + real.keys().ids().equals(List.of("loop-1"))
                    + ", an ID token from that provider accepted: " + accepted);
        }
    }

    private static void act4WhoSignedIn(StringBuilder out) throws InvalidTokenException {
        line(out, "FOUR. Who signed in: issuer and subject, never the email.");
        World w = new World();
        line(out, "  the bank says " + ANA + " has the email " + SHARED_EMAIL + ", verified");
        line(out, "  wallet.example says its user " + MALLORY + " also has the email " + SHARED_EMAIL + ", not verified");
        Store naive = w.store(Store.AccountKey.EMAIL);
        login(naive, new Store.Browser("ana"), w.bank, ANA, "openid email");
        Store.Browser malloryBrowser = new Store.Browser("mallory");
        login(naive, malloryBrowser, w.wallet, MALLORY, "openid email");
        Store.Account taken = naive.signedIn(malloryBrowser).orElseThrow();
        line(out, "  a store that keys accounts by email: Mallory signs in with the wallet and gets Ana's account: "
                + (naive.accountCount() == 1 && taken.subject().equals(ANA)) + "  <- ACCOUNT TAKEOVER");
        Store store = w.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Store.Browser ana = new Store.Browser("ana");
        Login anaLogin = login(store, ana, w.bank, ANA, "openid email");
        Store.Browser mallory = new Store.Browser("mallory");
        login(store, mallory, w.wallet, MALLORY, "openid email");
        line(out, "  keyed by issuer and subject: " + store.accountCount() + " accounts, " + store.signedIn(ana).orElseThrow().key()
                + " and " + store.signedIn(mallory).orElseThrow().key());
        line(out, "  Mallory's account has no email, because the wallet did not verify it: " + (store.signedIn(mallory).orElseThrow().email() == null));
        Map<String, Object> info = w.bank.userinfo(anaLogin.tokens().accessToken());
        line(out, "  userinfo with Ana's access token: sub " + info.get("sub") + ", email " + info.get("email")
                + "; the same subject as the ID token: " + info.get("sub").equals(claimsOf(anaLogin.tokens().idToken()).get("sub")));
        line(out, "  userinfo with the ID token instead: " + outcome(() -> w.bank.userinfo(anaLogin.tokens().idToken())));
        w.clock.advance(Provider.ACCESS_TOKEN_SECONDS + 1);
        line(out, "  userinfo with the access token after it expires: " + outcome(() -> w.bank.userinfo(anaLogin.tokens().accessToken())));
    }

    private static void act5Logout(StringBuilder out) throws InvalidTokenException {
        line(out, "FIVE. Signing out, and the bill.");
        World w = new World();
        Store store = w.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Store.Browser ana = new Store.Browser("ana");
        login(store, ana, w.bank, ANA, "openid email");
        String redirect = store.logout(ana);
        line(out, "  Ana signs out at the store: the store's session ended: " + store.signedIn(ana).isEmpty());
        line(out, "  her browser is sent to " + redirect.substring(0, redirect.indexOf('?')) + " with "
                + String.join(", ", Store.parameterNames(redirect)));

        Store.Browser laptop = new Store.Browser("laptop");
        Store.Browser phone = new Store.Browser("phone");
        Login onLaptop = login(store, laptop, w.bank, ANA, "openid email");
        login(store, phone, w.bank, ANA, "openid email");
        String sid = (String) claimsOf(onLaptop.tokens().idToken()).get("sid");
        line(out, "  Ana is signed in on a laptop and a phone: " + store.sessionCount() + " store sessions");
        String logoutToken = w.bank.logoutToken(Store.CLIENT_ID, ANA, sid);
        Jws.Parts parts = Jws.parse(logoutToken);
        Map<String, Object> claims = claimsOf(logoutToken);
        line(out, "  the bank ends the laptop's session and sends a logout token, typ " + parts.header().get("typ") + ", with the logout event, a sid and no nonce: "
                + outcome(() -> store.backchannelLogout(logoutToken)));
        line(out, "  store sessions left: " + store.sessionCount() + ", laptop signed in: " + store.signedIn(laptop).isPresent()
                + ", phone signed in: " + store.signedIn(phone).isPresent());
        line(out, "  a logout token with a nonce: " + outcome(() -> store.backchannelLogout(w.bank.sign("logout+jwt", with(claims, "nonce", "x")))));
        Map<String, Object> noEvent = new LinkedHashMap<>(claims);
        noEvent.remove("events");
        line(out, "  a logout token without the event: " + outcome(() -> store.backchannelLogout(w.bank.sign("logout+jwt", noEvent))));
        line(out, "  an ID token sent as a logout token: " + outcome(() -> store.backchannelLogout(onLaptop.tokens().idToken())));
        line(out, "  OpenID Connect has many checks and options, so production code uses a certified library and a real provider");
        line(out, "  the ID token is for the store only: never send it to an API, and never take an access token as a sign-in");
        line(out, "  signing out must reach every party: the store, the provider, and through back-channel logout every other application");
    }
}
