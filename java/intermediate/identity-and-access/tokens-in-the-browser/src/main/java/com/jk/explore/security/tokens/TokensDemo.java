package com.jk.explore.security.tokens;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Map;

/** Runs the single-page app story in four numbered acts and prints exact, repeatable results. Tokens and keys are random and never printed. */
public class TokensDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static KeyPair newKey() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(new ECGenParameterSpec("secp256r1"));
        return g.generateKeyPair();
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        SimClock clock = new SimClock();
        AuthServer auth = new AuthServer();
        ResourceApi api = new ResourceApi(auth, clock);

        out.append("== 1. A token in local storage ==\n");
        BrowserPage spa = new BrowserPage();
        String bearer = auth.issue("ana", null);
        spa.storeInLocalStorage("access_token", bearer);
        out.append("the app calls the API with its token      -> ").append(api.call("GET", "/balance", "Bearer " + bearer, null)).append('\n');

        out.append("\n== 2. One injected script takes the token away ==\n");
        Map<String, String> loot = spa.whatAnInjectedScriptCanSteal();
        out.append("what the injected script can read         -> ").append(loot.keySet()).append('\n');
        String stolen = loot.get("localStorage.access_token");
        out.append("the attacker's own computer uses it       -> ").append(api.call("GET", "/balance", "Bearer " + stolen, null)).append("  <- NO KEY, NO PAGE, STILL WORKS\n");

        out.append("\n== 3. A backend for the frontend keeps the token ==\n");
        Bff bff = new Bff(auth, api);
        BrowserPage page = new BrowserPage();
        String sid = bff.signIn("ana");
        page.setCookie(Bff.COOKIE, sid, true);
        out.append("what the injected script can read         -> ").append(page.whatAnInjectedScriptCanSteal().keySet()).append('\n');
        out.append("the page calls the BFF with its cookie    -> ").append(bff.handle("GET", "/balance", page.cookieHeader(), "https://app.bank.example", null)).append('\n');
        out.append("the attacker's computer with no cookie    -> ").append(bff.handle("GET", "/balance", null, null, null)).append('\n');
        out.append("a form post from evil.example (cookie)    -> ").append(bff.handle("POST", "/transfer", page.cookieHeader(), "https://evil.example", null)).append('\n');
        out.append("the script rides the session in the page  -> ").append(bff.handle("POST", "/transfer", page.cookieHeader(), "https://app.bank.example", "fetch"))
                .append("  <- WHILE THE PAGE IS OPEN\n");
        bff.signOut(sid);
        out.append("after sign-out, the same cookie           -> ").append(bff.handle("GET", "/balance", page.cookieHeader(), "https://app.bank.example", null)).append('\n');

        out.append("\n== 4. A token that needs the key as well (DPoP) ==\n");
        KeyPair key = newKey();
        String thumbprint = Jwk.thumbprint((java.security.interfaces.ECPublicKey) key.getPublic());
        String bound = auth.issue("ana", thumbprint);
        String url = ResourceApi.BASE + "/balance";
        String proof = DpopProof.create(key, "GET", url, bound, clock.now(), "id-1");
        out.append("the app: token + a proof from its key     -> ").append(api.call("GET", "/balance", "DPoP " + bound, proof)).append('\n');
        out.append("the same proof sent again (replay)        -> ").append(api.call("GET", "/balance", "DPoP " + bound, proof)).append('\n');
        out.append("the stolen token as a bearer token        -> ").append(api.call("GET", "/balance", "Bearer " + bound, null)).append('\n');
        out.append("the stolen token, no proof                -> ").append(api.call("GET", "/balance", "DPoP " + bound, null)).append('\n');
        KeyPair attacker = newKey();
        out.append("the stolen token, a proof from their key  -> ")
                .append(api.call("GET", "/balance", "DPoP " + bound, DpopProof.create(attacker, "GET", url, bound, clock.now(), "id-2"))).append('\n');
        out.append("a proof meant for a transfer              -> ")
                .append(api.call("GET", "/balance", "DPoP " + bound, DpopProof.create(key, "POST", ResourceApi.BASE + "/transfer", bound, clock.now(), "id-3"))).append('\n');
        out.append("a proof saved for later                   -> ")
                .append(api.call("GET", "/balance", "DPoP " + bound, DpopProof.create(key, "GET", url, bound, clock.now() - 600, "id-4"))).append('\n');
        out.append("a fresh proof from the real key           -> ")
                .append(api.call("GET", "/balance", "DPoP " + bound, DpopProof.create(key, "GET", url, bound, clock.now(), "id-5"))).append('\n');
        return out.toString();
    }
}
