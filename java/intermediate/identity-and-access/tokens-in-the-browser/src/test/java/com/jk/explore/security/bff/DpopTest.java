package com.jk.explore.security.bff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DpopTest {

    private static final long NOW = 1_790_000_000L;
    private static final String URL = "https://api.bank.example/accounts";

    @Test
    void thumbprintMatchesRfc8037AppendixA3() throws Exception {
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "OKP");
        jwk.put("crv", "Ed25519");
        jwk.put("x", "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo");
        assertEquals("kPrK_qmxVWaYVA9wwBF6Iuo3vVzz7TxHCTwXBygrS4k", DpopKey.thumbprint(jwk));
        DpopKey fromSeed = new DpopKey(Ed25519.fromSeed(Base64Url.decode("nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A")));
        assertEquals("kPrK_qmxVWaYVA9wwBF6Iuo3vVzz7TxHCTwXBygrS4k", fromSeed.thumbprint());
    }

    @Test
    void aGoodProofReturnsTheKeyThumbprint() throws Exception {
        DpopKey key = new DpopKey(BffDemo.seeded("k"));
        DpopVerifier verifier = new DpopVerifier(new SimClock(NOW));
        assertEquals(key.thumbprint(), verifier.verify(key.proof("GET", URL, NOW, "token"), "GET", URL, "token"));
    }

    @Test
    void theTargetIgnoresQueryAndFragment() throws Exception {
        DpopKey key = new DpopKey(BffDemo.seeded("k"));
        DpopVerifier verifier = new DpopVerifier(new SimClock(NOW));
        assertEquals(key.thumbprint(), verifier.verify(key.proof("GET", URL + "?page=2", NOW, null), "GET", URL + "#top", null));
    }

    @Test
    void eachBadProofIsRefusedWithItsReason() {
        DpopKey key = new DpopKey(BffDemo.seeded("k"));
        DpopVerifier verifier = new DpopVerifier(new SimClock(NOW));
        assertEquals("a DPoP proof is required", refusal(() -> verifier.verify(null, "GET", URL, null)));
        assertEquals("the proof was made for another request", refusal(() -> verifier.verify(key.proof("POST", URL, NOW, null), "GET", URL, null)));
        assertEquals("the proof was made for another request", refusal(() -> verifier.verify(key.proof("GET", URL + "x", NOW, null), "GET", URL, null)));
        assertEquals("the proof is too old", refusal(() -> verifier.verify(key.proof("GET", URL, NOW - 61, null), "GET", URL, null)));
        assertEquals("the proof is too old", refusal(() -> verifier.verify(key.proof("GET", URL, NOW + 61, null), "GET", URL, null)));
        assertEquals("the proof was made for another access token", refusal(() -> verifier.verify(key.proof("GET", URL, NOW, "a"), "GET", URL, "b")));
        assertEquals("the proof was made for another access token", refusal(() -> verifier.verify(key.proof("GET", URL, NOW, null), "GET", URL, "b")));
    }

    @Test
    void aProofIsAcceptedOnlyOnce() throws Exception {
        DpopKey key = new DpopKey(BffDemo.seeded("k"));
        DpopVerifier verifier = new DpopVerifier(new SimClock(NOW));
        String proof = key.proof("GET", URL, NOW, null);
        verifier.verify(proof, "GET", URL, null);
        assertEquals("the proof was already used", refusal(() -> verifier.verify(proof, "GET", URL, null)));
    }

    @Test
    void aRefusedProofDoesNotUseUpItsId() throws Exception {
        DpopKey key = new DpopKey(BffDemo.seeded("k"));
        DpopVerifier verifier = new DpopVerifier(new SimClock(NOW));
        String proof = key.proof("GET", URL, NOW, "a");
        refusal(() -> verifier.verify(proof, "GET", URL, "b"));
        assertEquals(key.thumbprint(), verifier.verify(proof, "GET", URL, "a"));
    }

    @Test
    void anEditedProofOrAWrongHeaderIsRefused() throws Exception {
        DpopKey key = new DpopKey(BffDemo.seeded("k"));
        DpopVerifier verifier = new DpopVerifier(new SimClock(NOW));
        String[] parts = key.proof("GET", URL, NOW, null).split("\\.");
        String edited = parts[0] + "." + Base64Url.encode(("{\"jti\":\"abcdefghijklmnopqr\",\"htm\":\"GET\",\"htu\":\"" + URL + "\",\"iat\":" + NOW + "}")
                .getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
        assertEquals("the proof's signature does not match its key", refusal(() -> verifier.verify(edited, "GET", URL, null)));
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("typ", "JWT");
        header.put("alg", "EdDSA");
        header.put("jwk", key.jwk());
        String wrongType = Jws.sign(header, "{}".getBytes(StandardCharsets.UTF_8), BffDemo.seeded("k").getPrivate());
        assertEquals("the proof's type is not dpop+jwt", refusal(() -> verifier.verify(wrongType, "GET", URL, null)));
        Map<String, Object> privateJwk = new LinkedHashMap<>(key.jwk());
        privateJwk.put("d", "secret");
        header.put("typ", "dpop+jwt");
        header.put("jwk", privateJwk);
        String withPrivate = Jws.sign(header, "{}".getBytes(StandardCharsets.UTF_8), BffDemo.seeded("k").getPrivate());
        assertEquals("the proof's key is not a public Ed25519 key", refusal(() -> verifier.verify(withPrivate, "GET", URL, null)));
    }

    private interface Check {
        Object run() throws Exception;
    }

    private static String refusal(Check check) {
        return assertThrows(InvalidTokenException.class, check::run).getMessage();
    }
}
