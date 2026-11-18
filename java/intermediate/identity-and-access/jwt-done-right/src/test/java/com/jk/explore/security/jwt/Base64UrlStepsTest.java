package com.jk.explore.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The hand-made base64url gives the library's exact result, so the steps the demo prints are the real ones. */
class Base64UrlStepsTest {

    @Test
    void handMadeEncodingMatchesTheLibraryForTheHeaderAndTheClaims() {
        for (String json : new String[] {Json.write(Issuer.header(JwtDemo.OLD_KID)),
                Json.write(Issuer.claims("ana", "accounts:read", JwtDemo.START, 300)), "a", "ab", "abc"}) {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            assertEquals(Base64Url.encode(bytes), Base64UrlSteps.encode(bytes), json);
        }
    }

    @Test
    void everyTokenStartsWithEyJBecauseEveryHeaderStartsWithBraceQuote() {
        byte[] first = "{\"a".getBytes(StandardCharsets.US_ASCII);
        assertEquals("011110 110010 001001 100001", Base64UrlSteps.groups(first));
        assertEquals("eyJh", Base64UrlSteps.encode(first));
    }
}
