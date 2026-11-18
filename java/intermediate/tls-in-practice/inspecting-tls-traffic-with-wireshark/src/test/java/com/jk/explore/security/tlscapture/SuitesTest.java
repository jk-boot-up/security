package com.jk.explore.security.tlscapture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** What a suite name says, with no program. */
class SuitesTest {

    @Test
    void numbersBecomeNames() {
        assertEquals("TLS_AES_256_GCM_SHA384", Suites.name("0x1302"));
        assertEquals("TLS_RSA_WITH_AES_128_CBC_SHA", Suites.name("0x002f"));
        assertEquals("0x9999", Suites.name("0x9999"));
    }

    @Test
    void aSecretIsAsLongAsTheHash() {
        assertEquals(48, Suites.secretBytes("TLS_AES_256_GCM_SHA384"));
        assertEquals(32, Suites.secretBytes("TLS_AES_128_GCM_SHA256"));
    }

    @Test
    void onlyRsaKeyExchangeIsOpenedByTheServerKey() {
        assertTrue(Suites.keyExchange("TLS_AES_256_GCM_SHA384").startsWith("ephemeral"));
        assertTrue(Suites.keyExchange("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384").startsWith("ephemeral (ECDHE)"));
        assertTrue(Suites.keyExchange("TLS_RSA_WITH_AES_128_CBC_SHA").startsWith("RSA"));
        assertFalse(Suites.serverKeyOpens("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"));
        assertTrue(Suites.serverKeyOpens("TLS_RSA_WITH_AES_128_CBC_SHA"));
    }
}
