package com.jk.explore.security.sessions;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SessionIdsTest {

    @Test
    void anIdIsOneHundredTwentyEightRandomBitsInBase64url() {
        String id = SessionIds.newId();
        assertEquals(22, id.length());
        assertTrue(id.matches("[A-Za-z0-9_-]+"));
        assertEquals(16, Base64.getUrlDecoder().decode(id).length);
    }

    @Test
    void manyIdsAreAllDifferent() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(SessionIds.newId());
        }
        assertEquals(10_000, ids.size());
    }

    @Test
    void theStoredKeyIsTheSha256OfTheId() {
        // SHA-256("abc") from FIPS 180-2
        assertArrayEquals(HexFormat.of().parseHex("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"), SessionIds.hash("abc"));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", SessionIds.key(SessionIds.hash("abc")));
    }

    @Test
    void theStoreHoldsHashesNotIds() {
        SessionManager bank = new SessionManager(new SimClock(), Settings.bank(), new SessionStore(), SessionDemo.users());
        String id = bank.start();
        assertFalse(bank.store().keySet().contains(id));
        for (String key : bank.store().keys()) {
            assertEquals(Answer.UNKNOWN, bank.account(key));
        }
    }
}
