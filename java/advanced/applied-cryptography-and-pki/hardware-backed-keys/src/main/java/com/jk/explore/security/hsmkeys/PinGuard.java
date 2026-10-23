package com.jk.explore.security.hsmkeys;

import java.util.function.Predicate;

/** A lockout kept by the application: after N wrong PINs it stops asking the token. A token that locks itself does this in hardware. */
final class PinGuard {

    enum Outcome { ACCEPTED, REFUSED, LOCKED }

    private final int max;
    private final Predicate<String> tokenLogin;
    private int failures;

    PinGuard(int max, Predicate<String> tokenLogin) {
        this.max = max;
        this.tokenLogin = tokenLogin;
    }

    Outcome attempt(String pin) {
        if (failures >= max) {
            return Outcome.LOCKED;
        }
        if (tokenLogin.test(pin)) {
            failures = 0;
            return Outcome.ACCEPTED;
        }
        failures++;
        return Outcome.REFUSED;
    }

    boolean locked() {
        return failures >= max;
    }

    /** What a security officer does after checking who is at the keyboard. */
    void reset() {
        failures = 0;
    }
}
