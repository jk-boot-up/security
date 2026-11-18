package com.jk.explore.security.k8ssecrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The steps the demo prints are worked out by hand; these tests check them against the library and the real formats. */
class TeachingStepsTest {

    @Test
    void base64ByHandGivesWhatTheLibraryGives() {
        String encoded = Base64.getEncoder().encodeToString(KubernetesSecretsDemo.CARD_KEY.getBytes(StandardCharsets.UTF_8));
        Base64Steps g = Base64Steps.firstGroup(encoded);
        assertEquals("ZGVt", g.chars());
        assertEquals(List.of(25, 6, 21, 45), g.sixBitValues());
        assertEquals("011001 000110 010101 101101", g.sixBits());
        assertEquals("01100100 01100101 01101101", g.eightBits());
        assertEquals(new String(Base64.getDecoder().decode(encoded.substring(0, 4)), StandardCharsets.US_ASCII), g.text());
        assertEquals("dem", g.text());
    }

    @Test
    void aPlainValueNamesItsFieldAndLengthInFrontOfTheText() {
        // a Secret's data entry in protobuf: field 1 is the name, field 2 the value, each with its length in front
        String raw = "k8s\0\n\f\n\u0002v1\u0012\u0006Secret" + "\n\u0003key\u0012\u0017" + KubernetesSecretsDemo.CARD_KEY;
        Stored s = new Stored(raw);
        assertEquals(List.of(0x0a, 3, 'k' + 0, 'e' + 0, 'y' + 0, 0x12, 23), s.bytesBefore(KubernetesSecretsDemo.CARD_KEY, 7));
        assertEquals("k8s.....v1..Secret", s.start(18));
    }

    @Test
    void anEncryptedValueStartsWithTheProviderAndTheKeyName() {
        Stored s = new Stored(Encryption.MARKER + ":\u0001\u0002\u0003");
        assertEquals(List.of("k8s:enc", "aescbc", "v1", "key1"), s.markerParts());
        assertTrue(new Stored("plain").markerParts().isEmpty());
    }

    @Test
    void theClaimsOfATokenAreReadWithNoKey() {
        Jwt jwt = Jwt.parse(JwtTest.token("{\"aud\":[\"openbao\"],\"exp\":1600,\"iat\":1000,\"sub\":\"s\"}"));
        assertEquals(List.of("aud", "exp", "iat", "sub"), jwt.claimNames());
        assertEquals("openbao", jwt.audiences());
        assertEquals("", Jwt.parse(JwtTest.token("{\"sub\":\"s\"}")).audiences());
    }
}
