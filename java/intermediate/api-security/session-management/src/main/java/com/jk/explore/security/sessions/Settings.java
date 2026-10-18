package com.jk.explore.security.sessions;

/** How long sessions last and how the manager treats them. */
record Settings(long idleSeconds, long absoluteSeconds, long reauthSeconds, int maxSessions, boolean rotate) {

    /** A bank's settings: 15 idle minutes, 8 hours at most, the password again for a payment after 5 minutes, 3 sessions, a new id at every sign-in. */
    static Settings bank() {
        return new Settings(SimClock.minutes(15), SimClock.minutes(8 * 60), SimClock.minutes(5), 3, true);
    }

    /** The same, except that the id is kept at sign-in and at step-up: the mistake behind session fixation. */
    static Settings withoutRotation() {
        Settings bank = bank();
        return new Settings(bank.idleSeconds, bank.absoluteSeconds, bank.reauthSeconds, bank.maxSessions, false);
    }
}
