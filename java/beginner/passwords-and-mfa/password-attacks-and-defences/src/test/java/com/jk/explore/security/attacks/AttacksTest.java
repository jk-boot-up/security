package com.jk.explore.security.attacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AttacksTest {

    private Found dictionary(Defences d) {
        Ticker t = new Ticker();
        return new Attacks(AttacksDemo.bank(t, d, Set.of()), t).dictionary("attacker", "dana", AttacksDemo.WORDLIST);
    }

    private Found spray(Defences d) {
        Ticker t = new Ticker();
        return new Attacks(AttacksDemo.bank(t, d, Set.of()), t)
                .spray("attacker", AttacksDemo.usernames(), AttacksDemo.SPRAY_PASSWORDS);
    }

    @Test
    void withNoDefenceTheFourteenthGuessWinsAtTickThirteen() {
        Found f = dictionary(Defences.none());
        assertEquals(14, f.guesses());
        assertEquals(13, f.taken().get("dana"));
    }

    @Test
    void aLockoutDelaysTheSameGuessToTickThirtyThree() {
        Found f = dictionary(Defences.none().withLockout(5, 10, LockScope.ACCOUNT));
        assertEquals(14, f.guesses());
        assertEquals(33, f.taken().get("dana"));
    }

    @Test
    void aRateLimitDelaysItToTickTwentyThree() {
        Found f = dictionary(Defences.none().withRateLimit(5, 10));
        assertEquals(23, f.taken().get("dana"));
    }

    @Test
    void aStrongPasswordIsNotOnTheList() {
        Ticker t = new Ticker();
        Found f = new Attacks(AttacksDemo.bank(t, Defences.none(), Set.of()), t)
                .dictionary("attacker", "gus", AttacksDemo.WORDLIST);
        assertTrue(f.taken().isEmpty());
        assertEquals(20, f.guesses());
    }

    @Test
    void sprayingTakesFourAccountsInTwentyThreeGuesses() {
        Found f = spray(Defences.none());
        assertEquals(List.of("emil", "farah", "hana", "ivan"), List.copyOf(f.taken().keySet()));
        assertEquals(23, f.guesses());
        assertEquals(23, f.ticks());
    }

    @Test
    void aLockoutOfFiveDoesNotStopASprayBecauseNoAccountFailsFiveTimes() {
        Found f = spray(Defences.none().withLockout(5, 10, LockScope.ACCOUNT));
        assertEquals(4, f.taken().size());
        assertEquals(23, f.ticks());
    }

    @Test
    void aRateLimitSlowsTheSprayWithoutStoppingIt() {
        Found f = spray(Defences.none().withRateLimit(5, 10));
        assertEquals(4, f.taken().size());
        assertEquals(43, f.ticks());
    }

    @Test
    void stuffingFromManyAddressesNeverTriggersALimitOrALock() {
        Defences both = Defences.none().withLockout(5, 10, LockScope.ACCOUNT).withRateLimit(5, 10);
        Ticker t = new Ticker();
        Map<Outcome, Integer> counts = new Attacks(AttacksDemo.bank(t, both, Set.of()), t).stuff(AttacksDemo.LEAKED);
        assertEquals(4, counts.get(Outcome.OK));
        assertEquals(6, counts.get(Outcome.WRONG_PASSWORD));
        assertEquals(null, counts.get(Outcome.RATE_LIMITED));
        assertEquals(null, counts.get(Outcome.LOCKED));
    }

    @Test
    void aSecondFactorAndABreachCheckEachCutTheDamage() {
        Ticker t = new Ticker();
        Map<Outcome, Integer> mfa = new Attacks(AttacksDemo.bank(t, Defences.none(), Set.of("dana", "joel")), t)
                .stuff(AttacksDemo.LEAKED);
        assertEquals(2, mfa.get(Outcome.OK));
        assertEquals(2, mfa.get(Outcome.SECOND_FACTOR_NEEDED));
        Ticker t2 = new Ticker();
        Defences checked = Defences.none().withBreachedCheck(Set.copyOf(AttacksDemo.LEAKED.values()));
        Map<Outcome, Integer> breached = new Attacks(AttacksDemo.bank(t2, checked, Set.of()), t2).stuff(AttacksDemo.LEAKED);
        assertEquals(null, breached.get(Outcome.OK));
        assertEquals(4, breached.get(Outcome.RESET_REQUIRED));
    }
}
