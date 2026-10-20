package com.jk.explore.security.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import org.junit.jupiter.api.Test;

class TokensTest {

    private final SimClock clock = new SimClock();
    private final AuthServer auth = new AuthServer();
    private final ResourceApi api = new ResourceApi(auth, clock);
    private static final String URL = ResourceApi.BASE + "/balance";

    private String bound(KeyPair key) {
        return auth.issue("ana", Jwk.thumbprint((ECPublicKey) key.getPublic()));
    }

    @Test
    void aBearerTokenWorksForWhoeverHoldsIt() {
        String t = auth.issue("ana", null);
        assertEquals(200, api.call("GET", "/balance", "Bearer " + t, null).status());
        assertEquals(401, api.call("GET", "/balance", "Bearer nonsense", null).status());
        assertEquals(401, api.call("GET", "/balance", null, null).status());
    }

    @Test
    void localStorageIsReadableByAnInjectedScript() {
        BrowserPage p = new BrowserPage();
        p.storeInLocalStorage("access_token", "t");
        p.setCookie("theme", "dark", false);
        p.setCookie("sid", "s", true);
        var loot = p.whatAnInjectedScriptCanSteal();
        assertEquals("t", loot.get("localStorage.access_token"));
        assertEquals("dark", loot.get("cookie.theme"));
        assertFalse(loot.containsKey("cookie.sid"));
        assertTrue(p.cookieHeader().contains("sid=s"));
    }

    @Test
    void theBffKeepsTokensOutOfTheBrowserAndRefusesForeignPosts() {
        Bff bff = new Bff(auth, api);
        String sid = bff.signIn("ana");
        String cookie = Bff.COOKIE + "=" + sid;
        assertEquals(200, bff.handle("GET", "/balance", cookie, null, null).status());
        assertEquals(403, bff.handle("POST", "/transfer", cookie, "https://evil.example", "fetch").status());
        assertEquals(403, bff.handle("POST", "/transfer", cookie, "https://app.bank.example", null).status());
        assertEquals(200, bff.handle("POST", "/transfer", cookie, "https://app.bank.example", "fetch").status());
        assertEquals(401, bff.handle("GET", "/balance", Bff.COOKIE + "=guess", null, null).status());
        bff.signOut(sid);
        assertEquals(401, bff.handle("GET", "/balance", cookie, null, null).status());
    }

    @Test
    void aBoundTokenNeedsAFreshProofFromItsKey() throws Exception {
        KeyPair key = TokensDemo.newKey();
        String t = bound(key);
        String p = DpopProof.create(key, "GET", URL, t, clock.now(), "a");
        assertEquals(200, api.call("GET", "/balance", "DPoP " + t, p).status());
        assertEquals("proof already used", api.call("GET", "/balance", "DPoP " + t, p).message());
    }

    @Test
    void aBoundTokenIsUselessAsABearerTokenOrWithoutAProof() throws Exception {
        KeyPair key = TokensDemo.newKey();
        String t = bound(key);
        assertEquals("this token needs a proof of possession", api.call("GET", "/balance", "Bearer " + t, null).message());
        assertEquals("not a JWT", api.call("GET", "/balance", "DPoP " + t, null).message());
        assertEquals("not a JWT", api.call("GET", "/balance", "DPoP " + t, "a.b").message());
    }

    @Test
    void proofsFromAnotherKeyMethodAddressTokenOrTimeAreRefused() throws Exception {
        KeyPair key = TokensDemo.newKey();
        KeyPair other = TokensDemo.newKey();
        String t = bound(key);
        String t2 = bound(key);
        assertEquals("proof was signed with another key", api.call("GET", "/balance", "DPoP " + t, DpopProof.create(other, "GET", URL, t, clock.now(), "1")).message());
        assertEquals("proof is for another method", api.call("GET", "/balance", "DPoP " + t, DpopProof.create(key, "POST", URL, t, clock.now(), "2")).message());
        assertEquals("proof is for another address", api.call("GET", "/balance", "DPoP " + t, DpopProof.create(key, "GET", ResourceApi.BASE + "/x", t, clock.now(), "3")).message());
        assertEquals("proof is for another token", api.call("GET", "/balance", "DPoP " + t, DpopProof.create(key, "GET", URL, t2, clock.now(), "4")).message());
        assertEquals("proof is too old or from the future", api.call("GET", "/balance", "DPoP " + t, DpopProof.create(key, "GET", URL, t, clock.now() - 61, "5")).message());
        assertEquals("proof is too old or from the future", api.call("GET", "/balance", "DPoP " + t, DpopProof.create(key, "GET", URL, t, clock.now() + 61, "6")).message());
        assertEquals(200, api.call("GET", "/balance", "DPoP " + t, DpopProof.create(key, "GET", URL, t, clock.now() - 59, "7")).status());
    }

    @Test
    void aTamperedProofFailsTheSignature() throws Exception {
        KeyPair key = TokensDemo.newKey();
        String t = bound(key);
        String p = DpopProof.create(key, "GET", URL, t, clock.now(), "x");
        String[] parts = p.split("\\.");
        String forged = parts[0] + "." + B64.encode(B64.decodeText(parts[1]).replace("GET", "PUT")) + "." + parts[2];
        assertEquals("bad signature", api.call("GET", "/balance", "DPoP " + t, forged).message());
    }

    @Test
    void replayCacheForgetsOldProofsButTheWindowStillRefusesThem() throws Exception {
        KeyPair key = TokensDemo.newKey();
        String t = bound(key);
        String p = DpopProof.create(key, "GET", URL, t, clock.now(), "same");
        assertEquals(200, api.call("GET", "/balance", "DPoP " + t, p).status());
        clock.advance(200);
        assertEquals("proof is too old or from the future", api.call("GET", "/balance", "DPoP " + t, p).message());
    }

    @Test
    void jwkWithAPrivatePartOrTheWrongCurveIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Jwk.publicKey(MiniJson.parseObject("{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"AA\",\"y\":\"AA\",\"d\":\"AA\"}")));
        assertThrows(IllegalArgumentException.class, () -> Jwk.publicKey(MiniJson.parseObject("{\"kty\":\"RSA\"}")));
    }

    @Test
    void miniJsonReadsObjectsAndRejectsDuplicatesAndGarbage() {
        assertEquals("v", MiniJson.parseObject("{\"a\": \"v\", \"n\": 5, \"o\": {\"b\": \"c\"}}").get("a"));
        assertEquals(5L, MiniJson.parseObject("{\"n\":5}").get("n"));
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parseObject("{\"a\":\"1\",\"a\":\"2\"}"));
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parseObject("{\"a\":true}"));
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parseObject("{} x"));
    }
}
