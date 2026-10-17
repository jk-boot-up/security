package com.jk.explore.security.hashing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class AccountStoreTest {

    private final WorkMeter meter = new WorkMeter();
    private final Pbkdf2Hasher hasher = new Pbkdf2Hasher(meter, new CountingSalts(), 2000);

    @Test
    void aRightPasswordLogsInAndAWrongOneDoesNot() {
        AccountStore store = new AccountStore(hasher).register("ana", "welcome1");
        assertEquals(Login.OK, store.login("ana", "welcome1"));
        assertEquals(Login.WRONG, store.login("ana", "guess"));
        assertEquals(Login.WRONG, store.login("nobody", "welcome1"));
    }

    @Test
    void anOldHashIsUpgradedAfterACorrectLoginOnly() {
        AccountStore store = new AccountStore(hasher).restore("erin", hasher.hashWithRounds("letmein", 500));
        String before = store.storedFor("erin");
        assertEquals(Login.WRONG, store.login("erin", "guess"));
        assertEquals(before, store.storedFor("erin"));
        assertEquals(Login.UPGRADED, store.login("erin", "letmein"));
        assertEquals(2000, Pbkdf2Hasher.iterationsOf(store.storedFor("erin")));
        assertNotEquals(before, store.storedFor("erin"));
        assertEquals(Login.OK, store.login("erin", "letmein"));
    }

    @Test
    void theStolenTableNeverContainsAPasswordWhenAHashIsUsed() {
        AccountStore store = new AccountStore(hasher).register("ana", "welcome1");
        assertEquals(false, store.stolen().get("ana").contains("welcome1"));
    }

    @Test
    void oneLoginCostsAsManyStepsAsTheRounds() {
        AccountStore store = new AccountStore(hasher).register("ana", "welcome1");
        meter.reset();
        store.login("ana", "welcome1");
        assertEquals(2000, meter.steps());
    }
}
