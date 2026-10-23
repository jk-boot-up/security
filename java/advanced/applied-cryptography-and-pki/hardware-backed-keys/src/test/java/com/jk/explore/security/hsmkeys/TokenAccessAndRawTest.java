package com.jk.explore.security.hsmkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import javax.security.auth.login.FailedLoginException;
import org.junit.jupiter.api.Test;

class TokenAccessAndRawTest {

    @Test
    void loginFailuresAreClassifiedByTheirCause() {
        assertEquals(TokenAccess.LoginResult.PIN_INCORRECT, TokenAccess.classify(new IOException("load failed", new RuntimeException("CKR_PIN_INCORRECT"))));
        assertEquals(TokenAccess.LoginResult.PIN_LOCKED, TokenAccess.classify(new IOException("x", new IOException("y", new RuntimeException("CKR_PIN_LOCKED")))));
        assertEquals(TokenAccess.LoginResult.NO_PIN_GIVEN, TokenAccess.classify(new IOException("x", new FailedLoginException("no password provided, and no callback"))));
        assertEquals(TokenAccess.LoginResult.OTHER, TokenAccess.classify(new CertificateException("other")));
    }

    @Test
    void returnValuesHaveNames() {
        assertEquals("CKR_KEY_UNEXTRACTABLE", new Raw.Answer(0x6A, new byte[0]).name());
        assertEquals("CKR_ATTRIBUTE_SENSITIVE", new Raw.Answer(0x11, new byte[0]).name());
        assertEquals("CKR_0x1234", new Raw.Answer(0x1234, new byte[0]).name());
        assertTrue(new Raw.Answer(0, new byte[0]).ok());
        assertFalse(new Raw.Answer(1, new byte[0]).ok());
    }

    @Test
    void attributesAreEncodedTheWayPkcs11WantsThem() {
        assertEquals(1, Raw.Attr.bool(Raw.CKA_SENSITIVE, true).value().length);
        assertEquals(8, Raw.Attr.ulong(Raw.CKA_KEY_TYPE, Raw.CKK_RSA).value().length);
        assertEquals(12, Raw.Attr.text(Raw.CKA_LABEL, "payments-key").value().length);
    }

    @Test
    void theProviderConfigurationAsksForSensitiveNonExtractableKeysAndHexLabels() {
        String cfg = Lab.providerConfigText(Path.of("/lib/x.so"), "hsm", "ab");
        assertTrue(cfg.contains("library=/lib/x.so"));
        assertTrue(cfg.contains("CKA_SENSITIVE = true"));
        assertTrue(cfg.contains("CKA_EXTRACTABLE = false"));
        assertTrue(cfg.contains("CKA_LABEL = 0h6162"));
    }

    @Test
    void theDemoPinsAreNotWrittenInTheSource() {
        assertEquals(6, Lab.USER_PIN.length());
        assertEquals(8, Lab.SO_PIN.length());
    }
}
