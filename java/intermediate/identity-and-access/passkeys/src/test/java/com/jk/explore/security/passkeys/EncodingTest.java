package com.jk.explore.security.passkeys;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** Base64url against RFC 4648, SHA-256 against FIPS 180-2, and the byte layouts of client data and authenticator data. */
class EncodingTest {

    @ParameterizedTest
    @CsvSource({"f,Zg", "fo,Zm8", "foo,Zm9v", "foob,Zm9vYg", "fooba,Zm9vYmE", "foobar,Zm9vYmFy"})
    void base64urlMatchesRfc4648WithoutPadding(String plain, String encoded) {
        assertEquals(encoded, B64.encode(plain.getBytes(StandardCharsets.US_ASCII)));
        assertArrayEquals(plain.getBytes(StandardCharsets.US_ASCII), B64.decode(encoded));
    }

    @Test
    void base64urlIsStrict() {
        assertThrows(IllegalArgumentException.class, () -> B64.decode("Zg=="));
        assertThrows(IllegalArgumentException.class, () -> B64.decode("a+b/"));
        assertThrows(IllegalArgumentException.class, () -> B64.decode("Zm9vY"));
    }

    @Test
    void sha256MatchesThePublishedAbcVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", HexFormat.of().formatHex(Sha256.of("abc")));
    }

    @Test
    void clientDataRoundTripsAndKeepsTheOrderWebAuthnUses() {
        ClientData data = new ClientData(ClientData.GET, "abc_-", "https://bank.example");
        assertEquals("{\"type\":\"webauthn.get\",\"challenge\":\"abc_-\",\"origin\":\"https://bank.example\",\"crossOrigin\":false}",
                new String(data.json(), StandardCharsets.UTF_8));
        assertEquals(data, ClientData.parse(data.json()));
    }

    @Test
    void clientDataIsReadStrictly() {
        assertThrows(IllegalArgumentException.class, () -> ClientData.parse(
                "{\"type\":\"webauthn.get\",\"challenge\":\"a\",\"origin\":\"https://bank.example\",\"origin\":\"https://x.test\"}"
                        .getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> ClientData.parse(
                "{\"type\":\"webauthn.get\",\"challenge\":\"a\"}".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> ClientData.parse(
                "{\"type\":\"webauthn.get\",\"challenge\":\"a\",\"origin\":\"o\"} x".getBytes(StandardCharsets.UTF_8)));
        ClientData quoted = new ClientData(ClientData.GET, "a", "https://bank.example/\"x\"");
        assertEquals(quoted, ClientData.parse(quoted.json()));
    }

    @Test
    void authenticatorDataHasTheWebAuthnLayout() {
        byte[] bytes = new AuthenticatorData(Sha256.of("bank.example"), AuthenticatorData.USER_PRESENT | AuthenticatorData.USER_VERIFIED,
                258, null).encode();
        assertEquals(37, bytes.length);
        assertArrayEquals(Sha256.of("bank.example"), java.util.Arrays.copyOf(bytes, 32));
        assertEquals(0x05, bytes[32]);
        assertArrayEquals(new byte[] {0, 0, 1, 2}, java.util.Arrays.copyOfRange(bytes, 33, 37));
        AuthenticatorData back = AuthenticatorData.decode(bytes);
        assertEquals(258, back.signCount());
        assertTrue(back.has(AuthenticatorData.USER_VERIFIED));
        assertNull(back.credentialId());
    }

    @Test
    void registrationDataCarriesTheCredentialId() {
        byte[] id = {1, 2, 3, 4};
        byte[] bytes = new AuthenticatorData(Sha256.of("bank.example"), 0x41, 0, id).encode();
        assertEquals(37 + 16 + 2 + 4, bytes.length);
        assertArrayEquals(id, AuthenticatorData.decode(bytes).credentialId());
    }

    @Test
    void malformedAuthenticatorDataIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> AuthenticatorData.decode(new byte[36]));
        assertThrows(IllegalArgumentException.class, () -> AuthenticatorData.decode(new byte[38]));
        byte[] cut = new AuthenticatorData(new byte[32], 0x41, 0, new byte[8]).encode();
        assertThrows(IllegalArgumentException.class, () -> AuthenticatorData.decode(java.util.Arrays.copyOf(cut, cut.length - 1)));
    }
}
