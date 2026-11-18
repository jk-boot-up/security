package com.jk.explore.security.stepca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Opening a one-time token and mapping the CA's errors to reasons need no tool, so they are tested here without step-ca. */
class TokenTest {

    private static String part(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static X509Certificate root() throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(PkiTest.ROOT.stripIndent().getBytes(StandardCharsets.US_ASCII)));
    }

    private static String token(String sha) {
        return part("{\"alg\":\"ES256\",\"typ\":\"JWT\",\"kid\":\"k1\"}") + "."
                + part("{\"sub\":\"www.shop.localhost\",\"sans\":[\"www.shop.localhost\"],\"iss\":\"tokens\",\"aud\":\"https://localhost:9000/1.0/sign\","
                        + "\"nbf\":1767600000,\"iat\":1767600000,\"exp\":1767600300,\"sha\":\"" + sha + "\",\"jti\":\"abc\"}")
                + ".c2lnbmF0dXJl";
    }

    @Test
    void readsTheThreePartsAndTheClaims() throws Exception {
        Token t = Token.parse(token(Token.sha256(root())));
        assertEquals(3, t.parts());
        assertEquals("ES256", t.header().path("alg").asString());
        assertEquals("www.shop.localhost", t.text("sub"));
        assertEquals(List.of("www.shop.localhost"), t.sans());
        assertEquals("tokens", t.text("iss"));
        assertEquals(300, t.lifetimeSeconds());
    }

    @Test
    void theShaClaimPinsExactlyOneRoot() throws Exception {
        assertTrue(Token.parse(token(Token.sha256(root()))).pinsRoot(root()));
        assertFalse(Token.parse(token("00".repeat(32))).pinsRoot(root()));
        assertEquals(64, Token.sha256(root()).length());
    }

    @Test
    void theCaErrorsBecomeShortReasons() {
        assertEquals("not authorized", new StepCa.Outcome(false, "The request lacked necessary authorization to be completed.").reason());
        assertEquals("that name is outside what the CA's policy allows", new StepCa.Outcome(false, "dns name \"pay.example\" not allowed").reason());
        assertEquals("the key does not match: the TLS call fails", new StepCa.Outcome(false, "remote error: tls: bad certificate").reason());
        assertEquals("", new StepCa.Outcome(true, "").reason());
    }
}
