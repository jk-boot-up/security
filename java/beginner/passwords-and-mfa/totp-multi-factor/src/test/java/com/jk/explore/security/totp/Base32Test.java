package com.jk.explore.security.totp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class Base32Test {

    /** The test vectors of RFC 4648, section 10. */
    @ParameterizedTest
    @CsvSource(value = {"'', ''", "f, MY======", "fo, MZXQ====", "foo, MZXW6===", "foob, MZXW6YQ=", "fooba, MZXW6YTB",
        "foobar, MZXW6YTBOI======"})
    void matchesTheRfcVectors(String plain, String encoded) {
        assertEquals(encoded, Base32.encode(plain.getBytes(StandardCharsets.US_ASCII)));
        assertArrayEquals(plain.getBytes(StandardCharsets.US_ASCII), Base32.decode(encoded));
    }

    @Test
    void decodingIgnoresCaseAndSpaces() {
        assertArrayEquals("foobar".getBytes(StandardCharsets.US_ASCII), Base32.decode("mzxw 6ytb oi"));
    }

    @Test
    void aCharacterOutsideTheAlphabetIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Base32.decode("MZXW1"));
    }

    @Test
    void theDemoSecretLooksAsTheAuthenticatorAppWouldShowIt() {
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", Base32.encode(DemoSecrets.RFC_KEY));
    }

    @Test
    void lowerCaseSecretsDecodeTheSameInEveryLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.of("tr"));
            assertArrayEquals("foobar".getBytes(StandardCharsets.US_ASCII), Base32.decode("mzxw6ytboi"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
