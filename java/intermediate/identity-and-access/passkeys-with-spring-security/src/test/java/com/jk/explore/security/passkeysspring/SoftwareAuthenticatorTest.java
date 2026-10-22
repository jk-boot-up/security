package com.jk.explore.security.passkeysspring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;

/** The authenticator in software builds the bytes that a real one would. */
class SoftwareAuthenticatorTest {

    @Test
    void anAssertionIsThirtySevenBytesOfAuthenticatorData() {
        SoftwareAuthenticator a = new SoftwareAuthenticator();
        SoftwareAuthenticator.Assertion s = a.sign("bank.example", "https://bank.example", "c", true, true);
        assertEquals(37, B64.decode(s.authenticatorData()).length);
    }

    @Test
    void theFirst32BytesAreTheHashOfTheRelyingPartyId() throws Exception {
        byte[] expected = MessageDigest.getInstance("SHA-256").digest("bank.example".getBytes(StandardCharsets.UTF_8));
        byte[] data = B64.decode(new SoftwareAuthenticator().sign("bank.example", "https://bank.example", "c", true, true).authenticatorData());
        assertArrayEquals(expected, java.util.Arrays.copyOf(data, 32));
    }

    @Test
    void theFlagsReportPresenceAndVerification() {
        SoftwareAuthenticator a = new SoftwareAuthenticator();
        byte[] touch = B64.decode(a.sign("bank.example", "https://bank.example", "c", false, true).authenticatorData());
        byte[] finger = B64.decode(a.sign("bank.example", "https://bank.example", "c", true, true).authenticatorData());
        assertEquals(0x01, touch[32] & 0x05);
        assertEquals(0x05, finger[32] & 0x05);
    }

    @Test
    void theCounterGoesUpBySignatureAndSyncedPasskeysStayAtZero() {
        SoftwareAuthenticator a = new SoftwareAuthenticator();
        a.sign("bank.example", "https://bank.example", "c", true, true);
        a.sign("bank.example", "https://bank.example", "c", true, true);
        assertEquals(2, a.counter());
        SoftwareAuthenticator synced = new SoftwareAuthenticator().synced();
        synced.sign("bank.example", "https://bank.example", "c", true, true);
        assertEquals(0, synced.counter());
        assertEquals(0x18, B64.decode(synced.sign("bank.example", "https://bank.example", "c", true, true).authenticatorData())[32] & 0x18);
    }

    @Test
    void aRegistrationCarriesTheCredentialAndAFixedRelyingParty() {
        SoftwareAuthenticator a = new SoftwareAuthenticator();
        SoftwareAuthenticator.Registration r = a.create("bank.example", "https://bank.example", "c", true, "handle");
        assertTrue(a.hasPasskeyFor("bank.example"));
        assertFalse(a.hasPasskeyFor("bank-example.test"));
        assertEquals("handle", a.userHandle());
        assertEquals(a.credentialId(), r.credentialId());
        assertEquals(16, B64.decode(r.credentialId()).length);
        assertTrue(new String(B64.decode(r.clientDataJson()), StandardCharsets.UTF_8).contains("\"type\":\"webauthn.create\""));
    }

    @Test
    void aCloneHasTheSameCredentialAndAnImpostorOnlyTheSameId() {
        SoftwareAuthenticator a = new SoftwareAuthenticator();
        a.create("bank.example", "https://bank.example", "c", true, "h");
        a.sign("bank.example", "https://bank.example", "c", true, true);
        SoftwareAuthenticator clone = a.cloneIt();
        assertEquals(a.credentialId(), clone.credentialId());
        assertEquals(a.counter(), clone.counter());
        SoftwareAuthenticator impostor = a.impostor();
        assertEquals(a.credentialId(), impostor.credentialId());
        assertNotEquals(B64.encode(a.signature(new byte[] {1}, new byte[] {2})), B64.encode(impostor.signature(new byte[] {1}, new byte[] {2})));
    }

    @Test
    void aNewAuthenticatorHasNoPasskey() {
        assertFalse(SoftwareAuthenticator.empty().hasPasskeyFor("bank.example"));
    }
}
