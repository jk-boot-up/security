package com.jk.explore.security.zerotrust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The RS256 steps done by hand give the JDK's answer, for a good token and for each kind of forgery. */
class Rs256StepsTest {

    private static KeyPair key;
    private static RSAPublicKey pub;
    private static String good;

    @BeforeAll
    static void makeToken() throws Exception {
        key = Forge.newKey();
        pub = (RSAPublicKey) key.getPublic();
        good = Forge.token(Map.of("alg", "RS256", "kid", "k1"),
                Forge.claims("https://sso.bank.example/realms/bank", "ledger-api", "alice", List.of("teller"), true, 1_900_000_000L), key);
    }

    private static boolean jdk(String token) throws Exception {
        int dot = token.lastIndexOf('.');
        Signature v = Signature.getInstance("SHA256withRSA");
        v.initVerify(pub);
        v.update(token.substring(0, dot).getBytes(StandardCharsets.US_ASCII));
        try {
            return v.verify(Base64.getUrlDecoder().decode(token.substring(dot + 1)));
        } catch (SignatureException e) {
            return false;
        }
    }

    @Test
    void aGoodTokenHasThePaddingTheLabelAndTheHash() throws Exception {
        Rs256Steps.Steps s = Rs256Steps.check(good, pub);
        assertTrue(s.valid());
        assertTrue(jdk(good));
        assertEquals(256, s.signatureBytes());
        assertEquals(256, s.blockBytes());
        assertEquals(2048, s.keyBits());
        assertEquals(BigInteger.valueOf(65537), s.e());
        // 2 + FF bytes + 1 + 19 + 32 = 256
        assertEquals(256 - 2 - 1 - 19 - 32, s.ffBytes());
    }

    @Test
    void editedClaimsKeepThePaddingButNotTheHash() throws Exception {
        String edited = Forge.withEditedClaims(good, Map.of("realm_access", Map.of("roles", List.of("admin"))));
        Rs256Steps.Steps s = Rs256Steps.check(edited, pub);
        assertTrue(s.padding());
        assertTrue(s.label());
        assertFalse(s.hash());
        assertEquals(jdk(edited), s.valid());
    }

    @Test
    void anotherKeyGivesNoPaddingAtAll() throws Exception {
        String forged = Forge.token(Map.of("alg", "RS256", "kid", "k1"),
                Forge.claims("https://sso.bank.example/realms/bank", "ledger-api", "mallory", List.of("admin"), true, 1_900_000_000L), Forge.newKey());
        Rs256Steps.Steps s = Rs256Steps.check(forged, pub);
        assertFalse(s.padding());
        assertFalse(s.valid());
        assertEquals(jdk(forged), s.valid());
    }

    @Test
    void aFlippedSignatureCharacterIsCaughtLikeTheJdkDoes() throws Exception {
        String flipped = Forge.withFlippedSignature(good);
        assertFalse(Rs256Steps.check(flipped, pub).valid());
        assertFalse(jdk(flipped));
    }
}
