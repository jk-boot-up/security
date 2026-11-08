package com.jk.explore.security.attacks;

/**
 * One login attempt an attack made, as it was answered: the demo prints these to show an attack tick by tick.
 *
 * @param tick when the attempt was made
 * @param username the account it was aimed at
 * @param password the password it tried
 * @param outcome what the login answered
 */
public record Step(int tick, String username, String password, Outcome outcome) {

    /** Refused before the password was checked, so the guess was not used up. */
    public boolean refused() {
        return outcome == Outcome.LOCKED || outcome == Outcome.RATE_LIMITED;
    }
}
