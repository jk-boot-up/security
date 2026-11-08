package com.jk.explore.security.attacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The tick-by-tick view of an attack: runs of the same answer, and refused attempts that use up no guess. */
class TimelineTest {

    private static List<Step> dictionarySteps(Defences d) {
        Ticker t = new Ticker();
        Attacks attacks = new Attacks(AttacksDemo.bank(t, d, Set.of()), t);
        attacks.dictionary("attacker", "dana", AttacksDemo.WORDLIST);
        return attacks.steps();
    }

    @Test
    void aLockoutShowsTwoLocksOfTenTicksBeforeTheFourteenthGuessAtTickThirtyThree() {
        List<String> runs = Timeline.runs(dictionarySteps(Defences.none().withLockout(5, 10, LockScope.ACCOUNT)));
        assertEquals(List.of(
                "ticks 0-4     guesses 1-5     WRONG_PASSWORD",
                "ticks 5-14    no guess        LOCKED",
                "ticks 15-19   guesses 6-10    WRONG_PASSWORD",
                "ticks 20-29   no guess        LOCKED",
                "ticks 30-32   guesses 11-13   WRONG_PASSWORD",
                "tick 33       guess 14        OK"), runs);
    }

    @Test
    void aRateLimitRefusesTheRestOfEachTenTickWindow() {
        List<String> runs = Timeline.runs(dictionarySteps(Defences.none().withRateLimit(5, 10)));
        assertEquals("ticks 5-9     no guess        RATE_LIMITED", runs.get(1));
        assertEquals("tick 23       guess 14        OK", runs.get(runs.size() - 1));
    }

    @Test
    void withNoDefenceThereIsOneRunOfFailuresThenTheHit() {
        assertEquals(List.of("ticks 0-12    guesses 1-13    WRONG_PASSWORD", "tick 13       guess 14        OK"),
                Timeline.runs(dictionarySteps(Defences.none())));
    }

    @Test
    void onlyLocksAndLimitsAreRefusals() {
        assertTrue(new Step(0, "dana", "x", Outcome.LOCKED).refused());
        assertTrue(new Step(0, "dana", "x", Outcome.RATE_LIMITED).refused());
        assertFalse(new Step(0, "dana", "x", Outcome.WRONG_PASSWORD).refused());
    }

    @Test
    void theStoredHashIsSha256OfTheSaltNameAndPassword() {
        // SHA-256 of "demo-salt:dana:midnight7", as the demo prints it in act one
        assertTrue(Bank.hashOf("dana", "midnight7").startsWith("61340acccb770ca46d581666"));
        assertEquals(64, Bank.hashOf("dana", "123456").length());
        assertFalse(Bank.hashOf("dana", "123456").equals(Bank.hashOf("dana", "midnight7")));
    }
}
