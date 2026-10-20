package com.jk.explore.security.pinning;

/** The installed apps of a bank, and how many of them still carry an old pin when the server changes its key. */
public record Fleet(int installs, int percentUpdatedWithinAWeek) {

    /** Apps whose pin no longer matches when the server changes key on day 0, counted for the first week. */
    public int lockedOutAtDay0() {
        return installs;
    }

    public int lockedOutAfterAWeek() {
        return installs - installs * percentUpdatedWithinAWeek / 100;
    }
}
