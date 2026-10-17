package com.jk.explore.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KeysTest {

    private static final long START = JwtDemo.START;

    private static String reason(Validator validator, String token) {
        return assertThrows(InvalidTokenException.class, () -> validator.validate(token)).getMessage();
    }

    @Test
    void bothKeysWorkDuringTheChangeoverAndOnlyTheNewOneAfter() throws Exception {
        KeySet keys = JwtDemo.bankKeys().add(JwtDemo.NEW_KID, JwtDemo.NEW_KEY.getPublic());
        Validator validator = new Validator(Policy.bank(), keys, new SimClock(START));
        String old = Issuer.issue(JwtDemo.OLD_KID, JwtDemo.OLD_KEY.getPrivate(), "ana", "accounts:read", START, 300);
        String fresh = Issuer.issue(JwtDemo.NEW_KID, JwtDemo.NEW_KEY.getPrivate(), "ana", "accounts:read", START, 300);
        assertEquals("ana", validator.validate(old).subject());
        assertEquals("ana", validator.validate(fresh).subject());
        keys.remove(JwtDemo.OLD_KID);
        assertEquals("unknown key id bank-2026-01", reason(validator, old));
        assertEquals("ana", validator.validate(fresh).subject());
        assertEquals(List.of("bank-2026-07"), keys.ids());
    }

    @Test
    void aTokenCannotPickAnotherKeyFromTheSet() {
        KeySet keys = JwtDemo.bankKeys().add(JwtDemo.NEW_KID, JwtDemo.NEW_KEY.getPublic());
        Validator validator = new Validator(Policy.bank(), keys, new SimClock(START));
        String signedOldNamedNew = Issuer.issue(JwtDemo.NEW_KID, JwtDemo.OLD_KEY.getPrivate(), "ana", "accounts:read", START, 300);
        assertEquals("signature does not match", reason(validator, signedOldNamedNew));
    }

    @Test
    void anUnknownOrMissingKeyIdIsRefused() {
        Validator validator = new Validator(Policy.bank(), JwtDemo.bankKeys(), new SimClock(START));
        assertEquals("unknown key id attacker-key",
                reason(validator, Issuer.issue("attacker-key", JwtDemo.INTRUDER_KEY.getPrivate(), "ana", "accounts:read", START, 300)));
        Map<String, Object> header = new LinkedHashMap<>(Issuer.header(JwtDemo.OLD_KID));
        header.remove("kid");
        assertEquals("no key id", reason(validator, Issuer.issue(JwtDemo.OLD_KEY.getPrivate(), header, Issuer.claims("ana", "accounts:read", START, 300))));
        header.put("kid", 7L);
        assertEquals("no key id", reason(validator, Issuer.issue(JwtDemo.OLD_KEY.getPrivate(), header, Issuer.claims("ana", "accounts:read", START, 300))));
    }

    @Test
    void theDemoKeysAreDifferentAndStable() {
        assertNotEquals(Base64Url.encode(Ed25519.encode(JwtDemo.OLD_KEY.getPublic())), Base64Url.encode(Ed25519.encode(JwtDemo.NEW_KEY.getPublic())));
        assertEquals(Base64Url.encode(Ed25519.encode(JwtDemo.NEW_KEY.getPublic())),
                Base64Url.encode(Ed25519.encode(Ed25519.fromSeed(JwtDemo.seed("bank-2026-07 demo seed")).getPublic())));
    }

    @Test
    void publicKeysRoundTripThroughTheirEncoding() {
        for (var pair : List.of(JwtDemo.OLD_KEY, JwtDemo.NEW_KEY, JwtDemo.INTRUDER_KEY)) {
            byte[] raw = Ed25519.encode(pair.getPublic());
            assertEquals(Base64Url.encode(raw), Base64Url.encode(Ed25519.encode(Ed25519.publicKey(raw))));
        }
    }
}
