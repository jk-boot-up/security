package com.jk.explore.security.oauthkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Base64;
import org.junit.jupiter.api.Test;

/** The hand-made steps must give exactly what the library gives, so the demo's worked example is the real thing. No Docker needed. */
class ChallengeStepsTest {

    @Test
    void theStepsGiveTheRfc7636Challenge() {
        assertEquals(OAuthKeycloakDemo.RFC_CHALLENGE, ChallengeSteps.challenge(OAuthKeycloakDemo.RFC_VERIFIER));
    }

    @Test
    void theStepsAgreeWithTheClientForFreshVerifiers() {
        for (int i = 0; i < 50; i++) {
            String verifier = OAuthClient.newVerifier();
            assertEquals(OAuthClient.challenge(verifier), ChallengeSteps.challenge(verifier));
        }
    }

    @Test
    void handMadeBase64UrlMatchesJavaForEveryLength() {
        for (int n = 0; n <= 40; n++) {
            byte[] bytes = new byte[n];
            for (int i = 0; i < n; i++) {
                bytes[i] = (byte) (i * 37 + 250);
            }
            assertEquals(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes), ChallengeSteps.base64Url(bytes));
        }
    }

    @Test
    void theFirstGroupIsWorkedOutBitByBit() {
        byte[] hash = ChallengeSteps.sha256(ChallengeSteps.ascii(OAuthKeycloakDemo.RFC_VERIFIER));
        assertEquals("13 d3 1e = 000100 111101 001100 011110 = 4 61 12 30 = E9Me", ChallengeSteps.firstGroup(hash));
    }
}
