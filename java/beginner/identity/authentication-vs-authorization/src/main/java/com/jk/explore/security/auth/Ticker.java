package com.jk.explore.security.auth;

/** A clock made of ticks, so sessions can expire without waiting for real time and every run is identical. */
public class Ticker {

    private int now;

    public int now() {
        return now;
    }

    public void advance(int ticks) {
        now += ticks;
    }
}
