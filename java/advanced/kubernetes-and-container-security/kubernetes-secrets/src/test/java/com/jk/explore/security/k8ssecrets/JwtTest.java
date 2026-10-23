package com.jk.explore.security.k8ssecrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtTest {

    static String token(String claims) {
        Base64.Encoder e = Base64.getUrlEncoder().withoutPadding();
        return e.encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8)) + "." + e.encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + ".signature";
    }

    @Test
    void aBoundTokenHasALifetimeAnAudienceAndAPod() {
        Jwt jwt = Jwt.parse(token("{\"aud\":[\"openbao\"],\"iat\":1000,\"exp\":1600,\"sub\":\"system:serviceaccount:payments:app\",\"kubernetes.io\":{\"pod\":{\"name\":\"job\"}}}"));
        assertTrue(jwt.hasExpiry());
        assertEquals(600, jwt.lifetimeSeconds());
        assertTrue(jwt.hasAudience("openbao"));
        assertFalse(jwt.hasAudience("other"));
        assertEquals("job", jwt.boundPod());
        assertEquals("system:serviceaccount:payments:app", jwt.subject());
    }

    @Test
    void aLegacyTokenNeverExpiresAndIsTiedToNoPod() {
        Jwt jwt = Jwt.parse(token("{\"aud\":[\"https://kubernetes.default.svc\"],\"iat\":1000}"));
        assertFalse(jwt.hasExpiry());
        assertEquals(-1, jwt.lifetimeSeconds());
        assertEquals("", jwt.boundPod());
    }

    @Test
    void somethingThatIsNotAJwtIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Jwt.parse("abc"));
    }
}
