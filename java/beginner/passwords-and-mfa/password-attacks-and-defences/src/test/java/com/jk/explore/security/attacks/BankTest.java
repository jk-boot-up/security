package com.jk.explore.security.attacks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class BankTest {

    private Bank bank(Ticker t, Defences d) {
        return AttacksDemo.bank(t, d, Set.of());
    }

    @Test
    void aRightPasswordWithNoDefencesIsOk() {
        assertEquals(Outcome.OK, bank(new Ticker(), Defences.none()).login("a", "emil", "welcome1").outcome());
    }

    @Test
    void aWrongPasswordAndAnUnknownUserAreDifferentOutcomes() {
        Bank b = bank(new Ticker(), Defences.none());
        assertEquals(Outcome.WRONG_PASSWORD, b.login("a", "emil", "x").outcome());
        assertEquals(Outcome.UNKNOWN_USER, b.login("a", "zed", "x").outcome());
    }

    @Test
    void anAccountLocksAfterTheFifthFailureAndOpensAgainAfterTheLockTicks() {
        Ticker t = new Ticker();
        Bank b = bank(t, Defences.none().withLockout(5, 10, LockScope.ACCOUNT));
        for (int i = 0; i < 5; i++) {
            assertEquals(Outcome.WRONG_PASSWORD, b.login("a", "dana", "x" + i).outcome());
            t.advance(1);
        }
        assertEquals(Outcome.LOCKED, b.login("a", "dana", "midnight7").outcome());
        t.advance(9);
        assertEquals(Outcome.LOCKED, b.login("a", "dana", "midnight7").outcome());
        t.advance(1);
        assertEquals(Outcome.OK, b.login("a", "dana", "midnight7").outcome());
    }

    @Test
    void aSuccessResetsTheCountOfFailures() {
        Ticker t = new Ticker();
        Bank b = bank(t, Defences.none().withLockout(3, 10, LockScope.ACCOUNT));
        b.login("a", "dana", "x");
        b.login("a", "dana", "x");
        assertEquals(Outcome.OK, b.login("a", "dana", "midnight7").outcome());
        b.login("a", "dana", "x");
        b.login("a", "dana", "x");
        assertEquals(Outcome.OK, b.login("a", "dana", "midnight7").outcome());
    }

    @Test
    void lockingByAccountLetsAnAttackerLockTheOwnerOut() {
        Bank b = bank(new Ticker(), Defences.none().withLockout(3, 10, LockScope.ACCOUNT));
        for (int i = 0; i < 3; i++) {
            b.login("attacker", "dana", "x" + i);
        }
        assertEquals(Outcome.LOCKED, b.login("dana-phone", "dana", "midnight7").outcome());
    }

    @Test
    void lockingByAccountAndAddressDoesNot() {
        Bank b = bank(new Ticker(), Defences.none().withLockout(3, 10, LockScope.ACCOUNT_AND_ADDRESS));
        for (int i = 0; i < 3; i++) {
            b.login("attacker", "dana", "x" + i);
        }
        assertEquals(Outcome.LOCKED, b.login("attacker", "dana", "midnight7").outcome());
        assertEquals(Outcome.OK, b.login("dana-phone", "dana", "midnight7").outcome());
    }

    @Test
    void anAddressIsLimitedPerWindowAndOtherAddressesAreNot() {
        Ticker t = new Ticker();
        Bank b = bank(t, Defences.none().withRateLimit(2, 10));
        assertEquals(Outcome.WRONG_PASSWORD, b.login("a", "dana", "1").outcome());
        assertEquals(Outcome.WRONG_PASSWORD, b.login("a", "dana", "2").outcome());
        assertEquals(Outcome.RATE_LIMITED, b.login("a", "dana", "3").outcome());
        assertEquals(Outcome.WRONG_PASSWORD, b.login("b", "dana", "3").outcome());
        t.advance(10);
        assertEquals(Outcome.WRONG_PASSWORD, b.login("a", "dana", "3").outcome());
    }

    @Test
    void aSecondFactorStopsARightPassword() {
        Bank b = AttacksDemo.bank(new Ticker(), Defences.none(), Set.of("dana"));
        assertEquals(Outcome.SECOND_FACTOR_NEEDED, b.login("a", "dana", "midnight7").outcome());
        assertEquals(Outcome.OK, b.login("a", "emil", "welcome1").outcome());
    }

    @Test
    void aBreachedPasswordMustBeChangedEvenWhenRight() {
        Bank b = bank(new Ticker(), Defences.none().withBreachedCheck(Set.of("midnight7")));
        assertEquals(Outcome.RESET_REQUIRED, b.login("a", "dana", "midnight7").outcome());
        assertEquals(Outcome.WRONG_PASSWORD, b.login("a", "dana", "other").outcome());
    }

    @Test
    void genericMessagesAreTheSameForUnknownAndWrong() {
        Bank open = bank(new Ticker(), Defences.none());
        Bank generic = bank(new Ticker(), Defences.none().withGenericMessages());
        assertEquals(false, open.login("a", "zed", "x").message().equals(open.login("a", "emil", "x").message()));
        assertEquals(generic.login("a", "zed", "x").message(), generic.login("a", "emil", "x").message());
    }
}
