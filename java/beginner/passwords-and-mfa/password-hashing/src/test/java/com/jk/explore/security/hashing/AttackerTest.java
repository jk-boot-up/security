package com.jk.explore.security.hashing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AttackerTest {

    private final Attacker attacker = new Attacker(HashingDemo.WORDLIST);

    @Test
    void aTableCracksEveryUnsaltedAccountWhoseWordIsOnTheList() {
        UnsaltedSha256 hasher = new UnsaltedSha256(new WorkMeter());
        AccountStore store = new AccountStore(hasher);
        HashingDemo.PASSWORDS.forEach(store::register);
        Cracked cracked = attacker.crackWithTable(store.stolen(), hasher);
        assertEquals(List.of("ana", "ben", "carol", "erin"), List.copyOf(cracked.found().keySet()));
        assertFalse(cracked.found().containsKey("dave"));
    }

    @Test
    void aTableDoesNotWorkAgainstSaltedHashes() {
        WorkMeter meter = new WorkMeter();
        SaltedSha256 hasher = new SaltedSha256(meter, new CountingSalts());
        AccountStore store = new AccountStore(hasher);
        HashingDemo.PASSWORDS.forEach(store::register);
        // the table is built with a salt of its own, so none of its values match a stored one
        Cracked cracked = attacker.crackWithTable(store.stolen(), hasher);
        assertTrue(cracked.found().isEmpty());
    }

    @Test
    void saltedAccountsAreStillCrackedOneByOne() {
        WorkMeter meter = new WorkMeter();
        SaltedSha256 hasher = new SaltedSha256(meter, new CountingSalts());
        AccountStore store = new AccountStore(hasher);
        HashingDemo.PASSWORDS.forEach(store::register);
        meter.reset();
        Cracked cracked = attacker.crackEachAccount(store.stolen(), hasher);
        assertEquals(4, cracked.found().size());
        assertEquals(24, cracked.guesses());
        assertEquals(24, meter.steps());
    }

    @Test
    void aSlowHashMultipliesTheWorkOfTheSameAttack() {
        WorkMeter meter = new WorkMeter();
        Pbkdf2Hasher hasher = new Pbkdf2Hasher(meter, new CountingSalts(), 1000);
        AccountStore store = new AccountStore(hasher);
        HashingDemo.PASSWORDS.forEach(store::register);
        meter.reset();
        Cracked cracked = attacker.crackEachAccount(store.stolen(), hasher);
        assertEquals(24, cracked.guesses());
        assertEquals(24 * 1000, meter.steps());
        assertEquals(4, cracked.found().size());
    }

    @Test
    void aStrongPasswordIsNotOnTheListAndSurvives() {
        WorkMeter meter = new WorkMeter();
        Pbkdf2Hasher hasher = new Pbkdf2Hasher(meter, new CountingSalts(), 1000);
        AccountStore store = new AccountStore(hasher).register("dave", HashingDemo.PASSWORDS.get("dave"));
        assertTrue(attacker.crackEachAccount(store.stolen(), hasher).found().isEmpty());
    }
}
