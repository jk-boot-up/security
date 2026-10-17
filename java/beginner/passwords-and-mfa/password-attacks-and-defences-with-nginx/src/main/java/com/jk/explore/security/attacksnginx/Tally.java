package com.jk.explore.security.attacksnginx;

/** How many attempts were answered by the login (401) and how many were turned away by the limit (429). */
public record Tally(int answered, int refused) {

    public int total() {
        return answered + refused;
    }
}
