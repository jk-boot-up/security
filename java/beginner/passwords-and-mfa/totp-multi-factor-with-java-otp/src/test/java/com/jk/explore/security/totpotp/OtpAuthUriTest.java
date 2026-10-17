package com.jk.explore.security.totpotp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class OtpAuthUriTest {

    @Test
    void theUriCarriesTheIssuerAccountSecretAndSettings() {
        String uri = OtpAuthUri.totp("Example Bank", "dana@example.com", Secrets.SHA1, "SHA1", 6, 30);
        assertEquals("otpauth://totp/Example%20Bank:dana%40example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
                + "&issuer=Example%20Bank&algorithm=SHA1&digits=6&period=30", uri);
    }

    @Test
    void theSecretHasNoPaddingCharacters() {
        String uri = OtpAuthUri.totp("B", "a", "foo".getBytes(), "SHA1", 6, 30);
        String secret = uri.substring(uri.indexOf("secret=") + 7, uri.indexOf('&'));
        assertEquals("MZXW6", secret);
        assertFalse(secret.contains("="));
    }
}
