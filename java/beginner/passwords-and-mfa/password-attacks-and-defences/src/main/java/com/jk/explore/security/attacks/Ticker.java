package com.jk.explore.security.attacks;

/** A clock made of ticks (think of each tick as a second), so lockouts and limits need no waiting and every run is identical. */
public class Ticker {

    private int now;

    public int now() {
        return now;
    }

    public void advance(int ticks) {
        now += ticks;
    }
}
